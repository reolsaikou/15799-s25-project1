
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

### bug

1. some queries(such as q19) is transformed to nested loop join.
2. dialect of DuckDB needs to be added by self, override supportsCharset().
3. Calcite's
          PreparedStatement stmt = runner.prepareStatement(optimizedLogical))
may do unexpected heuristic transform.
It's a JDBC convention process, which only accepts logical plan.
4. the cost increases after I add the rowCount statistic.
