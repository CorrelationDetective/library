package _aux;

import _aux.lists.FastArrayList;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class LibTest {
    private static double[] v1 = new double[]{0.335, 0.2, 0.435, 0.53, 0.775, 0.278, 0.954, 0.451, 0.506, 0.067};
    private static double[] v2 = new double[]{0.347, 0.333, 0.79, 0.886, 0.96, 0.185, 0.094, 0.478, 0.868, 0.998};
    private static double[] v3 = new double[]{0.848, 0.205, 0.163, 0.024, 0.911, 0.925, 0.887, 0.256, 0.843, 0.204};
    private static double[][] M = new double[][]{v2, v3};

    @Test
    public void testDot(){
        double targetDot = 2.602833;

        Assert.assertEquals(targetDot, lib.dot(v1, v2), 1e-6);
    }

    @Test
    public void testAvg(){
        double targetAvg = 0.45309999999999995;
        Assert.assertEquals(targetAvg, lib.avg(v1), 1e-6);
    }

    @Test
    public void testVar(){
        double targetVar = 0.06213049000000004;
        Assert.assertEquals(targetVar, lib.var(v1), 1e-6);
    }

    @Test
    public void testStd(){
        double targetStd = .24925988445796896;
        Assert.assertEquals(targetStd, lib.std(v1), 1e-6);
    }

    @Test
    public void testMin(){
        double targetMin = 0.067;
        Assert.assertEquals(targetMin, lib.min(v1), 1e-6);
    }

    @Test
    public void testMax(){
        double targetMax = 0.954;
        Assert.assertEquals(targetMax, lib.max(v1), 1e-6);
    }

    @Test
    public void testScale(){
        double[] targetScale = new double[]{0.1675, 0.1, 0.2175, 0.265, 0.3875, 0.139, 0.477, 0.2255, 0.253, 0.0335};
        Assert.assertArrayEquals(targetScale, lib.scale(v1, .5), 1e-6);
    }

    @Test
    public void testElementwiseAvg(){
        double[] targetAvg = new double[]{0.34099999999999997, 0.2665, 0.6125, 0.708, 0.8674999999999999, 0.2315, 0.524, 0.4645, 0.687, 0.5325};
        Assert.assertArrayEquals(targetAvg, lib.elementwiseAvg(new double[][]{v1,v2}), 1e-6);
    }

    @Test
    public void testAdd(){
        double[] targetAdd = new double[]{0.6819999999999999, 0.533, 1.225, 1.416, 1.7349999999999999, 0.463, 1.048, 0.929, 1.374, 1.065};
        Assert.assertArrayEquals(targetAdd, lib.add(v1,v2), 1e-6);
    }

    @Test
    public void testMmul(){
        double[] targetMmul = new double[]{2.602833, 2.7737600000000002};
        Assert.assertArrayEquals(targetMmul, lib.mmul(v1, M), 1e-6);
    }

    @Test
    public void testl2(){
        double targetl2 = 1.6353290188827445;
        Assert.assertEquals(targetl2, lib.l2(v1), 1e-6);
    }

    @Test
    public void testZnorm(){
        Assert.assertEquals(1, lib.std(lib.znorm(v1)), 1e-6);
        Assert.assertEquals(0, lib.avg(lib.znorm(v1)), 1e-6);
    }

    @Test
    public void testl2norm(){
        Assert.assertEquals(1, lib.l2(lib.l2norm(v1)), 1e-6);
        Assert.assertEquals(0, lib.avg(lib.l2norm(v1)), 1e-6);
    }
}
