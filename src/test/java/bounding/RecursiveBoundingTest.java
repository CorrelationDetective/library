package bounding;

import _aux.GeneralTest;
import _aux.lists.FastArrayList;
import queries.*;
import clustering.Cluster;
import clustering.HierarchicalClustering;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;


public class RecursiveBoundingTest extends GeneralTest {

    HierarchicalClustering HC;
    RecursiveBounding RB;

    @Before
    public void setUp(){
        super.setUp();

//        Get cluster tree
        HC = runParameters.initializeHC();
        HC.run();

        RB = runParameters.initializeRB();
    }

    @Test
    public void testFullRunEmpirical(){
       runParameters.setTau(.67);
       runParameters.setMinJump(0);
       runParameters.setIrreducibility(false);
       runParameters.setEmpiricalBounding(true);
       runParameters.init(false);

        //        Run recursive bounding
        RB = new RecursiveBounding(runParameters);
        ResultSet resultSet = RB.run();
        List<ResultTuple> results = resultSet.close();

        List<ResultTuple> expectedResults = new ArrayList<>(Arrays.asList(
                new ResultTuple(
                        new int[]{19},
                        new int[]{75,32},
                        .67)
        ));

        //    Test correct results - empirical bounds
        for (int i = 0; i < results.size(); i++) {
            Assert.assertTrue(expectedResults.contains(results.get(i)));
        }
        for (int i = 0; i < expectedResults.size(); i++) {
            Assert.assertTrue(results.contains(expectedResults.get(i)));
        }

        //    Test number of lookups - empirical bounds
        Assert.assertEquals(41906, runParameters.getStatBag().getNLookups().get());


        //    Test number of CCs - empirical bounds
        Assert.assertEquals(42352, runParameters.getStatBag().getNCCs().get());

        //    Test number of positive DCCs - empirical bounds
        Assert.assertEquals(1, runParameters.getStatBag().getNPosDCCs().get());

        //    Test number of negative DCCs - empirical bounds
        Assert.assertEquals(0L, runParameters.getStatBag().getNNegDCCs().get());

    }

    @Test
    public void testFullRunTheoretical(){
        runParameters.setTau(.67);
        runParameters.setMinJump(0);
        runParameters.setIrreducibility(false);
        runParameters.setEmpiricalBounding(false);
        runParameters.init(false);

        //        Run recursive bounding
        RB = new RecursiveBounding(runParameters);
        ResultSet resultSet = RB.run();
        List<ResultTuple> results = resultSet.close();

        List<ResultTuple> expectedResults = new ArrayList<>(Arrays.asList(
                new ResultTuple(
                        new int[]{19},
                        new int[]{75,32},
                        .67)
        ));

        //    Test correct results - theoretical bounds
        for (int i = 0; i < results.size(); i++) {
            Assert.assertTrue(expectedResults.contains(results.get(i)));
        }
        for (int i = 0; i < expectedResults.size(); i++) {
            Assert.assertTrue(results.contains(expectedResults.get(i)));
        }

        //    Test number of lookups - theoretical bounds
        Assert.assertEquals(0, runParameters.getStatBag().getNLookups().get());

        //        Test number of CCs - theoretical bounds
        Assert.assertEquals(666764, runParameters.getStatBag().getNCCs().get());

        //    Test number of positive DCCs - theoretical bounds
        Assert.assertEquals(1, runParameters.getStatBag().getNPosDCCs().get());
    }

//    Test unpackAndCheckMinJump
    @Test
    public void testUnpackAndCheckMinJump() {
        runParameters.setEmpiricalBounding(true);
        runParameters.setTau(.6);
        runParameters.setMinJump(0);

        Cluster C1 = HC.singletonClusters[19];
        Cluster C2 = HC.singletonClusters[75];
        Cluster C3 = HC.singletonClusters[32];


//        Make a positive cluster combination
        Cluster[] LHS = new Cluster[]{C1};
        Cluster[] RHS = new Cluster[]{C2,C3};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, C1.size() + C2.size() + C3.size(), runParameters.isAllowVectorOverlap());
        runParameters.getSimMetric().bound(CC);

//        CC similarity is 0.6711316960507971, max subset similarity is 0.5876417217925668.
//        Let's first test with insignificant minjump
        FastArrayList<ResultObject> unpacked = CC.unpackAndCheckConstraints(runParameters);
        Assert.assertEquals(1, unpacked.size());
        Assert.assertEquals(CC, unpacked.get(0));

//        Let's now test with significant minjump
        runParameters.setMinJump(0.1);
        unpacked = CC.unpackAndCheckConstraints(runParameters);
        Assert.assertEquals(0, unpacked.size());
    }

//    Test recursiveBounding function
    @Test
    public void testRecursiveBounding() {
        runParameters.setQueryType(QueryTypeEnum.TOPK);
        runParameters.setEmpiricalBounding(true);
        runParameters.setMinJump(0);
        runParameters.setIrreducibility(false);
        runParameters.init(false);
        runParameters.setRunningThreshold(new RunningThreshold(.67));

        Cluster C1 = HC.singletonClusters[19].getParent();
        Cluster C2 = HC.singletonClusters[75].getParent();
        Cluster C3 = HC.singletonClusters[32].getParent();

//        Make a big cluster combination
        Cluster[] LHS = new Cluster[]{C1};
        Cluster[] RHS = new Cluster[]{C2,C3};
        ClusterCombination CC = new ClusterCombination(LHS, RHS, 0, C1.size() + C2.size() + C3.size(), runParameters.isAllowVectorOverlap());

//        Bound it with postponing
        runParameters.setShrinkFactor(0);
        CC.bound(runParameters);

        //    Test postponed DCCs
        Assert.assertEquals(1, runParameters.getPostponedDCCs().size());

        for (ClusterCombination postponedDCC: runParameters.getPostponedDCCs()) {
            Assert.assertTrue(postponedDCC.isDecisive());
            Assert.assertNotEquals(postponedDCC.bounds.getUB(), postponedDCC.getShrunkUB(0, runParameters.getBFSFactor()), 0.001);
        }
        ClusterCombination postponedDCC = runParameters.getPostponedDCCs().peek();

//        Now process postponed DCCs
        runParameters.setShrinkFactor(1);
        postponedDCC.bound(runParameters);

        //    Test actual results
        Assert.assertEquals(1, runParameters.getResultSet().size());

        //    Test nCCs
        Assert.assertEquals(27, runParameters.getStatBag().getNCCs().get());

        //    Test nLookups
        Assert.assertEquals(39, runParameters.getStatBag().getNLookups().get());
    }
}
