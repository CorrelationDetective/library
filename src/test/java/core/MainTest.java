package core;

import org.junit.Assert;
import org.junit.Test;

import java.util.InputMismatchException;

public class MainTest {
    /**
     * Tests:
     * 1. Missing required arguments
     * 2. Wrongly formatted abbreviated arguments
     * 3. Wrongly formatted named arguments
     * 4. Correct required abbreviated arguments
     * 4. Correct required named arguments
     * 5. Correct required arguments, plus extra arguments
     */

    String inputPath = "/home/jens/tue/data/stock/1620daily/stocks_1620daily_logreturn_deduped.csv";
    String simMetricName = "pearson_correlation";
    String maxPLeft = "1";
    String maxPRight = "2";
    String n = "100";


    @Test
    public void testMissingRequiredArguments(){
        String[] args = new String[]{
                inputPath,
                simMetricName,
                maxPLeft,
        };

//        Test if throws error with message
        Exception e = Assert.assertThrows(InputMismatchException.class, () -> Main.main(args));
        Assert.assertEquals("Not enough arguments, mind that the first 5 arguments should be <inputPath> <outputPath> <simMetricName> <maxPLeft> <maxPRight>",
                e.getMessage());
    }


    @Test
    public void testWronglyFormattedAbbreviatedArguments(){
        String[] args = new String[]{
                inputPath,
                simMetricName,
                maxPLeft,
                maxPRight,
                "-n",
                n,
        };

        //        Test if throws error with message
        Exception e = Assert.assertThrows(InputMismatchException.class, () -> Main.main(args));
        Assert.assertEquals("Invalid option: n", e.getMessage());
    }

    @Test
    public void testWronglyFormattedNamedArguments(){
        String[] args = new String[]{
                inputPath,
                simMetricName,
                maxPLeft,
                maxPRight,
                "--nVectors " + n,
        };

        //        Test if throws error with message
        Exception e = Assert.assertThrows(InputMismatchException.class, () -> Main.parseArgs(args));
        Assert.assertEquals("Invalid format for option: --nVectors " + n, e.getMessage());
    }

    @Test
    public void testCorrectRequiredAbbreviatedArguments(){
        String[] args = new String[]{
                inputPath,
                simMetricName,
                maxPLeft,
                maxPRight,
                "-nVectors",
                n,
        };

        RunParameters runParameters = Main.parseArgs(args);

        Assert.assertEquals(inputPath, runParameters.getInputPath());
        Assert.assertEquals(simMetricName.toUpperCase(), runParameters.getSimMetricName().toString());
        Assert.assertEquals(Integer.parseInt(maxPLeft), (int) runParameters.getMaxPLeft());
        Assert.assertEquals(Integer.parseInt(maxPRight), (int) runParameters.getMaxPRight());
        Assert.assertEquals(Integer.parseInt(n), (int) runParameters.getNVectors());
    }

    @Test
    public void testCorrectRequiredNamedArguments(){
        String[] args = new String[]{
                inputPath,
                simMetricName,
                maxPLeft,
                maxPRight,
                "--nVectors=" + n,
        };

        RunParameters runParameters = Main.parseArgs(args);

        Assert.assertEquals(inputPath, runParameters.getInputPath());
        Assert.assertEquals(simMetricName.toUpperCase(), runParameters.getSimMetricName().toString());
        Assert.assertEquals(Integer.parseInt(maxPLeft), (int) runParameters.getMaxPLeft());
        Assert.assertEquals(Integer.parseInt(maxPRight), (int) runParameters.getMaxPRight());
        Assert.assertEquals(Integer.parseInt(n), (int) runParameters.getNVectors());
    }

    @Test
    public void testCorrectRequiredArgumentsPlusExtraArguments(){
        String[] args = new String[]{
                inputPath,
                simMetricName,
                maxPLeft,
                maxPRight,
                "-irreducibility",
                "-nVectors", n,
        };

        RunParameters runParameters = Main.parseArgs(args);

        Assert.assertEquals(inputPath, runParameters.getInputPath());
        Assert.assertEquals(simMetricName.toUpperCase(), runParameters.getSimMetricName().toString());
        Assert.assertEquals(Integer.parseInt(maxPLeft), (int) runParameters.getMaxPLeft());
        Assert.assertEquals(Integer.parseInt(maxPRight), (int) runParameters.getMaxPRight());
        Assert.assertEquals(Integer.parseInt(n), (int) runParameters.getNVectors());
        Assert.assertTrue(runParameters.isIrreducibility());
    }

}
