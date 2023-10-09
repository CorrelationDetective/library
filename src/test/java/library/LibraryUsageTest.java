package library;

import algorithms.performance.CorrelationDetective;
import core.RunParameters;
import core.StatBag;
import org.junit.Test;
import queries.QueryTypeEnum;
import queries.ResultSet;
import queries.ResultTuple;
import similarities.SimEnum;

import java.util.List;

public class LibraryUsageTest {
    /**
     * Goal: Show how one can run the similarity detective algorithm as a library (i.e., configure a query and run it).
     * Steps:
     * 1. Create a CorrelationDetective object directly, and set specific parameters via the sd.runParameters object.
     * 2. Create a RunParameters object, and set specific parameters via the RunParameters object, and pass it to the CorrelationDetective constructor.
     * 3. Run the query via the sd.run() method.
     * 4. Show how to interact with the returned ResultSet object.
     * 5. Show how to get the statBag object.
     * 6. Show how to save the output of both the ResultSet and the statBag as both a CSV and JSON file.
     */

    @Test
    public void testLibraryUsage() {
//        Define necessary parameters (see docs for more info)
        String inputPath = "/home/jens/tue/data/stock/1620daily/stocks_1620daily_logreturn_deduped.csv";
        String outputPath = "";
        SimEnum simMetricName = SimEnum.PEARSON_CORRELATION;
        int maxPLeft = 1;
        int maxPRight = 2;


//      1. How to initialize the CorrelationDetective object:

//      Option 1: Create CorrelationDetective object directly, set optional parameters via the sd.runParameters object
        CorrelationDetective sd = new CorrelationDetective(inputPath, simMetricName, maxPLeft, maxPRight);

//        Change the query to a threshold query, and make sure we run on a subset of 100 vectors
        sd.runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
        sd.runParameters.setTau(0.7);
        sd.runParameters.setNVectors(200);

//        Option 2: Create a RunParameters object, set optional parameters via the RunParameters object, and pass it to the CorrelationDetective constructor
        RunParameters runParameters = new RunParameters(inputPath, simMetricName, maxPLeft, maxPRight);
        runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
        runParameters.setTau(0.7);
        runParameters.setNVectors(200);

        sd = new CorrelationDetective(runParameters);

//      2. How to run the query; call the run() method, which returns a ResultSet object and will log the progress to the console
        ResultSet resultSet = sd.run();

//      3. How to interact with the returned ResultSet object:
        int numResults = resultSet.size();
        System.out.println("Number of results: " + numResults);

//        To get the results as tuples of indexes following the correlation pattern (i.e., (x, (y,z))), we need to call the getResultTuples() method
        List<ResultTuple> resultTuples = resultSet.getResultTuples();

//        Let's print the first 10 results
        System.out.println("First 10 results:");
        for (int i = 0; i < 10; i++) {
            ResultTuple resultTuple = resultTuples.get(i);
            System.out.println(resultTuple);
        }

//        4. How to get the statBag object (which contains all statistics of the run) and interact with it
        StatBag statBag = sd.getStatBag();

//        Let's print some statistics
        System.out.println("Total duration: " + statBag.getTotalDuration());
        System.out.println("Total number of cluster combinations inspected: " + statBag.getNCCs());
        statBag.printStageDurations();


//        5. How to save the output of both the ResultSet and the statBag as both a CSV and JSON file
        String outputDir = "/home/jens/tue/1.CorrelationDetective/CorrelationDetective/output";

//        Save results as csv
        resultSet.saveAsCSV(outputDir + "/results.csv");

//        Save results as json
        resultSet.saveAsJson(outputDir + "/results.json");

//        Save statBag as csv
        statBag.saveAsCSV(outputDir + "/stats.csv");

//        Save statBag as json
        statBag.saveAsJson(outputDir + "/stats.json");
    }
}
