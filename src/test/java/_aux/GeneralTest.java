package _aux;

import algorithms.AlgorithmEnum;
import core.RunParameters;
import org.junit.Before;
import queries.QueryTypeEnum;
import similarities.SimEnum;

import java.util.logging.Level;

public abstract class GeneralTest {
    public RunParameters runParameters;

    @Before
    public void setUp() {
        String inputPath = "/home/jens/tue/data/stock/1620daily/stocks_1620daily_logreturn_deduped.csv";
        SimEnum simMetricName = SimEnum.PEARSON_CORRELATION;
        int maxPLeft = 1;
        int maxPRight = 2;

        runParameters = new RunParameters(inputPath, simMetricName, maxPLeft, maxPRight);

        runParameters.setLogLevel(Level.SEVERE);
        runParameters.setNVectors(100);
        runParameters.setNDimensions(1307);
        runParameters.setPartition(0);

        runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
        runParameters.setTau(.6);
        runParameters.setMinJump(0);
        runParameters.setIrreducibility(false);
        runParameters.setAllowVectorOverlap(false);

        runParameters.setShrinkFactor(1);
        runParameters.setTopK(0);
        runParameters.setBFSRatio(0.75);
        runParameters.setKMeans(10);
        runParameters.setParallel(false);
        runParameters.setRandom(false);

        runParameters.init();

//        Compute pairwise distances
        runParameters.computePairwiseDistances();
    }
}
