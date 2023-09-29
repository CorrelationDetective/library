package similarities;

import _aux.GeneralTest;
import bounding.ClusterCombination;
import clustering.Cluster;
import core.RunParameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import similarities.functions.TotalCorrelation;

public class TotalCorrelationTest extends GeneralTest {

    @Before
    public void setUp() {
        super.setUp();
        runParameters.setSimMetric(new TotalCorrelation(runParameters));
        runParameters.init();
    }

    @Test
    public void testSingletonBounds(){
        Cluster C1 = new Cluster(0, runParameters);
        C1.finalize();
        C1.setId(0);

        Cluster C2 = new Cluster(1, runParameters);
        C2.finalize();
        C2.setId(1);

        Cluster C3 = new Cluster(2, runParameters);
        C3.finalize();
        C3.setId(2);

        ClusterCombination CC = new ClusterCombination(new Cluster[]{C1,C2,C3}, new Cluster[0], 0, 3, runParameters.isAllowVectorOverlap());
        runParameters.getSimMetric().bound(CC);

//        Get actual tc
        double[][] M = new double[3][runParameters.getNDimensions()];
        double TC = 0;
        for (int i = 0; i < 3; i++) {
            double[] x = runParameters.getData()[i];
            M[i] = x;
            TC += TotalCorrelation.entropy(x);
        }

        TC -= TotalCorrelation.jointEntropy(M);

        Assert.assertEquals(TC, CC.getBounds().getLB(), 1e-6);
    }
}
