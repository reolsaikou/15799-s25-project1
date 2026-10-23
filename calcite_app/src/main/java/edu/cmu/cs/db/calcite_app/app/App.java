package edu.cmu.cs.db.calcite_app.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.enumerable.EnumerableInterpretable;
import org.apache.calcite.adapter.enumerable.EnumerableRel;
import org.apache.calcite.adapter.enumerable.EnumerableRules;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.adapter.jdbc.JdbcTable;
import org.apache.calcite.avatica.Meta.Pat;
import org.apache.calcite.config.NullCollation;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCostImpl;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.metadata.ChainedRelMetadataProvider;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.runtime.ArrayBindable;
import org.apache.calcite.runtime.Bindable;
import org.apache.calcite.runtime.Hook;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.ViewTable;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlDialectFactory;
import org.apache.calcite.sql.SqlExplainFormat;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql2rel.RelDecorrelator;
import org.apache.calcite.sql2rel.RelFieldTrimmer;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.Program;
import org.apache.calcite.tools.Programs;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.RelRunner;
import org.apache.calcite.tools.ValidationException;
import org.apache.calcite.util.trace.CalciteTimingTracer;
import org.apache.calcite.util.trace.CalciteTrace;
import org.checkerframework.dataflow.qual.Pure;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

public class App {
  private static final Logger TIMING_LOG = CalciteTrace.getSqlTimingTracer();
  private static final ObjectMapper PLAN_MAPPER = new ObjectMapper();

  static Program traced(
      String phase,
      Program delegate,
      BiConsumer<String, String> recorder,
      Logger logger) {
    return (planner, input, traits, materializations, lattices) -> {

      long start = System.nanoTime();
      RelNode output;

      try {
        output = delegate.run(planner, input, traits, materializations, lattices);
      } finally {
        double elpased = (System.nanoTime() - start) / 1_000_000.0;

        logger.debug("Programs phase = {}, elpased time = {} ms",
            phase,
            elpased);
      }

      recorder.accept(phase, RelOptUtil.toString(output, SqlExplainLevel.ALL_ATTRIBUTES));

      return output;
    };
  }

  private static void executePhysical(RelNode physicalPlan, CalciteConnection connection, Path outputPath,
      String queryName, CalciteTimingTracer timer)
      throws IOException, SQLException {
    if (!(physicalPlan instanceof EnumerableRel)) {
      throw new IllegalArgumentException(
          "Expected EnumerableRel, god: "
              + physicalPlan.getClass().getName());
    }

    Map<String, Object> parameters = new HashMap<>();

    Bindable<?> executable = EnumerableInterpretable.toBindable(
        parameters,
        null,
        (EnumerableRel) physicalPlan,
        EnumerableRel.Prefer.ARRAY);

    DataContext base = connection.createPrepareContext().getDataContext();

    parameters.put(
        DataContext.Variable.CANCEL_FLAG.camelName, new AtomicBoolean());

    DataContext runtime = new DataContext() {

      @Override
      public SchemaPlus getRootSchema() {
        return base.getRootSchema();
      }

      @Override
      public JavaTypeFactory getTypeFactory() {
        return base.getTypeFactory();
      }

      @Override
      public QueryProvider getQueryProvider() {
        return base.getQueryProvider();
      }

      @Override
      public Object get(String name) {
        return parameters.containsKey(name) ? parameters.get(name) : base.get(name);
      }
    };

    // Bind Context

    timer.traceTime("Bind Context Begin");
    Enumerable<?> bind = executable.bind(runtime);
    timer.traceTime("Bind End");
    Enumerator<?> rows = bind.enumerator();
    timer.traceTime("Enumerator End");

    List<String> lines = new ArrayList<>();
    lines.add(physicalPlan.getRowType().getFieldNames().toString());

    try {

      timer.traceTime("execute begin");

      SerializeResultSet(
          (EnumerableRel) physicalPlan,
          rows,
          outputPath.resolve(queryName.substring(0, queryName.length() - 4) + "_results.csv").toFile());

      timer.traceTime("execute end");
    } finally {
      rows.close();

      Files.write(
          outputPath.resolve(queryName.substring(0, queryName.length() - 4) + "_results.csv"),
          lines);
    }
  }

  private static String toSqlDuckDB(RelNode relNode) {
    SqlDialect.Context context = SqlDialect.EMPTY_CONTEXT.withDatabaseProductName("DuckDB")
        .withIdentifierQuoteString("\"").withNullCollation(NullCollation.LAST);

    SqlDialect dialect = new SqlDialect(context);

    RelToSqlConverter converter = new RelToSqlConverter(dialect);

    return converter.visitRoot(relNode).asStatement().toSqlString(dialect).getSql();
  }

  private static class DuckDBSqlDialect extends SqlDialect {

    public static final SqlDialect.Context DEFAULT_CONTEXT = SqlDialect.EMPTY_CONTEXT
        .withIdentifierQuoteString("\"")
        .withNullCollation(NullCollation.LAST);

    public static final SqlDialect DEFAULT = new DuckDBSqlDialect(DEFAULT_CONTEXT);

    public DuckDBSqlDialect(SqlDialect.Context context) {
      super(context);
    }

    @Override
    @Pure
    public boolean supportsCharSet() {
      return false;
    }

  }

  private static class QueryMeta {
    final String fileName;
    final RelNode relNode;

    QueryMeta(String fileName, RelNode relNode) {
      this.fileName = fileName;
      this.relNode = relNode;
    }
  }

  private static RelNode PrepareStmtAndExecute(RelNode optimizedLogical, SchemaPlus duckSchema, File relNodePath,
      File resultPath)
      throws SQLException, IOException {
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:caseSensitive=false")) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);

      calciteConnection.getRootSchema().add("DuckDB", duckSchema.unwrap(JdbcSchema.class));

      RelRunner runner = connection.unwrap(RelRunner.class);

      final RelNode[] physical = new RelNode[1];

      CalciteTimingTracer timingTracer = new CalciteTimingTracer(CalciteTrace.getSqlTimingTracer(),
          "Sql Prepare & Execution");

      try (Hook.Closeable hook = Hook.PLAN_BEFORE_IMPLEMENTATION.addThread((RelRoot root) -> {
        physical[0] = root.rel;
      });

          PreparedStatement stmt = runner.prepareStatement(optimizedLogical)) {

        timingTracer.traceTime("prepare Stmt completed");
        if (physical[0] == null || physical[0].getConvention() != EnumerableConvention.INSTANCE) {
          throw new IllegalStateException("Expected an Enumerable physical plan");
        }

        SerializePlan(physical[0], relNodePath, "Enumerable physical plan");
        // Files.writeString(relNodePath.toPath(), toSqlDuckDB(physical[0]));

        try (ResultSet resultSet = stmt.executeQuery()) {
          timingTracer.traceTime("executeQuery return");

          SerializeResultSet(resultSet, resultPath);

          timingTracer.traceTime("Execution completed");
        }
      }

      return physical[0];
    }
  }

  private static SchemaPlus LoadData(Path dbPath, String dbJdbc) throws Exception {
    if (!Files.isRegularFile(dbPath) || !dbPath.getFileName().toString().endsWith(".db")) {
      throw new IllegalArgumentException(
          "db path parameter error: " + dbPath);
    }

    SqlDialect dialect = new DuckDBSqlDialect(DuckDBSqlDialect.DEFAULT_CONTEXT);

    SqlDialectFactory dialectFactory = new SqlDialectFactory() {
      @Override
      public SqlDialect create(DatabaseMetaData metaData) {
        return dialect;
      }
    };

    String dbUrl = dbJdbc + dbPath;

    DataSource datasource = JdbcSchema.dataSource(
        dbUrl, "org.duckdb.DuckDBDriver", null, null);

    try (Connection connection = DriverManager.getConnection("jdbc:calcite:caseSensitive=false")) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);

      SchemaPlus root = calciteConnection.getRootSchema();

      return root.add("DuckDB", JdbcSchema.create(root, "DuckDB", datasource, dialectFactory, null, "main"));
    }
  }

  private static List<QueryMeta> ReadSqlFromQuriesDir(Path dir, FrameworkConfig config, Path res_dir)
      throws IOException, SqlParseException, ValidationException, RelConversionException {

    if (!Files.isDirectory(dir)) {
      throw new IllegalArgumentException("not a directory: " + dir);
    }

    List<Path> skipped_paths = List.of(Path.of("capybara1.sql"), Path.of("capybara2.sql"), Path.of("capybara3.sql"),
        Path.of("capybara4.sql"), Path.of("q1.sql"), Path.of("q10.sql"), Path.of("q11.sql"), Path.of("q12.sql"),
        Path.of("q13.sql"));

    List<QueryMeta> metas = new ArrayList<QueryMeta>();
    try (Stream<Path> entreis = Files.list(dir)) {
      Path[] sqlFiles = entreis
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".sql"))
          .sorted()
          .toArray(Path[]::new);

      for (Path path : sqlFiles) {
        System.out.println(path);

        // if (skipped_paths.contains(path.getFileName())) {
        // continue;
        // }

        String sql = Files.readString(path);
        try (Planner planner = Frameworks.getPlanner(config)) {
          SqlNode parsed = planner.parse(sql);

          SqlNode validated = planner.validate(parsed);

          RelNode rel = planner.rel(validated).project();

          // rel = planner.transform(0,
          // rel.getTraitSet().replace(EnumerableConvention.INSTANCE), rel);

          SerializePlan(
              rel,
              res_dir
                  .resolve(
                      path.getFileName().toString().substring(0, path.getFileName().toString().length() - 4) + ".txt")
                  .toFile(),
              "origin");

          // Or Move?
          Files.copy(path, res_dir.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
          metas.add(new QueryMeta(path.getFileName().toString(), rel));
        }
      }

    } catch (IOException e) {
      System.err.println("read dir failed: " + e.getMessage());
    }

    return metas;
  }

  private static RelNode OptimizeRelNodeWithHep(RelNode relNode, HepPlanner hepplanner) {
    hepplanner.setRoot(relNode);
    RelNode optimized = hepplanner.findBestExp();

    return optimized;
  }

  private static void SerializePlan(RelNode relNode, File outputPath, String header) throws IOException {
    Files.writeString(outputPath.toPath(),
        RelOptUtil.dumpPlan(header, relNode, SqlExplainFormat.TEXT, SqlExplainLevel.ALL_ATTRIBUTES));
  }

  private static void SerializeResultSet(ResultSet resultSet, File outputPath) throws SQLException, IOException {
    ResultSetMetaData metaData = resultSet.getMetaData();
    int columnCount = metaData.getColumnCount();
    StringBuilder resultSetString = new StringBuilder();
    for (int i = 1; i <= columnCount; i++) {
      if (i > 1) {
        resultSetString.append(",");
      }
      resultSetString.append(metaData.getColumnName(i));
    }
    resultSetString.append("\n");
    while (resultSet.next()) {
      for (int i = 1; i <= columnCount; i++) {
        if (i > 1) {
          resultSetString.append(",");
        }
        String s = resultSet.getString(i);
        s = s.replace("\n", "\\n");
        s = s.replace("\r", "\\r");
        s = s.replace("\"", "\"\"");
        resultSetString.append("\"");
        resultSetString.append(s);
        resultSetString.append("\"");
      }
      resultSetString.append("\n");
    }
    Files.writeString(outputPath.toPath(), resultSetString.toString());
  }

  private static void SerializeResultSet(EnumerableRel physicalPlan, Enumerator<?> resultRows, File outputPath)
      throws SQLException, IOException {
    int columnCount = physicalPlan.getRowType().getFieldCount();
    StringBuilder resultSetString = new StringBuilder();

    resultSetString.append(String.join(
        ",", physicalPlan.getRowType().getFieldNames()));

    resultSetString.append("\n");
    while (resultRows.moveNext()) {
      Object current = resultRows.current();
      Object[] row = current instanceof Object[]
          ? (Object[]) current
          : new Object[] { current };

      for (int i = 0; i < columnCount; i++) {
        if (i > 0) {
          resultSetString.append(",");
        }
        String s = row[i] == null ? "" : row[i].toString();
        s = s.replace("\n", "\\n");
        s = s.replace("\r", "\\r");
        s = s.replace("\"", "\"\"");
        resultSetString.append("\"");
        resultSetString.append(s);
        resultSetString.append("\"");
      }
      resultSetString.append("\n");
    }
    Files.writeString(outputPath.toPath(), resultSetString.toString());
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("Usage: java -jar App.jar <arg1> <arg2>");
      return;
    }

    // String key = "org.slf4j.simpleLogger.log.org.apache.calcite.sql.timing";
    //
    // System.out.println("日志参数 = " + System.getProperty(key));

    // Feel free to modify this to take as many or as few arguments as you want.
    System.out.println("Running the app!");

    // input dir
    String arg1 = args[0];
    System.out.println("\tArg1: " + arg1);

    // result dir
    String arg2 = args[1];
    System.out.println("\tArg2: " + arg2);

    // database file Path
    String arg3 = args[2];
    System.out.println("\tArg3: " + arg3);

    // Note: in practice, you would probably use
    // org.apache.calcite.tools.Frameworks.
    // That package provides simple defaults that make it easier to configure
    // Calcite.
    // But there's a lot of magic happening there; since this is an educational
    // project,
    // we guide you towards the explicit method in the writeup.

    SqlParser.Config parserConfig = SqlParser.config().withCaseSensitive(false);

    SchemaPlus schema = LoadData(Path.of(arg3), "jdbc:duckdb:");

    SchemaPlus duckDBSchema = schema;

    JdbcTable table = (JdbcTable) duckDBSchema.getTable("customer");
    duckDBSchema.add("customer", new RowCountJdbcTable(table, 150000,
        new ArrayList<Double>(List.of(150000D, 150000D, 150000D, 25D, 150000D, 140187D, 5D, 148965D))));
    table = (JdbcTable) duckDBSchema.getTable("lineitem");
    duckDBSchema.add("lineitem", new RowCountJdbcTable(table, 6001215,
        new ArrayList<Double>(List.of(1500000D, 200000D, 10000D, 7D, 50D, 933900D, 11D, 9D, 3D, 2D, 2526D, 2466D, 2554D,
            4D, 7D, 3610733D))));
    table = (JdbcTable) duckDBSchema.getTable("nation");
    duckDBSchema.add("nation", new RowCountJdbcTable(table, 25,
        new ArrayList<Double>(List.of(25D, 25D, 5D, 25D))));
    table = (JdbcTable) duckDBSchema.getTable("orders");
    duckDBSchema.add("orders", new RowCountJdbcTable(table, 1500000,
        new ArrayList<Double>(List.of(1500000D, 99996D, 3D, 1464556D, 2406D, 5D, 1000D, 1D, 1387872D))));
    table = (JdbcTable) duckDBSchema.getTable("part");
    duckDBSchema.add("part", new RowCountJdbcTable(table, 200000,
        new ArrayList<Double>(List.of(200000D, 199997D, 5D, 25D, 150D, 50D, 40D, 20899D, 130704D))));
    table = (JdbcTable) duckDBSchema.getTable("partsupp");
    duckDBSchema.add("partsupp", new RowCountJdbcTable(table, 800000,
        new ArrayList<>(List.of(200000D, 10000D, 9999D, 99865D, 772739D))));
    table = (JdbcTable) duckDBSchema.getTable("region");
    duckDBSchema.add("region", new RowCountJdbcTable(table, 5,
        new ArrayList<>(List.of(5D, 5D, 5D))));
    table = (JdbcTable) duckDBSchema.getTable("supplier");
    duckDBSchema.add("supplier", new RowCountJdbcTable(table, 10000,
        new ArrayList<>(List.of(10000D, 10000D, 10000D, 25D, 10000D, 9955D, 9998D))));

    String viewSql = "SELECT "
        + "  \"l_suppkey\" AS \"supplier_no\", "
        + "  SUM(\"l_extendedprice\" * (1 - \"l_discount\")) "
        + "    AS \"total_revenue\" "
        + "FROM \"lineitem\" "
        + "WHERE \"l_shipdate\" >= DATE '1995-01-01' "
        + "  AND \"l_shipdate\" < DATE '1995-01-01' + INTERVAL '3' MONTH "
        + "GROUP BY \"l_suppkey\"";
    duckDBSchema.add("revenue0r15721", ViewTable.viewMacro(duckDBSchema, viewSql, ImmutableList.of("DuckDB"),
        ImmutableList.of("DuckDB", "revenue0r15721"), false).apply(ImmutableList.of()));

    FrameworkConfig config = Frameworks.newConfigBuilder()
        .defaultSchema(schema)
        .parserConfig(parserConfig)
        .programs(Programs.ofRules())
        .build();

    try (Connection connection = DriverManager.getConnection("jdbc:calcite:caseSensitive=false")) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      calciteConnection.getRootSchema().add("DuckDB", schema.unwrap(JdbcSchema.class));

      List<QueryMeta> metas = ReadSqlFromQuriesDir(Path.of(arg1), config, Path.of(arg2));

      RelMetadataProvider provider = ChainedRelMetadataProvider.of(
          ImmutableList.of(
              SelectivityHandler.SOURCE,
              DistinctRowCount.SOURCE,
              JdbcJoinCost.SOURCE,
              DefaultRelMetadataProvider.INSTANCE));

      // reduce expressions.
      Program reduceExpression = Programs.hep(
          ImmutableList.of(
              CoreRules.FILTER_REDUCE_EXPRESSIONS,
              CoreRules.JOIN_REDUCE_EXPRESSIONS,
              CoreRules.PROJECT_REDUCE_EXPRESSIONS),
          true,
          provider);

      // expand the sub queries
      Program expandSubquery = Programs.subQuery(provider);

      // decorrelate
      Program decorrelate = (planner, rel, requiredOutputTraits, materializations, lattices) -> RelDecorrelator
          .decorrelateQuery(
              rel,
              RelFactories.LOGICAL_BUILDER.create(
                  rel.getCluster(),
                  null));

      // filter into join.

      Program filterIntoJoin = Programs.hep(
          ImmutableList.of(CoreRules.FILTER_INTO_JOIN),
          true,
          provider);

      // join pull factors

      Program joinPullFactors = Programs.hep(
          ImmutableList.of(JoinPullFactorsRule.Config.DEFAULT.toRule()),
          true,
          provider);

      // trim

      Program trim = ((planner, rel, requiredOutputTraits, materializations, lattices) -> {
        RelBuilder builder = RelFactories.LOGICAL_BUILDER.create(
            rel.getCluster(), null);

        return new RelFieldTrimmer(null, builder).trim(rel);
      });

      // expand distinct aggregates
      Program expandDistinct = Programs.hep(
          ImmutableList.of(
              CoreRules.AGGREGATE_EXPAND_DISTINCT_AGGREGATES,
              CoreRules.AGGREGATE_REDUCE_FUNCTIONS),
          true,
          provider);

      // to enumerable
      List<RelOptRule> physicalRules = Arrays.asList(
          EnumerableRules.ENUMERABLE_JOIN_RULE,
          EnumerableRules.ENUMERABLE_PROJECT_RULE,
          EnumerableRules.ENUMERABLE_FILTER_RULE,
          EnumerableRules.ENUMERABLE_AGGREGATE_RULE,
          EnumerableRules.ENUMERABLE_SORT_RULE,
          EnumerableRules.ENUMERABLE_TABLE_SCAN_RULE,
          EnumerableRules.ENUMERABLE_LIMIT_SORT_RULE,
          EnumerableRules.ENUMERABLE_VALUES_RULE);

      Program enumerableJoinRule = (planner, rel, requiredTraits, materializations, lattices) -> {
        // join to Enumerable

        RelOptCluster cluster = rel.getCluster();
        cluster.setMetadataProvider(provider);
        cluster.invalidateMetadataQuery();

        planner.clear();
        physicalRules.forEach(planner::addRule);

        RelTraitSet targetTraits = rel.getTraitSet().replace(EnumerableConvention.INSTANCE);

        planner.setRoot(rel);
        RelNode target = planner.changeTraits(rel, targetTraits);
        planner.setRoot(target);
        return planner.findBestExp();
      };

      // join to correlate
      // Program JoinToCorrelate = Programs.hep(
      // ImmutableList.of(CoreRules.JOIN_TO_CORRELATE),
      // true,
      // DefaultRelMetadataProvider.INSTANCE);

      Map<String, String> plans_tracer = new LinkedHashMap<>();

      String physical_phase = "optimization 08 physical: join to enumerable";
      Program chain = Programs.sequence(
          traced("optimization 01 reduce expressions", reduceExpression, plans_tracer::put, TIMING_LOG),
          traced("optimization 02 expand subquery", expandSubquery, plans_tracer::put, TIMING_LOG),
          traced("optimization 03 decorrelate", decorrelate, plans_tracer::put, TIMING_LOG),
          traced("optimization 04 filter into join", filterIntoJoin, plans_tracer::put, TIMING_LOG),
          traced("optimization 05 join pull factors", joinPullFactors, plans_tracer::put, TIMING_LOG),
          traced("optimization 06 trim", trim, plans_tracer::put, TIMING_LOG),
          traced("optimization 07 expand distinct agg", expandDistinct, plans_tracer::put, TIMING_LOG),
          traced(physical_phase, enumerableJoinRule, plans_tracer::put, TIMING_LOG),
          Programs.calc(DefaultRelMetadataProvider.INSTANCE));

      // duckdb connection

      for (QueryMeta meta : metas) {
        System.out.println("\t query: " + meta.fileName);
        // Heuristic rules implementation
        // RelNode optimizedWithHep = meta.relNode;

        RelOptCluster cluster = meta.relNode.getCluster();

        cluster.setMetadataProvider(provider);
        cluster.invalidateMetadataQuery();

        CalciteTimingTracer timer = new CalciteTimingTracer(TIMING_LOG, "optimize begin");

        RelNode physicalPlan = chain.run(
            meta.relNode.getCluster().getPlanner(),
            meta.relNode,
            meta.relNode.getTraitSet(),
            Collections.emptyList(),
            Collections.emptyList());

        timer.traceTime("optimize end");

        timer.traceTime("write begin");

        // try (BufferedWriter writer = Files.newBufferedWriter(Path.of(arg2)
        // .resolve("process_relNode_" + meta.fileName.substring(0,
        // meta.fileName.length() - 4) + ".sql"))) {
        // for (Map.Entry<String, String> entry : plans_tracer.entrySet()) {
        // writer.write("================" + entry.getKey() + "================");
        // writer.newLine();
        // writer.write(entry.getValue());
        // writer.newLine();
        // writer.newLine();
        // }
        // }

        SerializePlan(
            physicalPlan,
            Path.of(arg2).resolve(meta.fileName.substring(0, meta.fileName.length() - 4) + "_optimized.txt")
                .toFile(),
            "");

        SqlDialect normalDialect = PostgresqlSqlDialect.DEFAULT;

        SqlNode sqlNode = new RelToSqlConverter(normalDialect)
            .visitRoot(physicalPlan)
            .asStatement();

        String sql = sqlNode.toSqlString(normalDialect).getSql();

        // Write the Result Optimized Sql
        Files.writeString(
            Path.of(arg2).resolve(meta.fileName.substring(0, meta.fileName.length() - 4)
                + "_optimized.sql"),
            sql);

        // SerializePlan(physicalPlan, Path.of(arg2).resolve("physicalPlan_relNode_" +
        // meta.fileName).toFile(),
        // "physicalPlan");

        timer.traceTime("write end");

        executePhysical(physicalPlan, calciteConnection,
            Path.of(arg2), meta.fileName, timer);

        plans_tracer.clear();
        // optimizedWithHep = RelDecorrelator.decorrelateQuery(meta.relNode,
        // RelFactories.LOGICAL_BUILDER.create(meta.relNode.getCluster(), null));
        // optimizedWithHep = new RelFieldTrimmer(null,
        // RelFactories.LOGICAL_BUILDER.create(meta.relNode.getCluster(), null))
        // .trim(optimizedWithHep);
      }

    }

  }
}
