package edu.cmu.cs.db.calcite_app.app;

import java.nio.file.DirectoryStream.Filter;
import org.immutables.value.Value;
import org.apache.calcite.rel.rules.TransformationRule;

import org.apache.calcite.plan.RelRule;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rules.TransformationRule;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.plan.RelOptCostFactory;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.rel.logical.LogicalJoin;

@Value.Enclosing
public final class JoinPullFactorsRule extends RelRule<JoinPullFactorsRule.Config>
    implements TransformationRule {

  protected JoinPullFactorsRule(Config config) {
    super(config);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    LogicalJoin join = call.rel(0);

    RexNode oldCondition = join.getCondition();

    if (join.getJoinType() != JoinRelType.INNER
        || !RexUtil.isDeterministic(oldCondition)) {
      return;
    }

    RexNode newCondition = RexUtil.pullFactors(join.getCluster().getRexBuilder(), oldCondition);

    if (newCondition.equals(oldCondition)) {
      return;
    }

    call.transformTo(join.copy(
        join.getTraitSet(),
        newCondition,
        join.getLeft(),
        join.getRight(),
        join.getJoinType(),
        join.isSemiJoinDone()));

  }

  @Value.Immutable(singleton = true)
  public interface Config extends RelRule.Config {

    Config DEFAULT = ImmutableJoinPullFactorsRule.Config.of()
        .withOperandSupplier(b -> b.operand(LogicalJoin.class).anyInputs())
        .as(Config.class);

    @Override
    default JoinPullFactorsRule toRule() {
      return new JoinPullFactorsRule(this);
    }
  }

}
