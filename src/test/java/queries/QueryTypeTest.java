package queries;

import _aux.GeneralTest;
import algorithms.performance.CorrelationDetective;
import bounding.ClusterCombination;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class QueryTypeTest extends GeneralTest {
    private CorrelationDetective sd;


    @Before
    public void setUp() {
        super.setUp();
        sd = new CorrelationDetective(runParameters);
    }

    @Test
    public void testThresholdQuery(){
        runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
        runParameters.setTau(0.6);
        runParameters.setMinJump(0);
        runParameters.setIrreducibility(false);
        runParameters.init();

        ResultSet resultSet = sd.run();
        List<ResultTuple> results = resultSet.close();

        Assert.assertEquals(38, results.size());

        for (ResultTuple result : results) {
            Assert.assertTrue(result.getSimilarity() >= runParameters.getTau());
        }
    }

    @Test
    public void testThresholdQueryMinjump(){
        runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
        runParameters.setTau(.5);
        runParameters.setMinJump(0.05);
        runParameters.setIrreducibility(false);
        runParameters.init();

        ResultSet resultSet = sd.run();
        List<ResultTuple> results = resultSet.close();

        Assert.assertEquals(255, results.size());

        for (ResultTuple result : results) {
            Assert.assertTrue(result.getSimilarity() >= runParameters.getTau());
        }

        for (ResultObject res : resultSet.getResultObjects()){
            ClusterCombination cc = (ClusterCombination) res;
            double sim = cc.getBounds().getLB();
            Assert.assertTrue(sim >= runParameters.getTau());

            if (cc.getLHS().length + cc.getRHS().length > 2){
                Assert.assertTrue(sim >= cc.getMaxSubsetSimilarity() + runParameters.getMinJump());
            }
        }
    }

    @Test
    public void testThresholdQueryIrreducibility(){
        runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
        runParameters.setTau(0.5);
        runParameters.setIrreducibility(true);
        runParameters.init();

        ResultSet resultSet = sd.run();
        List<ResultTuple> results = resultSet.close();

        Assert.assertEquals(328, results.size());

        for (ResultTuple result : results) {
            Assert.assertTrue(result.getSimilarity() >= runParameters.getTau());
        }
    }

    @Test
    public void testTopKQuery(){
        runParameters.setLogLevel(Level.FINER);
        runParameters.setQueryType(QueryTypeEnum.TOPK);
        runParameters.setMinJump(0);
        runParameters.setTopK(10);
        runParameters.setShrinkFactor(0);
        runParameters.init();

        ResultSet resultSet = sd.run();
        List<ResultTuple> results = resultSet.close();
        List<ResultTuple> sortedResults = results.stream().sorted(ResultTuple::compareTo).collect(Collectors.toList());

        Assert.assertEquals(10, results.size());
        Assert.assertEquals(runParameters.getRunningThreshold().get(), sortedResults.get(0).getSimilarity(), 0.0001);
    }

    @Test
    public void testProgressiveQuery(){
        runParameters.setLogLevel(Level.FINER);
        runParameters.setQueryType(QueryTypeEnum.PROGRESSIVE);
        runParameters.setTau(0.5);
        runParameters.setMinJump(0.05);
        runParameters.setTopK(10);
        runParameters.setShrinkFactor(0);
        runParameters.init();

        ResultSet resultSet = sd.run();
        List<ResultTuple> results = resultSet.close();

        Assert.assertEquals(10, results.size());

        for (ResultTuple result : results) {
            Assert.assertTrue(result.getSimilarity() >= runParameters.getTau());
        }

        for (ResultObject res : resultSet.getResultObjects()){
            ClusterCombination cc = (ClusterCombination) res;
            double sim = cc.getBounds().getLB();
            Assert.assertTrue(sim >= runParameters.getTau());
            if (cc.getLHS().length + cc.getRHS().length > 2){
                Assert.assertTrue(sim >= cc.getMaxSubsetSimilarity() + runParameters.getMinJump());
            }
        }
    }
}
