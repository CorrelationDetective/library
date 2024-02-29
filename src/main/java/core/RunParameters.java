package core;

import _aux.Pair;
import algorithms.AlgorithmEnum;
import bounding.BoundDiscounting;
import bounding.ClusterCombination;
import bounding.RecursiveBounding;
import clustering.ClusteringAlgorithmEnum;
import clustering.HierarchicalClustering;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import data_io.DataHandler;
import data_io.FileHandler;
import data_io.MinioHandler;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.math3.util.FastMath;
import queries.QueryTypeEnum;
import queries.ResultSet;
import queries.RunningThreshold;
import similarities.MultivariateSimilarityFunction;
import similarities.SimEnum;
import similarities.functions.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.*;

@RequiredArgsConstructor
public class RunParameters {
//    Annotation that makes sure we check if the value is between min and max, if not, we clip it
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Between{
        double min();
        double max();
    }


//  ---------------------------  Required parameters  ---------------------------
    @Expose @NonNull @Getter @Setter private String inputPath;
    @Expose @NonNull @Getter @Setter private SimEnum simMetricName;
    @Expose @NonNull @Between(min = 1, max = 10) @Getter @Setter private  Integer maxPLeft;
    @Expose @NonNull @Between(min = 0, max = 10) @Getter @Setter private  Integer maxPRight;


    //  ---------------------------  Helpers  ---------------------------
    @Getter private Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT)
            .serializeSpecialFloatingPointValues()
            .create();
    @Getter private DataHandler inputHandler;
    @Getter private DataHandler outputHandler;
    public static String S3_PREFIX = "s3://";

    //  --------------------------- Logging ---------------------------
    @Getter @Setter private  Level logLevel = Level.INFO;
    @Expose @Getter         private String dateTime = (new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss")).format(new Date());
    @Getter @Setter private  boolean monitorStats = true;

    @Expose @Getter private int threads = FastMath.min(80, Runtime.getRuntime().availableProcessors()*4);
    @Expose @Getter @Setter private String outputPath;

    //  ---------------------------  Run details ---------------------------
    @Getter @Setter private  AlgorithmEnum algorithm = AlgorithmEnum.SIMILARITY_DETECTIVE;

    @Expose @Getter @Setter private  boolean parallel = true;
    @Expose @Getter @Setter private  boolean random = true;
    @Expose @Getter @Setter private  int seed = 0;
            @Getter private  ForkJoinPool forkJoinPool = new ForkJoinPool(threads);

//  ---------------------------  Query ---------------------------
    @Expose @Getter @Setter private  QueryTypeEnum queryType = QueryTypeEnum.TOPK;

            @Getter @Setter private  MultivariateSimilarityFunction simMetric;
    @Expose @Getter @Setter private  double tau;
    @Expose @Getter @Setter private  RunningThreshold runningThreshold;
    @Expose @Between(min = 0, max = Double.MAX_VALUE) @Getter @Setter private  double minJump = 0;
    @Expose @Getter @Setter private  boolean irreducibility = false;
    @Expose @Between(min = 0, max = 100_000) @Getter @Setter private  int topK = 100;

    @Expose @Getter @Setter private  boolean allowVectorOverlap = false;
            @Getter @Setter private  ResultSet resultSet;

//  ---------------------------  Data ---------------------------
            @Getter private  String[] headers;
            @Getter private  double[][] data;
            @Getter @Setter private double[][] orgData;
    @Expose @Between(min = 1, max = Integer.MAX_VALUE) @Getter @Setter private  int nVectors = 1000;
    @Expose @Between(min = 1, max = Integer.MAX_VALUE) @Getter @Setter private  int nDimensions = (int) 1e6;
    @Expose @Between(min = 0, max = Integer.MAX_VALUE) @Getter @Setter private  int partition;

//  ---------------------------  Dimensionality reduction ---------------------------
    @Expose @Getter @Setter private  boolean dimensionalityReduction;
    @Expose @Between(min = 0, max = 1) @Getter @Setter private  double dimredEpsilon = 0.1;
    @Expose @Between(min = 0, max = 1) @Getter @Setter private  double dimredDelta = 0.8;
    @Expose @Getter @Setter private  boolean dimredCorrect = true;
    @Expose @Getter @Setter private @Between(min = 1, max = Integer.MAX_VALUE)  Integer dimredComponents = null;

//  ---------------------------  Discounting ---------------------------
    @Expose  @Getter @Setter private  boolean discounting = false;
    @Expose @Between(min = 0, max = 2) @Getter @Setter private  double discountThreshold = .7;
    @Expose @Between(min = 1, max = Integer.MAX_VALUE) @Getter @Setter private  int discountTopK = 10; // The amount of extrema distances to store for each CC.
    @Expose @Between(min = 1, max = Integer.MAX_VALUE) @Getter @Setter private  int discountStep = 1;

//  ---------------------------  Bounding ---------------------------
    @Expose @Getter @Setter private  boolean empiricalBounding = true;
            @Getter @Setter private RecursiveBounding RB;
    @Getter PriorityQueue<ClusterCombination> postponedDCCs =
            new PriorityQueue<>(10000, Comparator.comparingDouble(ClusterCombination::getCriticalShrinkFactor));

//  ---------------------------  Clustering ---------------------------
            @Getter @Setter private HierarchicalClustering HC;
            @Getter @Setter private BoundDiscounting BD;

    @Expose @Between(min = 1, max = Integer.MAX_VALUE) @Getter @Setter private  Integer kMeans = null;
    @Expose @Getter @Setter private  boolean geoCentroid = false;
    @Expose @Between(min = 0, max = Double.MAX_VALUE) @Getter @Setter private  double startEpsilon;
    @Expose @Between(min = 0, max = 1) @Getter @Setter private  double epsilonMultiplier = 0.8;
    @Expose @Between(min = 1, max = Integer.MAX_VALUE) @Getter @Setter private  int maxLevels = 20;
    @Expose @Getter @Setter private  ClusteringAlgorithmEnum clusteringAlgorithm = ClusteringAlgorithmEnum.KMEANS;
    @Expose @Between(min = 0, max = Integer.MAX_VALUE) @Getter @Setter private  int breakFirstKLevelsToMoreClusters = 0;
    @Expose @Between(min = 1, max = Integer.MAX_VALUE) @Getter @Setter private  int clusteringRetries = 20;

    @Expose @Between(min = 1, max = Integer.MAX_VALUE) @Getter @Setter private  int hashSize;

//  ---------------------------  Routing ---------------------------
    @Expose @Between(min = 0, max = 1) @Getter @Setter private  double BFSRatio = 0.5;
    @Expose @Getter @Setter private  double BFSFactor;

//  ---------------------------  Top-k ---------------------------
    @Expose @Getter @Setter private  double shrinkFactor = 0;

//  ---------------------------  Misc ---------------------------
            @Getter private  StatBag statBag;
            @Getter private  Random randomGenerator;
            @Getter private  double[][] pairwiseDistances;


    public void init(){
        configLogger(logLevel);
        check();
    }

    public void init(boolean initLogger){
        if (initLogger){
            configLogger(logLevel);
        }
        check();
    }

    public String toJson(){
        return gson.toJson(this);
    }

    public  JsonElement toJsonElement(){
        return gson.toJsonTree(this);
    }

    public Object get(String key) throws NoSuchFieldException, IllegalAccessException{
//        Get parameter value
        java.lang.reflect.Field field = RunParameters.class.getDeclaredField(key);
        field.setAccessible(true);
        return field.get(this);
    }

    public void set(String key, Object value) throws NoSuchFieldException, IllegalAccessException{
//        Set a parameter value, if parameter is enum, make all caps and parse it
        java.lang.reflect.Field field = RunParameters.class.getDeclaredField(key);
        field.setAccessible(true);

        if (field.getType().isEnum()){
            field.set(this, Enum.valueOf((Class<Enum>) field.getType(), value.toString().toUpperCase()));
        } else {
            field.set(this, value);
        }
    }



    //    Check and correct parameter values for incorrect configs
    private  void check(){
        checkRequired();

        checkDataParameters();
        dimensionalityReductionChecks();
        queryTypeChecks();
        queryConstraintChecks();
        simMetricChecks();
        discountingChecks();
        corrPatternChecks();

        loadDataset();

        setDependentVariables();

        checkBetween();
    }

    //    Check if all required parameters (i.e., with @NonNull) are set
    private  void checkRequired(){
        for (java.lang.reflect.Field field : RunParameters.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(NonNull.class)) {
                try {
                    if (field.get(null) == null) {
                        throw new IllegalArgumentException("Required parameter " + field.getName() + " is not set");
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //    Check if parameters with between annotation are within the specified range
    private  void checkBetween(){
        for (java.lang.reflect.Field field : RunParameters.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(Between.class)) {
                try {
                    if (field.getType().equals(int.class)) {
                        int value = (int) field.get(this);
                        Between between = field.getAnnotation(Between.class);
                        if (value < between.min() || value > between.max()) {
                            Logger.getGlobal().severe("Parameter " + field.getName() + " is not between " + between.min() + " and " + between.max());
                            System.exit(1);
                        }
                    } else if (field.getType().equals(double.class)) {
                        double value = (double) field.get(this);
                        Between between = field.getAnnotation(Between.class);
                        if (value < between.min() || value > between.max()) {
                            Logger.getGlobal().severe("Parameter " + field.getName() + " is not between " + between.min() + " and " + between.max());
                            System.exit(1);
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    Check dataset parameters
    private void checkDataParameters(){
//        Check which filesystem is used for input and output
        inputHandler = inputPath.startsWith(S3_PREFIX) ? new MinioHandler(): new FileHandler();
        if (outputPath != null){
            outputHandler = outputPath.startsWith(S3_PREFIX) ? new MinioHandler(): new FileHandler();
        }
    }

    public  void loadDataset(){
        Pair<String[], double[][]> dataPair = inputHandler.readCSV(inputPath, nVectors, nDimensions, true, partition, null);
        headers = dataPair.x;
        data = dataPair.y;

        //        update parameters if we got less data
        nVectors = data.length;
        nDimensions = data[0].length;
    }

    public void setDependentVariables(){
//        Hash size based on fitted polynomial
        hashSize = (int) FastMath.ceil(5000 - 8.067 * nVectors + 0.53583 * nVectors * nVectors);

        statBag = new StatBag(this);
        randomGenerator = random ? new Random(): new Random(seed);
        runningThreshold = new RunningThreshold(tau);
        resultSet = new ResultSet(this);

        //        Simmetric dependent parameters
        startEpsilon = simMetric.simToDist(0.81*simMetric.MAX_SIMILARITY);
        dimensionalityReduction = !simMetric.isEmpiricalBounded();
        if (kMeans == null) kMeans = simMetric.isEmpiricalBounded() ? 30: 50;

        //        preprocess (if necessary)
        data = simMetric.preprocess(data);
        nDimensions = data[0].length;

        BFSFactor = simMetric.getMaxApproximationSize(BFSRatio);
    }

    public HierarchicalClustering initializeHC(){
        HC = new HierarchicalClustering(this);
        return HC;
    }

    public RecursiveBounding initializeRB(){
        RB = new RecursiveBounding(this);
        return RB;
    }

    public BoundDiscounting initializeBD(){
        BD = new BoundDiscounting(this);
        return BD;
    }

    private void discountingChecks(){
//        Discounting works only on empirical bounded metrics
        if (discounting && !simMetric.allowsDiscounting()){
            Logger.getGlobal().severe("Discounting is set, but the similarity metric does not support discounting, setting discounting to false");
            discounting = false;
        }

        discountTopK *= discountStep;
    }


    private void dimensionalityReductionChecks(){
//        If epsilon is set, delta should also be set
        if (dimredEpsilon > 0 && dimredDelta == 0){
            Logger.getGlobal().severe("Epsilon is set, but delta is not set, setting delta to 0.1");
            dimredDelta = 0.1;
        }
//        If delta is set, epsilon should also be set
        if (dimredDelta > 0 && dimredEpsilon == 0){
            Logger.getGlobal().severe("Delta is set, but epsilon is not set, setting epsilon to 0.3");
            dimredEpsilon = 0.3;
        }

//        If dimredComponents set, make sure its positive
        if (dimredComponents != null && dimredComponents <= 0){
            Logger.getGlobal().severe("dimredComponents is set, but is <= 0, setting it to null");
            dimredComponents = null;
        }
    }


    private  void queryTypeChecks(){
        //        Query-type specific corrections
        switch (queryType){
            case THRESHOLD: {
                if (tau == 0){
                    throw new InputMismatchException("Query type is threshold, but tau is not set");
                }
                if (topK > 0){
                    Logger.getGlobal().severe("Query type is threshold, but topK is > 0, setting topK to 0");
                    topK = 0;
                }
                if (shrinkFactor < 1){
                    Logger.getGlobal().severe("Query type is threshold, but shrinkFactor is < 1, setting shrinkFactor to 1");
                    shrinkFactor = 1;
                }
                break;
            }
            case TOPK: {
                if (topK <= 0){
                    Logger.getGlobal().severe("Query type is top-k, but topK is <= 0, setting topK to 100");
                    topK = 100;
                }
                if (minJump > 0) {
                    Logger.getGlobal().severe("Query type is top-k, but minJump is > 0, setting minJump to 0");
                    minJump = 0;
                }
                if (irreducibility) {
                    Logger.getGlobal().severe("Query type is top-k, but irreducibility is true, setting irreducibility to false");
                    irreducibility = false;
                }
                if (tau > 0){
                    Logger.getGlobal().severe("Query type is top-k, but tau is > 0, setting tau to 0");
                    tau = 1e-6;
                }
                break;
            }
        }
    }

    private  void queryConstraintChecks(){
        //        Irreducibility specific corrections
        if (irreducibility && minJump > 0){
            Logger.getGlobal().severe("Irreducibility is true, but minJump is > 0, setting minJump to 0");
            minJump = 0;
        }
    }

    private  void simMetricChecks(){
//        Specific class checks
        if (simMetricName.equals(SimEnum.TOTAL_CORRELATION)){
//           Always have irreducibility for threshold queries with TC
            if (!irreducibility && minJump == 0 && (queryType == QueryTypeEnum.THRESHOLD || queryType == QueryTypeEnum.PROGRESSIVE)) {
                Logger.getGlobal().severe("Irreducibility is trivial for threshold/progressive queries with total correlation. Irreducibility is set to true.");
                irreducibility = true;
            }
        }

        //        Get similarity function from enum
        switch (simMetricName){
            case PEARSON_CORRELATION: default: simMetric = new PearsonCorrelation(this); break;
            case SPEARMAN_CORRELATION: simMetric = new SpearmanCorrelation(this); break;
            case MULTIPOLE: simMetric = new Multipole(this); break;
            case EUCLIDEAN_SIMILARITY: simMetric = new EuclideanSimilarity(this); break;
            case MANHATTAN_SIMILARITY: simMetric = new ManhattanSimilarity(this); break;
            case TOTAL_CORRELATION: simMetric = new TotalCorrelation(this); break;
        }

        //        Set empirical bounding based on metric
        if (empiricalBounding && !simMetric.isEmpiricalBounded()){
            Logger.getGlobal().severe("The chosen similarity metric is not empirically bounded, setting empiricalBounding to false");
            empiricalBounding = false;
        }
    }

    //        Check if pleft and pright are correctly chosen
    private  void corrPatternChecks(){
        if (!simMetric.isTwoSided() && maxPRight > 0){
            Logger.getGlobal().severe("The chosen similarity metric is not two-sided, but pright is > 0, adding pright to pleft");
            maxPLeft += maxPRight;
            maxPRight = 0;
        }
        if (simMetric.isTwoSided() && maxPRight == 0){
            Logger.getGlobal().severe("The chosen similarity metric is two-sided, but pright is 0, setting pright to 1");
            maxPRight = 1;
            maxPLeft -= 1;
        }
    }

    public void configLogger(Level logLevel){
        Logger mainLogger = Logger.getGlobal();
        mainLogger.setUseParentHandlers(false);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private  final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";
            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        });
        handler.setLevel(logLevel);
        mainLogger.addHandler(handler);
        mainLogger.setLevel(logLevel);
    }

    public void computePairwiseDistances(){
        pairwiseDistances = simMetric.computePairwiseDistances(data);
    }
    public void computePairwiseDistances(double[][] data){pairwiseDistances = simMetric.computePairwiseDistances(data);}

    public Map<String, Object> getParameterMap(){
        Map<String, Object> parameterMap = new HashMap<>();
        for (java.lang.reflect.Field field : RunParameters.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(Expose.class)){
                try {
                    parameterMap.put(field.getName(), field.get(this));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return parameterMap;
    }
}

