package similarities;

import _aux.GeneralTest;
import _aux.lib;
import bounding.ClusterBounds;
import bounding.ClusterCombination;
import bounding.ClusterPair;
import clustering.Cluster;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import similarities.functions.Multipole;
import tools.ClusterKit;

public class MultipoleTest extends GeneralTest {
    private ClusterKit kit;
    private double[][] data;

    @Before
    public void setUp(){
        super.setUp();
        runParameters.setSimMetric(new Multipole(runParameters));
        kit = new ClusterKit(runParameters);
        data = runParameters.getData();
    }

    @Test
    public void testSim(){
        double[] v1 = data[kit.C1.get(0)];
        double[] v2 = data[kit.C1.get(1)];

        double[] z1 = lib.l2norm(v1);
        double[] z2 = lib.l2norm(v2);

        double targetSim = lib.dot(z1, z2);
        double sim = runParameters.getSimMetric().sim(z1,z2);

        Assert.assertEquals(targetSim, sim, 0.0001);
    }

    @Test
    public void testEmpiricalDistanceBounds(){
        double lb = 0.9426551980999137;
        double ub = 1.6451619408764522;

        ClusterPair cp = runParameters.getSimMetric().empiricalDistanceBounds(kit.C1, kit.C2);

        Assert.assertEquals(lb, cp.getBounds().getLB(), 0.0001);
        Assert.assertEquals(ub, cp.getBounds().getUB(), 0.0001);
    }

    @Test
    public void testEmpiricalSimilarityBounds1(){
        double lb = 0;
        double ub = 0;
        double maxLBSubset = -1;

        Cluster[] LHS = new Cluster[]{kit.C1};
        Cluster[] RHS = new Cluster[0];
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().empiricalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
        Assert.assertEquals(maxLBSubset, bounds.getMaxLowerBoundSubset(), 0.0001);
    }

    @Test
    public void testEmpiricalSimilarityBounds2(){
        double lb = -.0742970896933719;
        double ub = .5876417217925668;
        double maxLBSubset = -1;

        Cluster[] LHS = new Cluster[]{kit.C1, kit.C2};
        Cluster[] RHS = new Cluster[0];
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().empiricalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
        Assert.assertEquals(maxLBSubset, bounds.getMaxLowerBoundSubset(), 0.0001);
    }


    @Test
    public void testTheoreticalDistanceBounds(){
        double centroidDistance = lib.normedAngle(kit.C1.getCentroid(), kit.C2.getCentroid());
        double lb = FastMath.max(0, centroidDistance - kit.C1.getRadius() - kit.C2.getRadius());
        double ub = FastMath.min(FastMath.PI, centroidDistance + kit.C1.getRadius() + kit.C2.getRadius());


        ClusterPair cp = runParameters.getSimMetric().theoreticalDistanceBounds(kit.C1, kit.C2);

        Assert.assertEquals(lb, cp.getBounds().getLB(), 0.0001);
        Assert.assertEquals(ub, FastMath.min(FastMath.PI,cp.getBounds().getUB()), 0.0001);
    }

    @Test
    public void testTheoreticalSimilarityBounds1(){
        double lb = 0;
        double ub = 0;
        double maxLBSubset = -1;

        Cluster[] LHS = new Cluster[]{kit.C1};
        Cluster[] RHS = new Cluster[0];
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().theoreticalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
        Assert.assertEquals(maxLBSubset, bounds.getMaxLowerBoundSubset(), 0.0001);
    }

    @Test
    public void testTheoreticalSimilarityBounds2(){
        double lb = -.0742970896933719;
        double ub = .5876417217925668;
        double maxLBSubset = -1;

        Cluster[] LHS = new Cluster[]{kit.C1, kit.C2};
        Cluster[] RHS = new Cluster[0];
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().theoreticalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
        Assert.assertEquals(maxLBSubset, bounds.getMaxLowerBoundSubset(), 0.0001);
    }

}
