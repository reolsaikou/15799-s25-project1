package edu.cmu.cs.db.calcite_app.app;

import static org.apache.calcite.util.NumberUtil.multiply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.AbstractQueryableTable;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.adapter.jdbc.JdbcTable;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.linq4j.JoinType;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.calcite.util.NumberUtil;
import org.apache.calcite.rel.metadata.BuiltInMetadata;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMdUtil;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.metadata.BuiltInMetadata.Collation;

public final class RowCountJdbcTable extends AbstractQueryableTable
    implements ScannableTable {

  private final JdbcTable delegate;
  private final Statistic statistic;
  private ArrayList<Double> NDVs;
  // private final List<Objects[]> rows;

  public RowCountJdbcTable(JdbcTable delegate, double rowCount, ArrayList<Double> NDVs) {
    super(delegate.getElementType());

    if (!Double.isFinite(rowCount) || rowCount < 0) {
      throw new IllegalArgumentException("rowCount must be finite and non-negative");
    }

    this.delegate = Objects.requireNonNull(delegate);

    this.statistic = Statistics.of(rowCount, Collections.emptyList());

    this.NDVs = NDVs;

    // this.rows = rows;
  }

  @Override
  public Statistic getStatistic() {
    return statistic;
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return delegate.getRowType(typeFactory);
  }

  @Override
  public Schema.TableType getJdbcTableType() {
    return delegate.getJdbcTableType();
  }

  // @Override
  // public RelNode toRel(
  // RelOptTable.ToRelContext context, RelOptTable relOptTable) {
  // return delegate.toRel(context, relOptTable);
  // }

  @Override
  public <T> Queryable<T> asQueryable(
      QueryProvider provider,
      SchemaPlus schema,
      String tableName) {
    return delegate.asQueryable(provider, schema, tableName);
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    if (clazz.isInstance(this)) {
      return clazz.cast(this);
    }
    if (clazz.isInstance(delegate)) {
      return clazz.cast(delegate);
    }

    return delegate.unwrap(clazz);
  }

  public Double getDistinctRowCount(
      Integer idx) {

    if (idx == null || idx < 0 || idx >= NDVs.size()) {
      return null;
    }

    return NDVs.get(idx);
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return delegate.scan(root);

  }

}
