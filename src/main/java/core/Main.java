package core;

import _aux.lib;
import algorithms.Algorithm;
import algorithms.baselines.SimpleBaseline;
import algorithms.baselines.SmartBaseline;
import algorithms.performance.CorrelationDetective;
import data_io.DataHandler;
import queries.ResultSet;
import similarities.SimEnum;

import javax.xml.crypto.Data;
import java.util.*;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
//        Run default query
        if (args.length == 0){
            args = new String[]{
                    "s3://correlation-detective/example_data.csv",
                    "s3://correlation-detective",
                    "pearson_correlation",
                    "1",
                    "2",
            };
        }

//        Set minio environment variables
        System.setProperty("MINIO_ENDPOINT_URL", "http://localhost:9000");
        System.setProperty("MINIO_ACCESS_KEY", "minioadmin");
        System.setProperty("MINIO_SECRET_KEY", "minioadmin");

//        Read parameters from args
        RunParameters runParameters = parseArgs(args);

//        Run the query
        run(runParameters);
    }

    public static RunParameters parseArgs(String[] args){
        // Parse necessary arguments (and delete them from the list)
        RunParameters runParameters = parseRequired(args);

        if (args.length == 5){
            return runParameters;
        }

        // Iterate through the optional command-line arguments
        for (int i=5; i<args.length; i++) {
            try {
                String arg = args[i];
                if (arg.startsWith("--")) {
                    parseNamedArg(runParameters, arg);
                } else if (arg.startsWith("-")) {
//                If second argument is not present, make sure that the first argument is a boolean
                    if (i + 1 < args.length) {
                        if (args[i + 1].startsWith("-")) {
                            parseAbbreviatedArg(runParameters, arg, "true");
                        } else {
                            parseAbbreviatedArg(runParameters, arg, args[i + 1]);
                            i++;
                        }
                    } else {
                        parseAbbreviatedArg(runParameters, arg, "true");
                    }
                } else {
                    throw new InputMismatchException("Invalid format for option: " + arg);
                }
            } catch (NoSuchFieldException e){
                throw new InputMismatchException("Invalid option: " + e.getMessage());
            } catch (IllegalAccessException e){
                throw new InputMismatchException("Invalid value for option: " + e.getMessage());
            }
        }

        return runParameters;
    }

    private static RunParameters parseRequired(String[] args){
        String inputInstructions = ", mind that the first 5 arguments should be <inputPath> <outputPath> <simMetricName> <maxPLeft> <maxPRight>";

        // Parse necessary arguments (first 5 non-named or abbreviated arguments)
        if (args.length < 5){
            throw new InputMismatchException("Not enough arguments" + inputInstructions);
        }

//        First extract the output path (and delete it from the list)
        String outputPath = args[1];
        args = lib.remove(args, 1);

//        Check if output path is valid
        if (!lib.isValidPath(outputPath)){
            throw new InputMismatchException("Invalid output path: " + outputPath);
        }

        int i = 0;
        try {
            String inputPath = args[i++];
            SimEnum simMetricName = SimEnum.valueOf(args[i++].toUpperCase());
            int maxPLeft = Integer.parseInt(args[i++]);
            int maxPRight = Integer.parseInt(args[i++]);

            RunParameters rp = new RunParameters(inputPath, simMetricName, maxPLeft, maxPRight);
            rp.setOutputPath(outputPath);

            return rp;
        } catch (NumberFormatException e){
            throw new InputMismatchException("Invalid format for option: " + args[i] + inputInstructions);
        }
    }

    private static void parseAbbreviatedArg(RunParameters runParameters, String arg1, String arg2) throws NoSuchFieldException, IllegalAccessException{
//        Check if the argument is in the form -key value
        String key = arg1.substring(1);
        Object value = lib.parseString(arg2);
        runParameters.set(key, value);
    }

    private static void parseNamedArg(RunParameters runParameters, String arg) throws NoSuchFieldException, IllegalAccessException{
        // Check if the argument is in the form --key=value
        int equalsIndex = arg.indexOf('=');
        if (equalsIndex != -1) {
            String key = arg.substring(2, equalsIndex);
            Object value = lib.parseString(arg.substring(equalsIndex + 1));
            runParameters.set(key, value);
        } else {
            throw new InputMismatchException("Invalid format for option: " + arg);
        }
    }

    private static void run(RunParameters runParameters) {


        Algorithm algorithm;
        switch (runParameters.getAlgorithm()){
            case SIMILARITY_DETECTIVE: default: algorithm = new CorrelationDetective(runParameters); break;
            case SIMPLE_BASELINE: algorithm = new SimpleBaseline(runParameters); break;
            case SMART_BASELINE: algorithm = new SmartBaseline(runParameters); break;
        }

//        Run the algorithm
        algorithm.run();

//        Save the results and stats
        saveResults(runParameters, runParameters.getOutputPath());
    }

    private static void saveResults(RunParameters runParameters, String outputPath){
//        Add a random run identifier to the outputPath
        outputPath += "/" + UUID.randomUUID().toString();
        Logger.getGlobal().info("Saving results and stats to " + outputPath);

        DataHandler outputHandler = runParameters.getOutputHandler();
        StatBag statBag = runParameters.getStatBag();
        ResultSet resultSet = runParameters.getResultSet();

//        Save the statBag as a json file
        outputHandler.writeToFile(outputPath + "/stats.json", statBag.toJson());

//        Save the results as a json file
        outputHandler.writeToFile(outputPath + "/results.json", resultSet.toJson());
    }
}