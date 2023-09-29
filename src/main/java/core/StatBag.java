package core;

import _aux.Stage;
import _aux.lib;
import _aux.lists.FastArrayList;
import algorithms.AlgorithmEnum;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.time.StopWatch;
import similarities.MultivariateSimilarityFunction;

import java.io.File;
import java.io.FileWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StatBag {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Time{} // Time statistic measured in nanos

    @NonNull private RunParameters runParameters;

            @Getter private StopWatch stopWatch = new StopWatch();
    @Expose @Getter @Setter private double totalDuration;
    @Expose @Getter @Setter private FastArrayList<Stage> stageDurations;

//    CC stats
    @Expose @Getter private AtomicLong nLookups = new AtomicLong(0);
    @Expose @Getter private AtomicLong nCCs = new AtomicLong(0);
    @Expose @Getter private AtomicLong nSecCCs = new AtomicLong(0);
    @Expose @Getter private AtomicLong nParallelCCs = new AtomicLong(0);
            @Getter private AtomicLong totalCCSize = new AtomicLong(0);
    @Expose private double avgCCSize = 0;
    @Expose @Getter private AtomicLong nPosDCCs = new AtomicLong(0);
    @Expose @Getter private AtomicLong nNegDCCs = new AtomicLong(0);

    @Expose public long actualHashSize = 0;

    //    Discounting stats
    @Expose @Getter private AtomicLong nDiscountedCCs = new AtomicLong(0);
    @Expose @Getter private AtomicLong nDiscountCuts = new AtomicLong(0);
    @Expose @Time @Getter private AtomicLong discountingTime = new AtomicLong(0);

    //    Time stats
            @Time @Getter private AtomicLong DFSTime = new AtomicLong(0);
            @Time public AtomicLong distTime = new AtomicLong(0);
            @Time private long GCTime = 0;

    //    Result stats
            public double precision = 1;
            public double recall = 1;

    public static <T> T timeit(Supplier<T> function, AtomicLong timeStatistic){
        long startTime = System.nanoTime();
        try {
            return function.get();
        } finally {
            timeStatistic.getAndAdd(System.nanoTime() - startTime);
        }
    }

    public static void timeit(Runnable function, AtomicLong timeStatistic) {
        long startTime = System.nanoTime();
        try {
            function.run();
        } finally {
            timeStatistic.getAndAdd(System.nanoTime() - startTime);
        }
    }

//    Only computed if experiment is true
    public void incrementStat(AtomicLong stat){
        if (runParameters.isMonitorStats()) stat.incrementAndGet();
    }

//    Only computed if experiment is true
    public void addStat(AtomicLong stat, Supplier<Integer> value){
        if (runParameters.isMonitorStats()) stat.addAndGet(value.get());
    }


//    ------------------ POST PROCESSING ------------------

    public void computeAvgStats() {
        avgCCSize = (totalCCSize.get() / (double) nCCs.get());
    }
    private void prepareStats(){
        MultivariateSimilarityFunction simMetric = runParameters.getSimMetric();

//        Set actual hash size
        actualHashSize = !runParameters.isEmpiricalBounding() && !runParameters.getSimMetric().isEmpiricalBounded() ?
                runParameters.getSimMetric().centroidCache.size(): runParameters.getSimMetric().pairwiseClusterCache.size();

        computeAvgStats();
        GCTime = lib.getGCTime();
    }

    public void printStats(){
        prepareStats();

//        Print all non-hidden stats
        Arrays.stream(this.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Expose.class))
                .forEach(field -> {
                    try {
                        Object val = field.get(this);

//                        If value is atomic, get the value
                        if (val instanceof AtomicLong) val = ((AtomicLong) val).get();
                        else if (val instanceof AtomicDouble) val = ((AtomicDouble) val).get();

//                        If value is time, convert to seconds
                        if (field.isAnnotationPresent(Time.class)) {
                            val = lib.nanoToSec((long) val) / runParameters.getThreads();
                        }

//                        Print per type
                        if (val instanceof Double) Logger.getGlobal().fine(String.format("%-30s %.5f", field.getName(), (double) val));
                        else if (val instanceof Long) Logger.getGlobal().fine(String.format("%-30s %d", field.getName(), (long) val));
                        else if (val instanceof Integer) Logger.getGlobal().fine(String.format("%-30s %d", field.getName(), (int) val));
                        else if (val instanceof String) Logger.getGlobal().fine(String.format("%-30s %s", field.getName(), val));
                        else if (val instanceof Boolean) Logger.getGlobal().fine(String.format("%-30s %b", field.getName(), val));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });

        printStageDurations();
    }

    public void printStageDurations(){
        lib.printBar(Logger.getGlobal());
        int i = 0;
        for (Stage stageDuration: this.stageDurations){
            if (stageDuration.expectedDuration != null){
                Logger.getGlobal().fine(String.format("Duration stage %d. %-50s: %.5f sec (estimated %.5f sec)",
                        i, stageDuration.name, stageDuration.duration, stageDuration.expectedDuration));
            } else {
                Logger.getGlobal().fine(String.format("Duration stage %d. %-50s: %.5f sec",
                        i, stageDuration.name, stageDuration.duration));
            }
            i++;
        }
        Logger.getGlobal().info(String.format("%-68s: %.5f sec", "Total duration", this.totalDuration));
    }

    public Map<String, Object> getAttributeMap(){
        return Arrays.stream(this.getClass().getDeclaredFields()) // get all attributes of statbag class
                .filter(field -> field.isAnnotationPresent(Expose.class))
                .collect(Collectors.toMap(Field::getName, field -> {
                    try {
                        return field.get(this);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return null;
                }));
    }

    public void saveAsCSV(String outputPath){

        try {
//            Create directory if it doesn't exist
            String rootdirname = outputPath.substring(0, outputPath.lastIndexOf("/"));
            new File(rootdirname).mkdirs();

            File file = new File(outputPath);

            boolean exists = file.exists();

            FileWriter resultWriter = new FileWriter(outputPath, true);

            Logger.getGlobal().info("saving to " + outputPath);

//            Also get statbag fields
            List<Field> statBagFields = Arrays.stream(this.getClass().getDeclaredFields()) // get all attributes of statbag class
                    .filter(field -> field.isAnnotationPresent(Expose.class))
                    .collect(Collectors.toList());


//            Add all run parameters
            Map<String, Object> fieldMap = runParameters.getParameterMap();

//            Add all statbag fields
            fieldMap.putAll(getAttributeMap());

//            Add stagedurations as nested list
            String sdString = this.stageDurations.stream().map(st -> String.valueOf(st.getDuration())).collect(Collectors.joining("-"));
            fieldMap.put("stageDurations", sdString);

//            Create header
            if (!exists) {
                String header = fieldMap.keySet().stream().collect(Collectors.joining(","));
                resultWriter.write(header + "\n");
            }

//            Create row
            String row = fieldMap.values().stream().map(o -> o == null ? "null": o.toString()).collect(Collectors.joining(","));
            resultWriter.write(row + "\n");
            resultWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toJson(){
        prepareStats();
        return runParameters.getGson().toJson(this);
    }

    public JsonElement toJsonElement(){
        prepareStats();
        return runParameters.getGson().toJsonTree(this);
    }

    public void saveAsJson(String outputPath){
        lib.stringToFile(toJson(), outputPath);
    }
}
