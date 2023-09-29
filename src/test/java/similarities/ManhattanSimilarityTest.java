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
import similarities.functions.ManhattanSimilarity;
import tools.ClusterKit;


public class ManhattanSimilarityTest extends GeneralTest {
    private ClusterKit kit;
    private double[][] data;

    @Before
    public void setUp(){
        super.setUp();
        runParameters.setSimMetric(new ManhattanSimilarity(runParameters));
        kit = new ClusterKit(runParameters);
        data = runParameters.getData();
    }

    @Test
    public void testSim(){
        double[] v1 = data[kit.C1.get(0)];
        double[] v2 = data[kit.C1.get(1)];

        double targetSim = 1 / (1 + lib.minkowski(v1,v2,1));
        double sim = runParameters.getSimMetric().sim(v1,v2);

        Assert.assertEquals(targetSim, sim, 0.0001);
    }

    @Test
    public void testEmpiricalDistanceBounds(){
        double lb = 21.18461677530389;
        double ub = 37.910496127624796;

        ClusterPair cp = runParameters.getSimMetric().empiricalDistanceBounds(kit.C1, kit.C2);

        Assert.assertEquals(lb, cp.getBounds().getLB(), 0.0001);
        Assert.assertEquals(ub, cp.getBounds().getUB(), 0.0001);
    }

    @Test
    public void testTheoreticalDistanceBounds(){
        double centroidDistance = lib.minkowski(kit.C1.getCentroid(), kit.C2.getCentroid(),1);
        double lb = FastMath.max(0,centroidDistance - kit.C1.getRadius() - kit.C2.getRadius());
        double ub = FastMath.max(0,centroidDistance + kit.C1.getRadius() + kit.C2.getRadius());

        ClusterPair cp = runParameters.getSimMetric().theoreticalDistanceBounds(kit.C1, kit.C2);

        Assert.assertEquals(lb, cp.getBounds().getLB(), 0.0001);
        Assert.assertEquals(ub, cp.getBounds().getUB(), 0.0001);
    }

    @Test
    public void testTheoreticalSimilarityBounds3(){
        double[] CXc = kit.C1.getCentroid();
        double CXr = kit.C1.getRadius();
        double[] CYc = lib.add(kit.C3.getCentroid(), kit.C4.getCentroid());
        double CYr = kit.C3.getRadius() + kit.C4.getRadius();

        double centroidDistance = lib.minkowski(CXc, CYc,1);
        double lb = 1 / (1 + FastMath.max(0,centroidDistance + CXr + CYr));
        double ub = 1 / (1 + FastMath.max(0,centroidDistance - CXr - CYr));

        Cluster[] LHS = new Cluster[]{kit.C1};
        Cluster[] RHS = new Cluster[]{kit.C3, kit.C4};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().theoreticalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
    }

    @Test
    public void testTheoreticalSimilarityBounds4(){
        double[] CXc = lib.add(kit.C1.getCentroid(), kit.C2.getCentroid());
        double CXr = kit.C1.getRadius() + kit.C2.getRadius();
        double[] CYc = lib.add(kit.C3.getCentroid(), kit.C4.getCentroid());
        double CYr = kit.C3.getRadius() + kit.C4.getRadius();

        double centroidDistance = lib.minkowski(CXc, CYc,1);
        double lb = 1 / (1 + FastMath.max(0,centroidDistance + CXr + CYr));
        double ub = 1 / (1 + FastMath.max(0,centroidDistance - CXr - CYr));

        Cluster[] LHS = new Cluster[]{kit.C1, kit.C2};
        Cluster[] RHS = new Cluster[]{kit.C3, kit.C4};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, 0, runParameters.isAllowVectorOverlap());
        ClusterBounds bounds = runParameters.getSimMetric().theoreticalSimilarityBounds(CC);

        Assert.assertEquals(lb,  bounds.getLB(), 0.0001);
        Assert.assertEquals(ub,  bounds.getUB(), 0.0001);
    }

}
