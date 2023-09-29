package data_reading;

import _aux.GeneralTest;
import _aux.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DataReaderTest {


     private void testDataSet(String dataType, int n, double[] targetVector0, double[] targetVector1, String targetHeader){
        int m = targetVector0.length;
        String inputPath = "/home/jens/tue/data";

        //      ----------------------------------------- TESTS ---------------------------------------------

//        Test 1: Test if dataReader reads the right file and skips based on variance as expected
        Pair<String[], double[][]> dataPair = DataReader.getData(dataType, inputPath, n, m, 0);
        Assert.assertEquals(targetHeader, dataPair.x[0]);
        Assert.assertArrayEquals(targetVector0, dataPair.y[0], 1e-3);
//        Test 2: Test if dataReader reads right partition
        Pair<String[], double[][]> data = DataReader.getData(dataType, inputPath, n, m, 1);
        Assert.assertEquals(targetHeader, data.x[0]);
        Assert.assertArrayEquals(targetVector1, data.y[0], 1e-3);
    }

    @Test
    public void testStockData(){
        double[] targetVector0 = new double[]{1550.0, 1560.0, 1565.0, 1600.0, 1580.0, 1560.0, 1560.0, 1550.0, 1550.0, 1545.0};
        double[] targetVector1 = new double[]{1565.0, 1515.0, 1505.0, 1505.0, 1510.0, 1510.0, 1510.0, 1510.0, 1515.0, 1515.0};
        String targetHeader = "8836.Asien--Australien--Südamerika--Afrika-Japan-Hokuetsu-Metal-Co.-Ltd._049497";

        testDataSet("stock",10,  targetVector0, targetVector1, targetHeader);
    }

    @Test
    public void testFMRIData(){
        double[] targetVector0 = new double[]{0.17334552,-0.14838597,-0.07023076,0.057886492,-0.040152334,0.08532504,0.054167412,-0.09764337,-0.1188039,-0.16708693
        };
        double[] targetVector1 = new double[]{0.012948887,0.013477271,-0.021646202,0.0037141498,-0.15840244,-0.15338603,0.256802,0.1452943,-0.0023118462,-0.26286328
        };
        String targetHeader = "0_3_2";

        testDataSet("fmri", 0, targetVector0, targetVector1, targetHeader);

    }

    @Test
    public void testSLPData(){
        double[] targetVector0 = new double[]{10115.0, 10273.0, 10324.0, 10196.0, 10216.0, 10140.0, 10155.0, 10155.0, 10124.0, 10110.0};
        double[] targetVector1 = new double[]{10110.0, 10151.0, 10171.0, 10171.0, 10144.0, 10192.0, 10156.0, 10068.0, 10050.0, 10076.0};
        String targetHeader = "1026099999";

        testDataSet("weather_slp", 10, targetVector0, targetVector1, targetHeader);
    }

    @Test
    public void testTMPData(){
        double[] targetVector0 = new double[]{-32.0, -42.0, -55.0, -99.0, -134.0, -151.0, -162.0, -170.0, -65.0, -47.0};
        double[] targetVector1 = new double[]{-79.0, -139.0, -158.0, -142.0, -113.0, -36.0, -39.0, -140.0, -165.0, -129.0};
        String targetHeader = "1026099999";

        testDataSet("weather_tmp", 10, targetVector0, targetVector1, targetHeader);
    }

    @Test
    public void testRandomData(){
        double[] targetVector0 = new double[]{0.335, 0.2, 0.435, 0.53, 0.775, 0.278, 0.954, 0.451, 0.506, 0.067};
        double[] targetVector1 = new double[]{0.627, 0.836, 0.894, 0.523, 0.353, 0.254, 0.024, 0.818, 0.362, 0.213};
        String targetHeader = "0";

        testDataSet("random", 10, targetVector0, targetVector1, targetHeader);
    }

    @Test
    public void testStockLogData(){
        double[] targetVector0 = new double[]{0.0064, 0.0032, 0.0221, -0.0126, -0.0127, 0.0, -0.0064, 0.0, -0.0032, 0.0129};
        double[] targetVector1 = new double[]{-0.0325, -0.0066, 0.0, 0.0033, 0.0, 0.0, 0.0, 0.0033, 0.0, 0.0293};
        String targetHeader = "8836.Asien--Australien--Südamerika--Afrika-Japan-Hokuetsu-Metal-Co.-Ltd._049497";

        testDataSet("stock_log", 10, targetVector0, targetVector1, targetHeader);
    }
}
