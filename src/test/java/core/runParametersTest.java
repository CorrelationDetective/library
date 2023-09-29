package core;

import _aux.GeneralTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import queries.QueryTypeEnum;
import similarities.SimEnum;

import java.util.logging.Level;

public class runParametersTest extends GeneralTest {

//    Test that maxPLeft and maxPRight are set correctly
    @Test
    public void testCorrPatternChecks(){
       runParameters.setLogLevel(Level.FINER);
       runParameters.setSimMetricName(SimEnum.TOTAL_CORRELATION);
       runParameters.init();

       Assert.assertEquals(3,(int) runParameters.getMaxPLeft());
       Assert.assertEquals(0,(int) runParameters.getMaxPRight());

       runParameters.setSimMetricName(SimEnum.PEARSON_CORRELATION);
       runParameters.init();

       Assert.assertEquals(2,(int) runParameters.getMaxPLeft());
       Assert.assertEquals(1,(int) runParameters.getMaxPRight());

    }

//    Test that the query type is set correctly
    @Test
    public void testQueryTypeChecks(){
       runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
       runParameters.setTopK(1);
       runParameters.setShrinkFactor(0.5);
       runParameters.init();

       Assert.assertEquals(0, runParameters.getTopK());
       Assert.assertEquals(1, runParameters.getShrinkFactor(), 0.0001);

       
       runParameters.setQueryType(QueryTypeEnum.TOPK);
       runParameters.setTopK(0);
       runParameters.setMinJump(1);
       runParameters.setIrreducibility(true);
       runParameters.setTau(1);
       runParameters.init();

       Assert.assertEquals(100, runParameters.getTopK());
       Assert.assertEquals(0, runParameters.getMinJump(), 0.0001);
       Assert.assertFalse(runParameters.isIrreducibility());
       Assert.assertEquals(0, runParameters.getRunningThreshold().get(), 0.0001);
    }

//    Test that the query constraints are set correctly
    @Test
    public void testQueryConstraintsChecks(){
        runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
        runParameters.setIrreducibility(true);
        runParameters.setMinJump(.5);
        runParameters.init();

        Assert.assertTrue(runParameters.isIrreducibility());
        Assert.assertEquals(0, runParameters.getMinJump(), 0.0001);

//        Check that jump is not unset if irreducibility is false
        runParameters.setMinJump(.5);
        runParameters.setIrreducibility(false);
        runParameters.init();

        Assert.assertFalse(runParameters.isIrreducibility());
        Assert.assertEquals(.5, runParameters.getMinJump(), 0.0001);

//        Check that irreducibility is not unset if jump is 0
        runParameters.setMinJump(0);
        runParameters.setIrreducibility(true);
        runParameters.init();

        Assert.assertTrue(runParameters.isIrreducibility());
        Assert.assertEquals(0, runParameters.getMinJump(), 0.0001);
    }

//    Test that the similarity metric is set correctly
    @Test
    public void testSimMetricChecks(){
        runParameters.setSimMetricName(SimEnum.TOTAL_CORRELATION);
        runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
        runParameters.setIrreducibility(false);
        runParameters.setMinJump(0);
        runParameters.init();

        Assert.assertTrue(runParameters.isIrreducibility());
        Assert.assertEquals(0, runParameters.getMinJump(), 0.0001);

//        Check that minjump and irreducibility are unchanged if not both false
        runParameters.setMinJump(0);
        runParameters.setIrreducibility(true);
        runParameters.init();

        Assert.assertTrue(runParameters.isIrreducibility());
        Assert.assertEquals(0, runParameters.getMinJump(), 0.0001);

        runParameters.setMinJump(.5);
        runParameters.setIrreducibility(false);
        runParameters.init();

        Assert.assertFalse(runParameters.isIrreducibility());
        Assert.assertEquals(.5, runParameters.getMinJump(), 0.0001);
    }

//    Test that the aggPattern is checked correctly
    @Ignore
    @Test
    public void testAggPatternChecks(){
        runParameters.setMinJump(0.5);
        runParameters.init();

        Assert.assertEquals(0, runParameters.getMinJump(), 0.0001);

        runParameters.setMinJump(0.5);
        runParameters.init();

        Assert.assertEquals(0.5, runParameters.getMinJump(), 0.0001);
    }

//    Test that empiricalBounding parameter is checked correctly
    @Test
    public void testEmpiricalBoundingChecks(){
        //        Set empiricalBounding to true for theoretical metric and check that it is set to false
        runParameters.setSimMetricName(SimEnum.MANHATTAN_SIMILARITY);
        runParameters.setEmpiricalBounding(true);
        runParameters.init();

        Assert.assertFalse(runParameters.isEmpiricalBounding());

//        Check if empiricalBounding is not unset for empirical metric
        runParameters.setSimMetricName(SimEnum.PEARSON_CORRELATION);
        runParameters.setEmpiricalBounding(true);
        runParameters.init();

        Assert.assertTrue(runParameters.isEmpiricalBounding());
    }

}
