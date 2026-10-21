package edu.cmu.cs.db.calcite_app.app;

import static org.apache.calcite.util.NumberUtil.add;
import static org.apache.calcite.util.NumberUtil.multiply;

import org.apache.calcite.adapter.enumerable.EnumerableRules;
import org.apache.calcite.adapter.jdbc.JdbcRules;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.BuiltInMetadata;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

public final class JdbcJoinCost
    implements BuiltInMetadata.NonCumulativeCost.Handler {
  public static final RelMetadataProvider SOURCE = ReflectiveRelMetadataProvider.reflectiveSource(
      new JdbcJoinCost(),
      BuiltInMetadata.NonCumulativeCost.Handler.class);

  public RelOptCost NonCumulativeCost(
      JdbcRules.JdbcJoin join,
      RelMetadataQuery mq) {
    Double left = mq.getRowCount(join.getLeft());
    Double right = mq.getRowCount(join.getRight());
    Double output = mq.getRowCount(join);

    if (left == null || right == null || output == null) {
      return null;
    }

    Double work;

    work = add(right, multiply(left, right));
    work = add(work, output);

    return join.getCluster().getPlanner().getCostFactory().makeCost(work, 0, 0);
  }

  @Override
  public RelOptCost getNonCumulativeCost(RelNode rel, RelMetadataQuery mq) {

    if (rel instanceof JdbcRules.JdbcJoin) {
      return NonCumulativeCost((JdbcRules.JdbcJoin) rel, mq);
    }

    return rel.computeSelfCost(rel.getCluster().getPlanner(), mq);
  }
}
