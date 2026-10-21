package edu.cmu.cs.db.calcite_app.app;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.adapter.jdbc.JdbcToEnumerableConverter;
import org.apache.calcite.linq4j.JoinType;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.RelMdDistinctRowCount;
import org.apache.calcite.rel.metadata.RelMdUtil;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.calcite.util.NumberUtil;
import org.apache.calcite.rel.metadata.BuiltInMetadata;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;

import static org.apache.calcite.util.NumberUtil.multiply;

public class DistinctRowCount extends RelMdDistinctRowCount
    implements BuiltInMetadata.DistinctRowCount.Handler {

  public static final RelMetadataProvider SOURCE = ReflectiveRelMetadataProvider.reflectiveSource(
      new DistinctRowCount(),
      BuiltInMetadata.DistinctRowCount.Handler.class);

  @Override
  public Double getDistinctRowCount(
      TableScan relNode,
      RelMetadataQuery mq,
      ImmutableBitSet groupKey,
      RexNode predicate) {
    if (relNode instanceof TableScan) {
      RowCountJdbcTable table = relNode.getTable().unwrap(RowCountJdbcTable.class);
      Double res = 1D;
      for (int i : groupKey) {
        res = multiply(res, table.getDistinctRowCount(i));
      }
      return res;
    }

    boolean uniq = RelMdUtil.areColumnsDefinitelyUnique(mq, relNode, groupKey);
    if (uniq) {
      return NumberUtil.multiply(mq.getRowCount(relNode),
          mq.getSelectivity(relNode, predicate));
    }
    return null;
  }

  private static String describeRel(RelNode rel) {
    if (rel == null) {
      return "null";
    }

    return rel.getClass().getSimpleName()
        + "#" + rel.getId()
        + " traits=" + rel.getTraitSet();
  }

  public Double getDistinctRowCount(
      JdbcToEnumerableConverter rel,
      RelMetadataQuery mq,
      ImmutableBitSet groupKey,
      RexNode predicate) {

    return mq.getDistinctRowCount(
        rel.getInput(),
        groupKey,
        predicate);
  }

  // @Override
  // public Double getDistinctRowCount(
  // RelSubset subset,
  // RelMetadataQuery mq,
  // ImmutableBitSet groupKey,
  // RexNode predicate) {
  //
  // RelNode best = subset.getBest();
  // RelNode original = subset.getOriginal();
  //
  // System.err.println(" best: " + describeRel(best));
  // System.err.println(" original: " + describeRel(original));
  //
  // System.err.println(
  // "[NDV START] subset#" + subset.getId()
  // + " traits=" + subset.getTraitSet()
  // + " groupKey=" + groupKey
  // + " predicate=" + predicate);
  // List<RelNode> candidates = subset.getRelList();
  //
  // System.err.println(" candidateCount=" + candidates.size());
  // for (RelNode candidate : candidates) {
  // System.err.println(" candidate: " + describeRel(candidate));
  // }
  //
  // RelNode target = best;
  // String source = "best";
  //
  // if (target == null) {
  // target = original;
  // source = "original";
  // }
  //
  // if (target == null) {
  // Double result = super.getDistinctRowCount(
  // subset, mq, groupKey, predicate);
  //
  // System.err.println(
  // "[NDV END] subset#" + subset.getId()
  // + " source=super result=" + result);
  //
  // return result;
  // }
  //
  // Double result = mq.getDistinctRowCount(
  // target, groupKey, predicate);
  //
  // System.err.println(
  // "[NDV END] subset#" + subset.getId()
  // + " source=" + source
  // + " target=" + describeRel(target)
  // + " result=" + result);
  //
  // return mq.getDistinctRowCount(target, groupKey, predicate);
  // }
}
