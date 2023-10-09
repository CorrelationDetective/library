package algorithms.baselines;

import _aux.*;
import _aux.lists.FastArrayList;
import algorithms.Algorithm;
import _aux.StageRunner;
import core.RunParameters;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;
import queries.ResultSet;
import similarities.SimEnum;

import java.util.function.Function;

public abstract class Baseline extends Algorithm {
    protected Baseline(@NonNull RunParameters runParameters) {
        super(runParameters);
    }

    protected Baseline(@NonNull String inputPath, @NonNull SimEnum simMetricName, int maxPLeft, int maxPRight) {
        super(inputPath, simMetricName, maxPLeft, maxPRight);
    }


    public abstract void prepare();

    @Override
    public ResultSet run() {
        //        Initialize the parameters
        runParameters.init();
        printRunParameters();

        StageRunner stageRunner = new StageRunner();

        StopWatch stopWatch = runParameters.getStatBag().getStopWatch();

        //        Start the timer
        stopWatch.start();

        // --> STAGE 1 - Prepare
        stageRunner.run("Preparation phase", this::prepare, stopWatch);

        // --> STAGE 2 - Iterate and handle candidatesGet candidate pairs
        stageRunner.run("Iterate candidates", this::iterateCandidates, stopWatch);

        stopWatch.stop();
        runParameters.getStatBag().setTotalDuration(lib.nanoToSec(stopWatch.getNanoTime()));
        runParameters.getStatBag().setStageDurations(stageRunner.stageDurations);

        return runParameters.getResultSet();
    }

    private void iterateCandidates() {
        int n = runParameters.getNVectors();
        FastArrayList<AssessCandidateTask> tasks = new FastArrayList<>(n * n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i==j) continue;

                int[] LHS = runParameters.getMaxPRight() > 0 ? new int[1]: new int[2];
                LHS[0] = i;

                int[] RHS = runParameters.getMaxPRight() > 0 ? new int[1]: new int[0];

                if (runParameters.getMaxPRight() > 0) {
                    RHS[0] = j;
                } else {
                    LHS[1] = j;
                }

                AssessCandidateTask task = new AssessCandidateTask(LHS,RHS,0, this.getSimilarityComputer(), runParameters);
                if (runParameters.isParallel()){
                    runParameters.getForkJoinPool().execute(task);
                } else {
                    runParameters.getForkJoinPool().invoke(task);
                }
                tasks.add(task);
            }

            //  Wait for all tasks to finish
            for (AssessCandidateTask task : tasks) {
                task.join();
            }
        }
    }

    //    Go over candidate and check if it (or its subsets) has a significant similarity
    public Function getSimilarityComputer(){
        return o -> computeSimilarity((Pair<int[], int[]>) o);
    }

    public abstract double computeSimilarity(Pair<int[], int[]> candidate);

    public double[] linearCombination(FastArrayList<Integer> idx, double[] W){
        double[] v = new double[runParameters.getNDimensions()];
        for (int i = 0; i < idx.size(); i++) {
            v = lib.add(v, lib.scale(runParameters.getData()[idx.get(0)], W[i]));
        }
        return v;
    }


}
