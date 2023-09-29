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
import similarities.functions.EuclideanSimilarity;
import tools.ClusterKit;

public class EuclideanSimilarityTest extends GeneralTest {
    private ClusterKit kit;
    private double[][] data;

    @Before
    public void setUp(){
        super.setUp();
        runParameters.setSimMetric(new EuclideanSimilarity(runParameters));
        runParameters.computePairwiseDistances();

        kit = new ClusterKit(runParameters);
        data = runParameters.getData();
    }

    @Test
    public void testSim(){
        double[] v1 = data[kit.C1.get(0)];
        double[] v2 = data[kit.C1.get(1)];

        double targetSim = 1 / (1 + lib.euclidean(v1,v2));
        double sim = runParameters.getSimMetric().sim(v1,v2);

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
    public void testEmpiricalSimilarityBounds3(){
        double lb = .32570783646089335;
        double ub = .5277079267913711;
        double maxLBSubset = .4076664097211445;

        Cluster[] LHS = new Cluster[]{kit.C1};
        Cluster[] RHS = new Cluster[]{kit.C3, kit.C4};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().empiricalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
        Assert.assertEquals(maxLBSubset, bounds.getMaxLowerBoundSubset(), 0.0001);
    }

    @Test
    public void testEmpiricalSimilarityBounds4(){
        double lb = 0.2782872197618067;
        double ub = 1;
        double maxLBSubset = 0.4079706791559631;

        Cluster[] LHS = new Cluster[]{kit.C1,kit.C2};
        Cluster[] RHS = new Cluster[]{kit.C3, kit.C4};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().empiricalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
        Assert.assertEquals(maxLBSubset, bounds.getMaxLowerBoundSubset(), 0.0001);
    }


    @Test
    public void testTheoreticalDistanceBounds(){
        double centroidDistance = lib.normedAngle(kit.C1.getCentroid(), kit.C2.getCentroid());
        double lb = FastMath.max(0,centroidDistance - kit.C1.getRadius() - kit.C2.getRadius());
        double ub = FastMath.max(0,centroidDistance + kit.C1.getRadius() + kit.C2.getRadius());

        ClusterPair cp = runParameters.getSimMetric().theoreticalDistanceBounds(kit.C1, kit.C2);

        Assert.assertEquals(lb, cp.getBounds().getLB(), 0.0001);
        Assert.assertEquals(ub, cp.getBounds().getUB(), 0.0001);
    }

    @Test
    public void testTheoreticalSimilarityBounds3(){
        runParameters.setEmpiricalBounding(false);

        double[] CXc = kit.C1.getCentroid();
        double CXr = kit.C1.getRadius();
        double[] CYc = lib.add(kit.C3.getCentroid(), kit.C4.getCentroid());
        double CYr = kit.C3.getRadius() + kit.C4.getRadius();

        double centroidDistance = lib.euclidean(CXc, CYc);
        double lb = 1 / (1 + FastMath.max(0,centroidDistance + CXr + CYr));
        double ub = 1 / (1 + FastMath.max(0,centroidDistance - CXr - CYr));

        Cluster[] LHS = new Cluster[]{kit.C1};
        Cluster[] RHS = new Cluster[]{kit.C3, kit.C4};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().theoreticalSimilarityBounds(CC);

        Assert.assertTrue(lb <= bounds.getLB());
        Assert.assertTrue(ub >= bounds.getUB());
    }

    @Test
    public void testTheoreticalSimilarityBounds4(){
        runParameters.setEmpiricalBounding(false);

        double[] CXc = lib.add(kit.C1.getCentroid(), kit.C2.getCentroid());
        double CXr = kit.C1.getRadius() + kit.C2.getRadius();
        double[] CYc = lib.add(kit.C3.getCentroid(), kit.C4.getCentroid());
        double CYr = kit.C3.getRadius() + kit.C4.getRadius();

        double centroidDistance = lib.euclidean(CXc, CYc);
        double lb = 1 / (1 + FastMath.max(0,centroidDistance + CXr + CYr));
        double ub = 1 / (1 + FastMath.max(0,centroidDistance - CXr - CYr));

        Cluster[] LHS = new Cluster[]{kit.C1, kit.C2};
        Cluster[] RHS = new Cluster[]{kit.C3, kit.C4};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().theoreticalSimilarityBounds(CC);

        Assert.assertTrue(lb <= bounds.getLB());
        Assert.assertTrue(ub >= bounds.getUB());
    }

}
