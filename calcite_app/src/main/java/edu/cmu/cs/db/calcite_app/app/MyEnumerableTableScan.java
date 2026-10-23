package edu.cmu.cs.db.calcite_app.app;

import org.apache.calcite.adapter.enumerable.EnumerableRel;
import org.apache.calcite.rel.core.TableScan;

public class MyEnumerableTableScan extends TableScan
    implements EnumerableRel {

}
