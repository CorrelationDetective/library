package algorithms.performance;

import _aux.*;
import algorithms.Algorithm;
import _aux.StageRunner;
import bounding.RecursiveBounding;
import clustering.HierarchicalClustering;
import core.RunParameters;
import core.StatBag;
import lombok.NonNull;
import org.apache.commons.lang3.time.StopWatch;
import queries.ResultSet;
import similarities.SimEnum;

public class CorrelationDetective extends Algorithm {
    public CorrelationDetective(@NonNull RunParameters runParameters) {
        super(runParameters);
    }

    public CorrelationDetective(@NonNull String inputPath, @NonNull SimEnum simMetricName, int maxPLeft, int maxPRight) {
        super(inputPath, simMetricName, maxPLeft, maxPRight);
    }

    @Override
    public ResultSet run() {
//        Initialize the parameters
        runParameters.init();

        StageRunner stageRunner = new StageRunner();

        StatBag statBag = runParameters.getStatBag();
        StopWatch stopWatch = statBag.getStopWatch();

//        Start the timer
        stopWatch.start();

//        STAGE 1 - Compute pairwise distances ON ORIGINAL DATA (also when using dimensionality reduction)
        boolean dimensionalityReduction = runParameters.isDimensionalityReduction();
        runParameters.setDimensionalityReduction(false);
        stageRunner.run("Compute pairwise distances",
                () -> runParameters.computePairwiseDistances(runParameters.getOrgData()), stopWatch);
        runParameters.setDimensionalityReduction(dimensionalityReduction);

//        STAGE 2 - Hierarchical clustering
        HierarchicalClustering HC = runParameters.initializeHC();
        stageRunner.run("Hierarchical clustering", HC::run, stopWatch);

//        STAGE 3 - Recursive bounding
        RecursiveBounding RB = runParameters.initializeRB();
        stageRunner.run("Recursive bounding", RB::run, stopWatch);

        ResultSet resultSet = runParameters.getResultSet();

//        STAGE 4 - Post-processing -- removing FPs (optional)
        if (dimensionalityReduction){
//            Temporarily set dimensionality reduction to false to make sure post-processing uses the original data
            runParameters.setDimensionalityReduction(false);
            stageRunner.run("Post-processing", () -> resultSet.filterFPs(runParameters.getOrgData(), runParameters.getTau()),
                    stopWatch);
            runParameters.setDimensionalityReduction(true);
        }

        stopWatch.stop();
        statBag.setTotalDuration(lib.nanoToSec(stopWatch.getNanoTime()));
        statBag.setStageDurations(stageRunner.stageDurations);

        return resultSet;
    }


}
