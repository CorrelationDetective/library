package bounding;

import _aux.GeneralTest;
import _aux.lists.FastArrayList;
import core.RunParameters;
import org.junit.Before;
import queries.ResultTuple;
import clustering.Cluster;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ClusterCombinationTest extends GeneralTest {
    private Cluster C1;
    private Cluster C2;
    private Cluster C3;
    private Cluster c1;
    private Cluster c2;
    private Cluster c3;
    private ClusterCombination CC;


    @Before
    public void setUp(){
        super.setUp();

        C1 = new Cluster(0,runParameters);
        C2 = new Cluster(5,runParameters); C2.setId(1);
        C3 = new Cluster(10, runParameters); C3.setId(2);

        for (int i = 1; i < 5; i++) {
            int p1 = i*3;
            int p2 = 1+i*3;
            int p3 = 2+i*3;
            C1.addPoint(p1);
            C2.addPoint(p2);
            C3.addPoint(p3);

//        Make singleton subclusters
            Cluster c1 = new Cluster(p1, runParameters); c1.finalize(); C1.addChild(c1); c1.setId(p1);
            Cluster c2 = new Cluster(p2, runParameters); c2.finalize(); C2.addChild(c2); c2.setId(p2);
            Cluster c3 = new Cluster(p3, runParameters); c3.finalize(); C3.addChild(c3); c3.setId(p3);
        }

        C1.finalize();
        C2.finalize();
        C3.finalize();
        CC = new ClusterCombination(
                new Cluster[]{C1},
                new Cluster[]{C2,C3},
                0,
                (long) C1.size() * C2.size() * C3.size(),
                runParameters.isAllowVectorOverlap()
        );

        c1 = C1.getChildren().get(0);
        c2 = C2.getChildren().get(0);
        c3 = C3.getChildren().get(0);
    }

    @Test
    public void testSimpleMethods(){
        //      Test get clusters (test if size is LHS+RHS)
        Assert.assertEquals(3, CC.getClusters().length);

//        Test size
        Assert.assertEquals((int) Arrays.stream(CC.getClusters()).map(Cluster::size).reduce((a,b)->a*b).orElse(0), CC.size());

//      Test bound (check if bounds are actually set and that LB <= UB)
        runParameters.getSimMetric().bound(CC);

        Assert.assertTrue(CC.bounds.getLB() >= -1);
        Assert.assertTrue(CC.bounds.getUB() >= -1);
        Assert.assertTrue(CC.bounds.getLB() <= CC.bounds.getUB());

        CC.bounds.update(new ClusterBounds(0, 1, 0));
        CC.bounds.update(new ClusterBounds(0.2, 1, 0));

        //      Test get and set bounds (set bounds, get bounds, check if you get proper min/max)
        Assert.assertEquals(0.2, CC.bounds.getLB(), 0.0001);

        //    Test get singletons (see if you get only ccs that are singletons)
        FastArrayList<ClusterCombination> singletons = CC.getSingletons();
        for (ClusterCombination cc: singletons){
            Assert.assertEquals(3, cc.getClusters().length);
        }
    }

    @Test
    public void testSingletonMethods(){
//        Make singleton cc
        ClusterCombination singletonCC = new ClusterCombination(
                new Cluster[]{c1},
                new Cluster[]{c2, c3},
                1,
                1,
                runParameters.isAllowVectorOverlap()
        );


        // Test is singleton (make CC with one point)
        Assert.assertTrue(singletonCC.isSingleton());

        //    Test to result tuple (check if everything is filled in properly, i.e. good size lists and right sim)
        runParameters.getSimMetric().bound(singletonCC);
        ResultTuple resultTuple = singletonCC.toResultTuple(runParameters.getHeaders());

        Assert.assertEquals((int) singletonCC.getLHS()[0].centroidIdx, resultTuple.getLHS()[0]);
        Assert.assertEquals((int) singletonCC.getRHS()[0].centroidIdx, resultTuple.getRHS()[0]);
        Assert.assertEquals((int) singletonCC.getRHS()[1].centroidIdx, resultTuple.getRHS()[1]);
        Assert.assertEquals(singletonCC.getLHS().length, resultTuple.getLHS().length);
        Assert.assertEquals(singletonCC.getRHS().length, resultTuple.getRHS().length);
        Assert.assertEquals(singletonCC.getBounds().getLB(), resultTuple.getSimilarity(), 0.001);
    }


    @Test
    public void testSplitting(){
//    Test cluster combination split (make CC with potential duplicates and see if they are filtered out)
        ClusterCombination childCC = new ClusterCombination(
                new Cluster[]{C1},
                new Cluster[]{c2, C3},
                0,
                C1.size() + c2.size() + C3.size(),
                runParameters.isAllowVectorOverlap()
        );

        FastArrayList<ClusterCombination> splits = CC.split();
        FastArrayList<ClusterCombination> splitsList = new FastArrayList<>(splits);

        Assert.assertEquals(4, splits.size());
        Assert.assertTrue(splitsList.contains(childCC));
    }






}
