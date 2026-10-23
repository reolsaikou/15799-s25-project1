package edu.cmu.cs.db.calcite_app.app;

import static org.apache.calcite.util.NumberUtil.divide;
import static org.apache.calcite.util.NumberUtil.max;
import static org.apache.calcite.util.NumberUtil.multiply;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.BuiltInMetadata;
import org.apache.calcite.rel.metadata.MetadataDef;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMdDistinctRowCount;
import org.apache.calcite.rel.metadata.RelMdSelectivity;
import org.apache.calcite.rel.metadata.RelMdUtil;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.util.ImmutableBitSet;

public final class SelectivityHandler extends RelMdSelectivity
    implements BuiltInMetadata.Selectivity.Handler {

  public static final RelMetadataProvider SOURCE = ReflectiveRelMetadataProvider.reflectiveSource(
      new SelectivityHandler(),
      BuiltInMetadata.Selectivity.Handler.class);

  private static Double getColumnNdv(
      Join join,
      RelMetadataQuery mq,
      RexInputRef ref) {

    int index = ref.getIndex();
    int leftCount = join.getLeft().getRowType().getFieldCount();

    RelNode input;
    int localIndex;

    if (index < leftCount) {
      input = join.getLeft();
      localIndex = index;
    } else {
      input = join.getRight();
      localIndex = index - leftCount;
    }

    return mq.getDistinctRowCount(
        input, ImmutableBitSet.of(localIndex), null);
  }

  @Override
  public MetadataDef<BuiltInMetadata.Selectivity> getDef() {
    return BuiltInMetadata.Selectivity.DEF;
  }

  @Override
  public Double getSelectivity(
      Join relNode,
      RelMetadataQuery mq,
      RexNode predicate) {
    Join join = (Join) relNode;

    if (predicate == null || predicate.isAlwaysTrue()) {
      return 1D;
    }

    if (predicate.isAlwaysFalse()) {
      return 0D;
    }

    if (!(predicate instanceof RexCall)
        || !predicate.isA(SqlKind.EQUALS)) {
      return RelMdUtil.guessSelectivity(predicate);
    }

    RexCall equals = (RexCall) predicate;

    RexNode left = equals.getOperands().get(0);
    RexNode right = equals.getOperands().get(1);

    Double lNdv = 1D;
    Double rNdv = 1D;

    if (left instanceof RexInputRef) {
      lNdv = getColumnNdv(join, mq, (RexInputRef) left);
    } else {
      lNdv = 1D;
    }

    if (right instanceof RexInputRef) {
      rNdv = getColumnNdv(join, mq, (RexInputRef) right);
    } else {
      rNdv = 1D;
    }

    // System.err.println(
    // "join=" + join.getClass().getSimpleName()
    // + ", left=" + join.getLeft().getClass().getSimpleName()
    // + ", lNdv=" + lNdv
    // + ", right=" + join.getRight().getClass().getSimpleName()
    // + ", rNdv=" + rNdv);

    if (lNdv == null && rNdv == null) {
      return RelMdUtil.guessSelectivity(predicate);
    }

    lNdv = lNdv == null ? 1D : lNdv;
    rNdv = rNdv == null ? 1D : rNdv;

    return divide(1D, max(lNdv, rNdv));
  }

  @Override
  public Double getSelectivity(
      RelNode relNode,
      RelMetadataQuery mq,
      RexNode predicate) {

    return RelMdUtil.guessSelectivity(predicate);
  }

  @Override
  public Double getSelectivity(
      TableScan relNode,
      RelMetadataQuery mq,
      RexNode predicate) {
    if (predicate == null || predicate.isAlwaysTrue()) {
      return 1D;
    }
    if (predicate.isAlwaysFalse()) {
      return 0D;
    }
    if (!(predicate instanceof RexCall)
        || !predicate.isA(SqlKind.EQUALS)) {
      return RelMdUtil.guessSelectivity(predicate);
    }

    TableScan scan = (TableScan) relNode;

    RexCall equals = (RexCall) predicate;

    RexNode left = equals.getOperands().get(0);
    RexNode right = equals.getOperands().get(1);
    Double Ndv = 0D;

    left = RexUtil.removeCast(left);
    right = RexUtil.removeNullabilityCast(relNode.getCluster().getTypeFactory(), right);

    if (left.isA(SqlKind.INPUT_REF)) {
      int leftIndex = ((RexInputRef) left).getIndex();
      Ndv = mq.getDistinctRowCount(scan, ImmutableBitSet.of(leftIndex), null);
    } else {
      int rightIndex = ((RexInputRef) right).getIndex();
      Ndv = mq.getDistinctRowCount(scan, ImmutableBitSet.of(rightIndex), null);
    }

    return divide(1D, Ndv);
  }

}
