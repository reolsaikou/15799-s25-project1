
duckdb:

it picked a hash join and pushed the projections and filters down into the sequential scans.

A read-world scenario for disabled_optmizer of duckdb is that it allows to improve a query plan without modifying the existing optimizer.

### Overview

Calcite's query optimizer is based on the Volcano framework.

Apache Calcite offers query parsing, validation and optimization capabilities.

Calcite validate sql, via a database system supports JDBC.

CalciteSchema & JdbcSchema

Summary:
SQL (string) <--> SqlNode (AST, possibly validated) <--> RelNode (relational expression)

### Optimizing Relational Algebra

RelOptUtil.dumpPlan is useful for viewing its structure, invoked with SqlExplainFormat.TEXT and SqlExplainLevel.ALL_ATTRIBUTES during development.

Two optimizers in Calcite: HepPlanner, VolcanoPlanner, by a RelOptPlanner interface.

a bunch of planner rules: <https://calcite.apache.org/javadocAggregate/org/apache/calcite/rel/rules/package-summary.html>

core rules: <https://calcite.apache.org/javadocAggregate/org/apache/calcite/rel/rules/CoreRules.html>

Calcite's Convention is a key point.

### Improving with Statistics

1. To fix the cardinality estimate, AbstractTable could be extended.
2. Thus ScannableTable might be implemented.

Try a combination of rules and additional statistics.

### Excuting

On Calcite

On Another System: Substrait

On DuckDB: sql

### RoadMap

### the first step

1. run it up
2. load data into duckdb & create input/db/test.db
3. use Calcite to parse sql queries and print the parse tree.
4. Now HepPlanner has been used to optimize.
5. Have translate relnode to sqlnode of DuckDB.

### key points

1. It is important to decide on the order of heuristic rules.
 1.1 put the filter_into_join at the front of Join_pull_factors otherwise cause an error.
 1.2
2. row expression
3. partitioned band join
4. Explain of DuckDB is intuitive.

### bug

1. some queries(such as q19) is transformed to nested loop join.
2. dialect of DuckDB needs to be added by self, override supportsCharset().
3. Calcite's
          PreparedStatement stmt = runner.prepareStatement(optimizedLogical))
may do unexpected heuristic transform.
It's a JDBC convention process, which only accepts logical plan.
4. the cost increases after I add the rowCount statistic.
5. view isn't be expanded to a subquery.
6. 注意到q9中有大量 enumerable projection, 尝试将project push down to the jdbcTableScan.
7. Enumerator cost Bunch of Time.
8. results do not match the result of DuckDB.
9. 对于duckdb来说， 输出的结果按照row_group 划分的？ 考虑在execute engine方面有什么优化？
set threas = 1;
后输出比较常规，猜测是多个row group并行执行。

### rules

| 规则集合 | 主要用途 | 例子 |
| --- | --- | --- |
| CoreRules | 通用逻辑改写 | 过滤下推、连接重排、展开 DISTINCT、改写 AVG |
| EnumerableRules | 生成或优化 Java Enumerable 执行计划 | Hash Join、聚合、排序、Limit |
| JdbcRules | 将操作转换到 JDBC 数据源执行 | JDBC Join、Filter、Aggregate，以及返回 Enumerable 的转换 |
| Bindables | 提供 Bindable 执行相关规则 | Bindable Scan、Filter、Join |
| 各适配器的规则集合 | 将操作下推到对应引擎 | Elasticsearch、MongoDB、Druid、Spark 等 |
