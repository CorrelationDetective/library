package bounding;

import _aux.GeneralTest;
import _aux.Pair;
import _aux.lists.FastArrayList;
import algorithms.performance.CorrelationDetective;
import clustering.Cluster;
import clustering.HierarchicalClustering;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import queries.ResultTuple;
import similarities.SimEnum;
import similarities.functions.ManhattanSimilarity;

import java.util.List;
import java.util.logging.Level;

public class BoundDiscountingTest extends GeneralTest {

    /**
     * Testing plan:
     * - RecursiveBoundingTask
     *      - checkDiscount
     *          1. CC3.isDiscounted() = true
     *          2. CC3.getLHS().length + CC3.getRHS().length <= 2
     *          3. CC3.getCriticalShrinkFactor() <= discountThreshold
     *          4. discounting = false
     *          5. CC3.isDiscounted() = false
     *          6. CC3.getLHS().length + CC3.getRHS().length > 2
     *          7. CC3.getCriticalShrinkFactor() > discountThreshold
     *          8. discounting = true
     * - runParameters.getSimMetric().bound
     *      - Correct empiricalBoundFactors set for empirical metric
     *      - No empiricalBoundFactors set for non-empirical metric
     *      - EmpiricalSimilarity from empiricalBoundFactors
     *          - For all empirical metrics:
     *              - Sim same as UB
     *              - Decreases when rank increased, increases when rank decreased
     * - BoundDiscounting
     *      - DiscountBounds
     *          1. Error thrown when no empiricalBoundFactors set
     *          2. Does not discount when all factors are at max rank
     *          3. Answer checks
     *              - Discounts when possible
     *              - Discounts the expected factors
     *              - Discounted set to true
     *              - Does not discount when not possible
     *              - Correct answers in result set
     *         4. Full answer set check
     *              - Check if same answer as without discounting
     *     - OptimalDiscountFactor
     *         1. Correct factor chosen
     *         2. Correct similarity returned
     *         3. No ranks incremented
     *     - CheckCutCombs
     *         1. Correct nCCs checked
     *         2. Correct answers in result Set
     *         3. Correct nDiscountCuts
     */

    private ClusterCombination CC11; // random CC3 with 2 clusters
    private ClusterCombination CC12; // CC3 with 3 clusters, good for discounting, new results after
    private ClusterCombination CC3; // CC3 with 3 clusters, good for discounting, new results after
    private ClusterCombination CCNoDiscount; // CC of 3 which is impossible to discount (fully positive)

    private SimEnum[] discountMetrics = new SimEnum[]{SimEnum.EUCLIDEAN_SIMILARITY, SimEnum.PEARSON_CORRELATION, SimEnum.MULTIPOLE, SimEnum.TOTAL_CORRELATION};
    private BoundDiscounting BD;

    @Before
    public void setUp(){
        super.setUp();
        runParameters.setDiscounting(true);
        runParameters.setDiscountThreshold(0.8);
        runParameters.setDiscountTopK(10);

        HierarchicalClustering HC = runParameters.initializeHC();
        HC.run();

        BD = runParameters.initializeBD();

        CC11 = new ClusterCombination(new Cluster[]{HC.singletonClusters[0]}, new Cluster[]{HC.singletonClusters[1]}, 0, 1, runParameters.isAllowVectorOverlap());

        Cluster c1 = HC.getCluster(56);
        Cluster c2 = HC.getCluster(56);
        Cluster c3 = HC.getCluster(13);
        CC12 = new ClusterCombination(new Cluster[]{c1}, new Cluster[]{c2, c3}, 0, 1, runParameters.isAllowVectorOverlap());
        CC3 = new ClusterCombination(new Cluster[]{c1,c2,c3}, new Cluster[]{}, 0, 1, runParameters.isAllowVectorOverlap());
    }


    @Test
    public void testCheckDiscounting(){
//        0. Non discounting metric
        SimEnum[] nonDiscountingMetrics = new SimEnum[]{SimEnum.MANHATTAN_SIMILARITY};
        CC12.criticalShrinkFactor = runParameters.getDiscountThreshold() + .1;
        for (SimEnum simEnum : nonDiscountingMetrics) {
            runParameters.setSimMetricName(simEnum);
            runParameters.init();
            Assert.assertFalse(BD.checkDiscounting(CC12));
        }

        runParameters.setEmpiricalBounding(true);
        runParameters.setSimMetricName(SimEnum.PEARSON_CORRELATION);
        runParameters.init();

        // 1. CC3.isDiscounted() = true
        CC12.setDiscounted(true);
        Assert.assertFalse(BD.checkDiscounting(CC12));

        // 2. CC3.getLHS().length + CC3.getRHS().length <= 2
        Assert.assertFalse(BD.checkDiscounting(CC11));

        // 3. CC3.getCriticalShrinkFactor() <= discountThreshold
        CC12.criticalShrinkFactor = runParameters.getDiscountThreshold() - .1;
        Assert.assertFalse(BD.checkDiscounting(CC12));

        // 4. discounting = false
        runParameters.setDiscounting(false);
        Assert.assertFalse(BD.checkDiscounting(CC12));

        // 5. all true
//        Bound to set bound factors
        CC12.criticalShrinkFactor = runParameters.getDiscountThreshold() + .1;
        runParameters.setDiscounting(true);

        CC12.setDiscounted(false);
        runParameters.getSimMetric().bound(CC12);
        Assert.assertTrue(BD.checkDiscounting(CC12));
    }

    @Test
    public void testEmpiricalBoundFactors(){
        Cluster[] clusters = CC12.getClusters();

        runParameters.getSimMetric().bound(CC12);

//        Correct count
        Assert.assertEquals(3, CC12.getEmpiricalBoundFactors().size());

//        First factor is negative impace with the min extreme between c1 and c2
        EmpiricalBoundFactor ebf = CC12.getEmpiricalBoundFactors().get(0);
        Assert.assertFalse(ebf.isPositiveImpact()); // negative impact
        ClusterPair cp = ebf.getClusterPair();
        Assert.assertEquals(clusters[0], cp.getLeft()); // c1 is left
        Assert.assertEquals(clusters[1], cp.getRight()); // c2 is right
        Assert.assertEquals(cp.getMinDistances()[0], ebf.getExtremaPair()); // min extreme between c1 and c2

//        Second factor is negative impact with the min extreme between c1 and c3
        ebf = CC12.getEmpiricalBoundFactors().get(1);
        Assert.assertFalse(ebf.isPositiveImpact()); // negative impact
        cp = ebf.getClusterPair();
        Assert.assertEquals(clusters[0], cp.getLeft()); // c1 is left
        Assert.assertEquals(clusters[2], cp.getRight()); // c3 is right
        Assert.assertEquals(cp.getMinDistances()[0], ebf.getExtremaPair()); // min extreme between c1 and c3

//        Third factor is positive impact with the max extreme between c2 and c3
        ebf = CC12.getEmpiricalBoundFactors().get(2);
        Assert.assertTrue(ebf.isPositiveImpact()); // positive impact
        cp = ebf.getClusterPair();
        Assert.assertEquals(clusters[1], cp.getLeft()); // c2 is left
        Assert.assertEquals(clusters[2], cp.getRight()); // c3 is right
        Assert.assertEquals(cp.getMaxDistances()[0], ebf.getExtremaPair()); // max extreme between c2 and c3

//        No empirical bound factors for non-empirical metric
        CC12.setEmpiricalBoundFactors(null);
        runParameters.setSimMetric(new ManhattanSimilarity(runParameters));
        runParameters.init();
        runParameters.getSimMetric().bound(CC12);
        Assert.assertNull(CC12.getEmpiricalBoundFactors());

//        For all empirical metrics:
        for (SimEnum metricName : discountMetrics){
            runParameters.setEmpiricalBounding(true);
            runParameters.setSimMetricName(metricName);
            runParameters.init();

//            Compute distances cache
            runParameters.computePairwiseDistances();

//            Prepare CC
            ClusterCombination testCC = runParameters.getSimMetric().isTwoSided() ? CC12: CC3;
            int pL = testCC.getLHS().length;
            int pR = testCC.getRHS().length;

//            Clear bounds
            testCC.clearBounds();

//            Sim same as UB
            double[][] pairwiseDistances = runParameters.getPairwiseDistances();
            runParameters.getSimMetric().bound(testCC);
            double UB = testCC.getUB();
            FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors = testCC.getEmpiricalBoundFactors();
            double estUB = runParameters.getSimMetric().empiricalSimilarity(empiricalBoundFactors, pL, pR, pairwiseDistances);
            Assert.assertEquals("UB and estUB not the same for " + metricName,
                    UB, estUB, 0.0001);

//            Decreases when rank increased
            empiricalBoundFactors.get(0).incrementRank();
            empiricalBoundFactors.get(1).incrementRank();
            double newUB = runParameters.getSimMetric().empiricalSimilarity(empiricalBoundFactors, pL, pR, pairwiseDistances);
            Assert.assertTrue("UB not decreased when rank increased for " + metricName,
                    newUB < UB);

//            Increases when rank decreased
            empiricalBoundFactors.get(0).decrementRank();
            double oldUB = newUB;
            newUB = runParameters.getSimMetric().empiricalSimilarity(empiricalBoundFactors, pL, pR, pairwiseDistances);
            Assert.assertTrue("UB not increased when rank decreased for " + metricName,
                    newUB > oldUB);
        }
    }

    @Test
    public void testTCEmpiricalBoundFactors(){
        runParameters.setSimMetricName(SimEnum.TOTAL_CORRELATION);
        runParameters.setEmpiricalBounding(true);
        runParameters.init();
        runParameters.computePairwiseDistances();

        Cluster[] clusters = CC3.getClusters();

        runParameters.getSimMetric().bound(CC3);

//        Correct count
        Assert.assertEquals(4, CC3.getEmpiricalBoundFactors().size());

        int i = 0;

//        First factor is positive impace with the max extreme of c1
        EmpiricalBoundFactor ebf = CC3.getEmpiricalBoundFactors().get(i++);
        Assert.assertTrue(ebf.isPositiveImpact()); // negative impact
        ClusterPair cp = ebf.getClusterPair();
        Assert.assertEquals(1, cp.getLHS().length + cp.getRHS().length);
        Assert.assertEquals(1, cp.getLHS().length);
        Assert.assertEquals(clusters[0], cp.getLeft()); // c1 is left
        Assert.assertEquals(cp.getMaxDistances()[0], ebf.getExtremaPair()); // min extreme between c1 and c2

//        Second factor is positive impact with the max extreme of c2
        ebf = CC3.getEmpiricalBoundFactors().get(i++);
        Assert.assertTrue(ebf.isPositiveImpact()); // negative impact
        cp = ebf.getClusterPair();
        Assert.assertEquals(1, cp.getLHS().length + cp.getRHS().length);
        Assert.assertEquals(1, cp.getLHS().length);
        Assert.assertEquals(clusters[1], cp.getLeft()); // c1 is left
        Assert.assertEquals(cp.getMaxDistances()[0], ebf.getExtremaPair()); // min extreme between c1 and c2

//        Third factor is positive impact with the max extreme of c2
        ebf = CC3.getEmpiricalBoundFactors().get(i++);
        Assert.assertTrue(ebf.isPositiveImpact()); // negative impact
        cp = ebf.getClusterPair();
        Assert.assertEquals(1, cp.getLHS().length + cp.getRHS().length);
        Assert.assertEquals(1, cp.getLHS().length);
        Assert.assertEquals(clusters[2], cp.getLeft()); // c1 is left
        Assert.assertEquals(cp.getMaxDistances()[0], ebf.getExtremaPair()); // min extreme between c1 and c2

//        Fourth factor is negative impact with the min extreme between c1 and c3
        ebf = CC3.getEmpiricalBoundFactors().get(i++);
        Assert.assertFalse(ebf.isPositiveImpact()); // negative impact
        cp = ebf.getClusterPair();
        Assert.assertEquals(2, cp.getLHS().length + cp.getRHS().length);
        Assert.assertEquals(1, cp.getLHS().length);
        Assert.assertEquals(1, cp.getRHS().length);
        Assert.assertEquals(clusters[0], cp.getLeft()); // c1 is left
        Assert.assertEquals(clusters[2], cp.getRight()); // c3 is right
        Assert.assertEquals(cp.getMinDistances()[0], ebf.getExtremaPair()); // min extreme between c1 and c2
    }

    @Test
    public void testDiscountBounds() {
        double[] taus = new double[]{0.4, 0.6, 0.6, 1.5};
        int[][] expectedRanks = new int[][]{
                {2, 5, 4},
                {1, 0, 0},
                {2, 0, 1, 0, 0, 0},
                {1, 0, 0, 1}
        };

        for (int i=0; i<discountMetrics.length; i++) {
            SimEnum metricName = discountMetrics[i];
            runParameters.setTau(taus[i]);
            runParameters.setEmpiricalBounding(true);
            runParameters.setSimMetricName(metricName);
            runParameters.init();

//            Compute distances cache
            runParameters.computePairwiseDistances();

//            Prepare CC
            ClusterCombination testCC = runParameters.getSimMetric().isTwoSided() ? CC12 : CC3;

//            Clear bounds
            testCC.clearBounds();

//        0. Error when not bounded
            Exception exception = Assert.assertThrows(RuntimeException.class, () -> BD.discountBounds(testCC));
            Assert.assertEquals("Cannot discount bounds of unbounded CC " + testCC, exception.getMessage());

//        1. No discounting when positive
            runParameters.getSimMetric().bound(testCC);
            testCC.setPositive(true);
            Assert.assertFalse(BD.discountBounds(testCC));
            testCC.setPositive(false);

            // 2. Error when no empirical bound factors set
            testCC.setEmpiricalBoundFactors(null);
            exception = Assert.assertThrows(RuntimeException.class, () -> BD.discountBounds(testCC));
            Assert.assertEquals("No empirical bound factors for CC " + testCC, exception.getMessage());

//        ----- Answer checks -----
//        3. Discount when possible
            testCC.clearBounds();
            runParameters.getSimMetric().bound(testCC);
            Assert.assertTrue(BD.discountBounds(testCC));
            Assert.assertTrue(testCC.isDiscounted());

//        4. Discount expected factors
            for (int j = 0; j < testCC.getEmpiricalBoundFactors().size(); j++) {
                EmpiricalBoundFactor ebf = testCC.getEmpiricalBoundFactors().get(j);
                Assert.assertEquals(expectedRanks[i][j], ebf.getRank());
            }
        }
    }

    private void testAnswers(){
        //        Get answers when not discounting
        runParameters.setDiscounting(false);
        CorrelationDetective sd = new CorrelationDetective(runParameters);
        sd.run();
        List<ResultTuple> groundTruth = runParameters.getResultSet().getResultTuples();

//        Get answers with discounting
        runParameters.setDiscounting(true);
        runParameters.init();
        sd = new CorrelationDetective(runParameters);
        sd.run();

//        Compare the two
        double[] precisionRecall = runParameters.getResultSet().computePrecisionRecall(groundTruth);
        double precision = precisionRecall[0];
        double recall = precisionRecall[1];
        Assert.assertEquals("Precision not the same with and without discounting", 1.0, precision, 0.0001);
        Assert.assertEquals("Recall not the same with and without discounting", 1.0, recall, 0.0001);
    }

    @Test
    public void testBoundDiscountingAnswers() {
        runParameters.setLogLevel(Level.FINER);
        testAnswers();
    }

    @Test
    public void testBoundDiscountingStep(){
        double[] taus = new double[]{0.4, 0.6, 0.6, 1.5};
        int[][] expectedRanks = new int[][]{
                {8, 2, 4},
                {2, 0, 0},
                {4, 0, 0, 0, 0, 0},
                {0, 0, 0, 2}
        };

        runParameters.setDiscountStep(2);
        runParameters.setDiscountTopK(20);

        for (int i=0; i<discountMetrics.length; i++) {
            SimEnum metricName = discountMetrics[i];
            runParameters.setTau(taus[i]);
            runParameters.setEmpiricalBounding(true);
            runParameters.setSimMetricName(metricName);
            runParameters.init();

//            Compute distances cache
            runParameters.computePairwiseDistances();

//            ---------- Discounting checks -----
//            Prepare CC
            ClusterCombination testCC = runParameters.getSimMetric().isTwoSided() ? CC12 : CC3;

//            Clear bounds
            testCC.clearBounds();

//        3. Discount when possible
            testCC.clearBounds();
            runParameters.getSimMetric().bound(testCC);
            Assert.assertTrue(BD.discountBounds(testCC));
            Assert.assertTrue(testCC.isDiscounted());

//        4. Discount expected factors
            for (int j = 0; j < testCC.getEmpiricalBoundFactors().size(); j++) {
                EmpiricalBoundFactor ebf = testCC.getEmpiricalBoundFactors().get(j);
                Assert.assertEquals(expectedRanks[i][j], ebf.getRank());
            }
        }
    }

    @Test
    public void testBoundDiscountingStepAnswers(){
        double[] taus = new double[]{0.5, 0.6, 0.6};
        SimEnum[] metrics = new SimEnum[]{SimEnum.EUCLIDEAN_SIMILARITY, SimEnum.PEARSON_CORRELATION, SimEnum.MULTIPOLE};

        runParameters.setDiscountStep(2);
        runParameters.setDiscountTopK(20);

        for (int i=0; i<metrics.length; i++) {
            SimEnum metricName = metrics[i];
            runParameters.setTau(taus[i]);
            runParameters.setEmpiricalBounding(true);
            runParameters.setSimMetricName(metricName);
            runParameters.init();

//            Compute distances cache
            runParameters.computePairwiseDistances();

            testAnswers();
        }
    }

    @Test
    public void testOptimalBoundFactor() {
        runParameters.getSimMetric().bound(CC12);
        int pL = CC12.getLHS().length;
        int pR = CC12.getRHS().length;

        int[] expFactors = new int[]{0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0};

        FastArrayList<EmpiricalBoundFactor> boundFactors = CC12.getEmpiricalBoundFactors();
        double sim = runParameters.getSimMetric().empiricalSimilarity(boundFactors, pL, pR, runParameters.getPairwiseDistances());
        for (int j = 0; j < 20; j++) {
            Pair<EmpiricalBoundFactor, Double> res = BD.optimalDiscountFactor(boundFactors, sim, pL, pR);
            double oldSim = sim;
            sim = res.y;
            int nFactor = boundFactors.indexOf(res.x);

            //  1.     Check if correct sequence of optimal bound factors is returned
            Assert.assertEquals(expFactors[j], nFactor);

            //  2.          Check if sim decreased
            Assert.assertTrue(sim <= oldSim);

            //            Increment factor
            res.x.incrementRank();
        }
    }

    @Test
    public void testCutCombs(){
        runParameters.getSimMetric().bound(CC12);

//        Run cutCombs
        EmpiricalBoundFactor cutFactor = CC12.getEmpiricalBoundFactors().get(0);
        BD.checkCutCombs(cutFactor.getExtremaPair(), cutFactor.getLocations(), CC12);

//        1. Check correct answers in resultSet
        Assert.assertEquals(1, runParameters.getResultSet().size());
        ResultTuple rt = runParameters.getResultSet().getResultTuples().get(0);
        ResultTuple gt = new ResultTuple(new int[]{19}, new int[]{32,37}, 0.6154295295037898);
        Assert.assertEquals(gt, rt);

//        2. Check correct nCCs
        Assert.assertEquals(1, runParameters.getStatBag().getNCCs().get());

//       3. Check correct nDiscountCuts
        Assert.assertEquals(1, runParameters.getStatBag().getNDiscountCuts().get());
    }
}
