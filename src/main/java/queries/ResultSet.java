package queries;

import _aux.lib;
import _aux.lists.FastArrayList;
import bounding.ClusterCombination;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import core.RunParameters;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Class that holds the (running) results of a query as a set of (positive) ClusterCombinations
public class ResultSet {
    private RunParameters runParameters;
    private QueryTypeEnum queryType;
    private int topK;
    private RunningThreshold runningThreshold;


    //    In case topk- this holds at most k results in a MinHeap
    @Getter @Setter
    private Queue<ResultObject> resultObjects;

    private List<ResultTuple> resultTuples;

//    For saving results of topk queries

    @Getter
    private final LinkedList<ResultObject> resultHistory = new LinkedList<>();


    public final static int MAX_RESULTS = 1000000;

//--------------------------------------------------------------

    public ResultSet(RunParameters runParameters){
        this.runParameters = runParameters;
        this.queryType = runParameters.getQueryType();
        this.topK = runParameters.getTopK();
        this.runningThreshold = runParameters.getRunningThreshold();


        if (queryType == QueryTypeEnum.TOPK) {
            resultObjects = new PriorityQueue<>(2*topK, Comparator.comparing(ResultObject::getSimilarity));
        } else {
            resultObjects = new LinkedList<>();
        }
    }

    public List<ResultTuple> getResultTuples() throws IllegalAccessError {
//        Throw warning if result tuples are not yet computed
        if (resultTuples == null){
            Logger.getGlobal().warning("Result tuples not yet computed. Closing resultSet now.");
            close();
        }
        return resultTuples;
    }

    public void add(ResultObject resultObject) throws ProgressiveStopException {
        FastArrayList<ResultObject> tmp = new FastArrayList<>(Arrays.asList(resultObject));
        this.addAll(tmp);
    }

    public void addAll(FastArrayList<ResultObject> newResults) throws ProgressiveStopException {
        switch (queryType) {
            case TOPK: {
                for (ResultObject res : newResults) {
                    double sim = res.getSimilarity();

//                Add to topk (if still necessary)
                    synchronized (resultObjects) {
                        if (sim > runningThreshold.get()) {
                            resultObjects.add(res);

//                            Get rid of worst result and update threshold
                            if (resultObjects.size() > topK) {
                                resultObjects.poll(); // Remove worst result
                                runningThreshold.setThreshold(resultObjects.peek().getSimilarity()); // Update threshold
                            }
                        }
                    }
                }

                break;
            }
            case PROGRESSIVE: {
                synchronized (resultObjects) {
                    if (newResults.size() + resultObjects.size() > topK) {
                        for (int i = 0; i < topK - resultObjects.size(); i++) {
                            this.resultObjects.add(newResults.remove(0));
                        }
                        throw new ProgressiveStopException("Early stopping - required results reached");
                    } else {
                        this.resultObjects.addAll(newResults.toList());
                    }
                }
                break;
            }
            case THRESHOLD: {
                synchronized (resultObjects) {
//                    Remove some results first if needed
                    if (resultObjects.size() + newResults.size() > MAX_RESULTS) {
                        for (int i = 0; i < newResults.size(); i++) {
                            resultObjects.poll();
                        }
                    }
                    this.resultObjects.addAll(newResults.toList());
                }
                break;
            }
        }
    }

//    Closes the result set -- maps all ResultObjects to ResultTuples
    public List<ResultTuple> close() throws IllegalAccessError{
//      Throw warning if result tuples are already computed
        if (resultTuples != null){
            Logger.getGlobal().warning("ResultSet already closed! Returning existing result tuples.");
            return resultTuples;
        }

        resultTuples = new ArrayList<>(resultObjects.size() + 1);
        for (ResultObject res: resultObjects){
            if (res instanceof ResultTuple){
                resultTuples.add((ResultTuple) res);
            } else if (res instanceof ClusterCombination){
                resultTuples.add(((ClusterCombination) res).toResultTuple(runParameters.getHeaders()));
            }
        }
        return resultTuples;
    }

    public int size() {
        return resultTuples == null ? resultObjects.size() : resultTuples.size();
    }


//    Compare results with expected results
    public double[] computePrecisionRecall(List<ResultTuple> expectedResults){
//        Close the result set first if not yet done
        List<ResultTuple> results = this.close();

//        Sort both sets
        results.forEach(ResultTuple::sortSides);
        expectedResults.forEach(ResultTuple::sortSides);

        results.sort(ResultTuple::compareTo);

        int tp = 0;
        int fp = 0;
        int fn = 0;

//        Assert.assertEquals(expectedResults.size(), actualResults.size());
        for (int i = 0; i < expectedResults.size(); i++) {
            ResultTuple expected = expectedResults.get(i);
            boolean inResults = results.contains(expected);
            if (!inResults){
                fn++;
                Logger.getGlobal().finer(String.format("Expected result %s not found in results", expected));
                continue;
            }
            tp++;
        }

        //        Also do left join on results to check if there are any unexpected results
        for (int i = 0; i < results.size(); i++) {
            ResultTuple result = results.get(i);
            boolean inExpected = expectedResults.contains(result);
            if (!inExpected){
                fp++;
                Logger.getGlobal().finer(String.format("Unexpected result %s found in results", result));
                continue;
            }
            tp++;
        }

        double precision = (double) tp / (tp + fp);
        double recall = (double) tp / (tp + fn);
        return new double[]{precision, recall};
    }

    /**
     * Reconsider all combinations in this result set by re-computing the similarities given a certain dataset and threshold.
     * Often used to remove false positives after dimensionality reduction.
     * @param data The dataset to re-compute the similarities on.
     * @param threshold The threshold to use for filtering.
     * @return A filtered result set with the updated similarities.
     */
    public ResultSet filterFPs(double[][] data, double threshold) {
//        Close the result set first
        List<ResultTuple> results = this.close();
        List<ResultTuple> newResults = lib.getStream(results, runParameters.isParallel()).filter(result -> {
            double[][] X = Arrays.stream(result.LHS).mapToObj(i -> data[i]).toArray(double[][]::new);
            double[][] Y = Arrays.stream(result.RHS).mapToObj(i -> data[i]).toArray(double[][]::new);
            double sim = runParameters.getSimMetric().sim(X, Y);
            result.setSimilarity(sim);
            return sim >= threshold;
        }).collect(Collectors.toList());

        Logger.getGlobal().info(String.format("Filtered %d results to %d results", results.size(), newResults.size()));
        runParameters.getStatBag().precision = (double) newResults.size() / results.size();

        resultTuples = newResults;
        return this;
    }

    /**
     * Transforms the result to a list of resultTuples if not already done and returns it as a json string.
     * @return A json string representing the result set.
     */
    public String toJson(Gson gson) {
        if (resultTuples == null) {
            this.close();
        }
        return gson.toJson(resultTuples);
    }

    public JsonElement toJsonElement(Gson gson) {
        if (resultTuples == null) {
            this.close();
        }
        return gson.toJsonTree(resultTuples);
    }

//    Save the resultSet as a csv file
    public void saveAsCSV(String outputPath){
        List<ResultTuple> results = this.close();

        //            Make root dirs if necessary
        String rootdirname = outputPath.substring(0, outputPath.lastIndexOf("/"));
        new File(rootdirname).mkdirs();

//        Write the results as a csv file
        try {
            File file = new File(outputPath);

            FileWriter fw = new FileWriter(file, false);

//            Write header
            fw.write("lhs,rhs,headers1,headers2,sim,timestamp\n");

//            Write results
            for (ResultTuple result : results) {
                if (result.LHS == null) continue;
                result.sortSides();

                fw.write(String.format("%s,%s,%s,%s,%.4f,%d%n",
                        Arrays.stream(result.LHS).mapToObj(String::valueOf).collect(Collectors.joining("-")),
                        Arrays.stream(result.RHS).mapToObj(String::valueOf).collect(Collectors.joining("-")),
                        String.join("-", result.lHeaders),
                        String.join("-", result.rHeaders),
                        result.similarity,
                        result.timestamp
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAsJson(String outputPath){
        lib.stringToFile(toJson(runParameters.getGson()), outputPath);
    }
}
