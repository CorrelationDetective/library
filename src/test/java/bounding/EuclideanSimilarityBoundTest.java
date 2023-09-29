package bounding;

import _aux.GeneralTest;
import _aux.lib;
import clustering.Cluster;
import clustering.HierarchicalClustering;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import similarities.SimEnum;

public class EuclideanSimilarityBoundTest extends GeneralTest {
    private HierarchicalClustering HC;

    @Before
    public void setUp() {
        super.setUp();
        HC = new HierarchicalClustering(runParameters);

       runParameters.setSimMetricName(SimEnum.EUCLIDEAN_SIMILARITY);
       runParameters.init();
       runParameters.computePairwiseDistances();
    }

    @Test
    public void testCCBounds() {
        int s = 100;

//        Do 100 tests on randomly created CCs
        for (int i = 0; i < s; i++) {
            runParameters.getSimMetric().clearCache();

            int i1 = runParameters.getRandomGenerator().nextInt(runParameters.getNVectors());
            int i2 = runParameters.getRandomGenerator().nextInt(runParameters.getNVectors());
            int i3 = runParameters.getRandomGenerator().nextInt(runParameters.getNVectors());
            int i4 = runParameters.getRandomGenerator().nextInt(runParameters.getNVectors());

            if (i1 == i2 || i1 == i3 || i1 == i4 || i2 == i3 || i2 == i4 || i3 == i4) {
                i--;
                continue;
            }

            Cluster c1 = new Cluster(i1, runParameters);
            c1.setId(1);
            c1.addPoint(i2);
            c1.finalize();

            Cluster c2 = new Cluster(i3, runParameters);
            c2.setId(2);
            c2.finalize();

            Cluster c3 = new Cluster(i4, runParameters);
            c3.setId(3);
            c3.finalize();

            HC.globalClusterID = 3;

            Cluster[] LHS = new Cluster[]{c1};
            Cluster[] RHS = new Cluster[]{c2, c3};
            ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 1, runParameters.isAllowVectorOverlap());

//        Compute actual bounds
            double[][] data = runParameters.getData();
            double[] data34 = lib.add(data[i3], data[i4]);
            double msim1 = 1 / (1 + lib.euclidean(data[i1], data34));
            double msim2 = 1 / (1 + lib.euclidean(data[i2], data34));

//        Get method computed bounds
            runParameters.getSimMetric().bound(CC);

//        Check that bounds are correct
            Assert.assertFalse(CC.bounds.getLB() - FastMath.min(msim1, msim2) > 1e-3);
            Assert.assertFalse(FastMath.max(msim1, msim2) - CC.bounds.getUB() > 1e-3);
        }
    }
}
