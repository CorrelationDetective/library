package core;

import _aux.lib;
import algorithms.Algorithm;
import algorithms.baselines.SimpleBaseline;
import algorithms.baselines.SmartBaseline;
import algorithms.performance.CorrelationDetective;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import data_io.DataHandler;
import data_io.FileHandler;
import queries.ResultSet;
import similarities.SimEnum;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
//        Run default query
        if (args.length == 0){
            args = new String[]{
                    "/home/jens/ownCloud/Documents/3.Werk/0.TUe_Research/1.SimilarityDetective/X.GitHub/library/src/test/resources/input_dev.json",
                    "/home/jens/ownCloud/Documents/3.Werk/0.TUe_Research/1.SimilarityDetective/X.GitHub/library/src/test/resources/output.json",
            };
        }

//        Get input and output json path
        String inputJsonPath = args[0];
        String outputJsonPath = args[1];

//        Read parameters from args
        RunParameters runParameters = parseArgs(inputJsonPath);

//        Run the query
        run(runParameters);

//        Get output json
        JsonObject response = getResponse(runParameters);

//        Write the output json to the output path
        FileHandler fileHandler = new FileHandler();
        fileHandler.writeToFile(outputJsonPath, response.toString());

        System.out.println(response);

        System.exit(0);
    }

    public static RunParameters parseArgs(String inputJsonPath){
//        Read input json as Json object
        ObjectMapper objectMapper = new ObjectMapper();

        try {
//            First print the input json as a string
            System.out.println("Input json:" + lib.readFile(inputJsonPath));

            JsonNode jsonNode = objectMapper.readTree(new File(inputJsonPath));

//            First parse the required arguments
            String inputPath = jsonNode.get("input").get(0).asText();
            JsonNode parameters = jsonNode.get("parameters");

            String outputPath = parameters.get("outputPath").asText();
            SimEnum simMetricName = SimEnum.valueOf(parameters.get("simMetricName").asText().toUpperCase());
            int maxPLeft = parameters.get("maxPLeft").asInt();
            int maxPRight = parameters.get("maxPRight").asInt();

//            Create a new RunParameters object
            RunParameters runParameters = new RunParameters(inputPath, simMetricName, maxPLeft, maxPRight);
            runParameters.setOutputPath(outputPath);

//            Set the optional parameters
            Iterator<String> fieldNames = parameters.fieldNames();
            while (fieldNames.hasNext()){
                String fieldName = fieldNames.next();
                if (!fieldName.equals("outputPath") && !fieldName.equals("simMetricName") && !fieldName.equals("maxPLeft") && !fieldName.equals("maxPRight")){
                    runParameters.set(fieldName, parameters.get(fieldName).asText());
                }
            }

//            Set the minio environment variables if given
            if (jsonNode.has("minio")){
                JsonNode minio = jsonNode.get("minio");
                System.setProperty("MINIO_ENDPOINT_URL", minio.get("endpoint_url").asText());
                System.setProperty("MINIO_ACCESS_KEY", minio.get("id").asText());
                System.setProperty("MINIO_SECRET_KEY", minio.get("key").asText());

                if (minio.has("skey")){
                    System.setProperty("MINIO_SESSION_TOKEN", minio.get("skey").asText());
                }
            }

            return runParameters;
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

//    public static RunParameters parseArgs(String[] args){
//        // Parse necessary arguments (and delete them from the list)
//        RunParameters runParameters = parseRequired(args);
//
//        if (args.length == 5){
//            return runParameters;
//        }
//
//        // Iterate through the optional command-line arguments
//        for (int i=5; i<args.length; i++) {
//            try {
//                String arg = args[i];
//                if (arg.startsWith("--")) {
//                    parseNamedArg(runParameters, arg);
//                } else if (arg.startsWith("-")) {
////                If second argument is not present, make sure that the first argument is a boolean
//                    if (i + 1 < args.length) {
//                        if (args[i + 1].startsWith("-")) {
//                            parseAbbreviatedArg(runParameters, arg, "true");
//                        } else {
//                            parseAbbreviatedArg(runParameters, arg, args[i + 1]);
//                            i++;
//                        }
//                    } else {
//                        parseAbbreviatedArg(runParameters, arg, "true");
//                    }
//                } else {
//                    throw new InputMismatchException("Invalid format for option: " + arg);
//                }
//            } catch (NoSuchFieldException e){
//                throw new InputMismatchException("Invalid option: " + e.getMessage());
//            } catch (IllegalAccessException e){
//                throw new InputMismatchException("Invalid value for option: " + e.getMessage());
//            }
//        }
//
//        return runParameters;
//    }

//    private static RunParameters parseRequired(String[] args){
//        String inputInstructions = ", mind that the first 5 arguments should be <inputPath> <outputPath> <simMetricName> <maxPLeft> <maxPRight>";
//
//        // Parse necessary arguments (first 5 non-named or abbreviated arguments)
//        if (args.length < 5){
//            throw new InputMismatchException("Not enough arguments" + inputInstructions);
//        }
//
////        First extract the output path (and delete it from the list)
//        String outputPath = args[1];
//        args = lib.remove(args, 1);
//
////        Check if output path is valid
//        if (!lib.isValidPath(outputPath)){
//            throw new InputMismatchException("Invalid output path: " + outputPath);
//        }
//
//        int i = 0;
//        try {
//            String inputPath = args[i++];
//            SimEnum simMetricName = SimEnum.valueOf(args[i++].toUpperCase());
//            int maxPLeft = Integer.parseInt(args[i++]);
//            int maxPRight = Integer.parseInt(args[i++]);
//
//            RunParameters rp = new RunParameters(inputPath, simMetricName, maxPLeft, maxPRight);
//            rp.setOutputPath(outputPath);
//
//            return rp;
//        } catch (NumberFormatException e){
//            throw new InputMismatchException("Invalid format for option: " + args[i] + inputInstructions);
//        }
//    }

//    private static void parseAbbreviatedArg(RunParameters runParameters, String arg1, String arg2) throws NoSuchFieldException, IllegalAccessException{
////        Check if the argument is in the form -key value
//        String key = arg1.substring(1);
//        Object value = lib.parseString(arg2);
//        runParameters.set(key, value);
//    }
//
//    private static void parseNamedArg(RunParameters runParameters, String arg) throws NoSuchFieldException, IllegalAccessException{
//        // Check if the argument is in the form --key=value
//        int equalsIndex = arg.indexOf('=');
//        if (equalsIndex != -1) {
//            String key = arg.substring(2, equalsIndex);
//            Object value = lib.parseString(arg.substring(equalsIndex + 1));
//            runParameters.set(key, value);
//        } else {
//            throw new InputMismatchException("Invalid format for option: " + arg);
//        }
//    }

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
//        outputPath += "/" + UUID.randomUUID().toString();
        Logger.getGlobal().info("Saving results and stats to " + outputPath);

        //        Create the output directory
        lib.createDir(outputPath);

        DataHandler outputHandler = runParameters.getOutputHandler();
        StatBag statBag = runParameters.getStatBag();
        ResultSet resultSet = runParameters.getResultSet();

//        Save the parameters as a json file
        outputHandler.writeToFile(outputPath + "/parameters.json", runParameters.toJson());

//        Save the statBag as a json file
        outputHandler.writeToFile(outputPath + "/stats.json", statBag.toJson());

//        Save the results as a json file
        outputHandler.writeToFile(outputPath + "/results.json", resultSet.toJson());
    }

    private static JsonObject getResponse(RunParameters runParameters){
        //        Prepare the output response
        JsonObject outputJson = new JsonObject();
        outputJson.addProperty("message", "Correlation Detective run completed successfully");

//        Output paths
        JsonArray outputArray = new JsonArray();

        JsonObject outputObject = new JsonObject();
        outputObject.addProperty("path", runParameters.getOutputPath() + "/results.json");
        outputObject.addProperty("name", "The query result");
        outputArray.add(outputObject);

        outputObject = new JsonObject();
        outputObject.addProperty("path", runParameters.getOutputPath() + "/parameters.json");
        outputObject.addProperty("name", "The parameters of the query (i.e., the run)");
        outputArray.add(outputObject);

        outputObject = new JsonObject();
        outputObject.addProperty("path", runParameters.getOutputPath() + "/stats.json");
        outputObject.addProperty("name", "Statistics of the run (e.g., runtimes of each of the subprocesses)");
        outputArray.add(outputObject);

        outputJson.add("output", outputArray);

//        Parameters
        JsonObject metrics = new JsonObject();
        metrics.add("statistics", runParameters.getStatBag().toJsonElement().getAsJsonObject());
        metrics.add("run_parameters", runParameters.toJsonElement().getAsJsonObject());
        outputJson.add("metrics", metrics);

        outputJson.addProperty("status", 200);

        return outputJson;
    }
}