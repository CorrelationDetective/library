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
import similarities.functions.PearsonCorrelation;
import tools.ClusterKit;


public class PearsonCorrelationTest extends GeneralTest {
    private ClusterKit kit;

    @Before
    public void setUp(){
        super.setUp();
        runParameters.setSimMetric(new PearsonCorrelation(runParameters));
        kit = new ClusterKit(runParameters);
    }

    @Test
    public void testSim(){
        double[] v1 = runParameters.getData()[kit.C1.get(0)];
        double[] v2 = runParameters.getData()[kit.C1.get(1)];

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
    public void testEmpiricalSimilarityBounds3(){
        double lb = -.081170059936065;
        double ub = .7576306989159439;
        double maxLBSubset = -.055584173366012846;

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
        double lb = -.1306298664325515;
        double ub = 1.0345202740400414;
        double maxLBSubset = -.052927657692114216;

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
        double lb = FastMath.max(0, centroidDistance - kit.C1.getRadius() - kit.C2.getRadius());
        double ub = FastMath.min(FastMath.PI, centroidDistance + kit.C1.getRadius() + kit.C2.getRadius());

        ClusterPair cp = runParameters.getSimMetric().theoreticalDistanceBounds(kit.C1, kit.C2);

        Assert.assertEquals(lb, cp.getBounds().getLB(), 0.0001);
        Assert.assertEquals(ub, FastMath.min(FastMath.PI,cp.getBounds().getUB()), 0.0001);
    }

    @Test
    public void testTheoreticalSimilarityBounds3(){
        double lb = -.081170059936065;
        double ub = .7576306989159439;
        double maxLBSubset = -.055584173366012846;

        Cluster[] LHS = new Cluster[]{kit.C1};
        Cluster[] RHS = new Cluster[]{kit.C3, kit.C4};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().theoreticalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
        Assert.assertEquals(maxLBSubset, bounds.getMaxLowerBoundSubset(), 0.0001);
    }

    @Test
    public void testTheoreticalSimilarityBounds4(){
        double lb = -.1306298664325515;
        double ub = 1.0345202740400414;
        double maxLBSubset = -.052927657692114216;

        Cluster[] LHS = new Cluster[]{kit.C1, kit.C2};
        Cluster[] RHS = new Cluster[]{kit.C3, kit.C4};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().theoreticalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
        Assert.assertEquals(maxLBSubset, bounds.getMaxLowerBoundSubset(), 0.0001);
    }

}
