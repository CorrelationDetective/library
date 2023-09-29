package bounding;

import _aux.lib;
import _aux.lists.FastArrayList;
import queries.*;
import core.RunParameters;
import clustering.Cluster;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RecursiveBounding {
    private RunParameters runParameters;
    private final Cluster rootCluster;

    //    Statistics
    public AtomicLong nNegDCCs = new AtomicLong(0);
    public double DFSTime;

//    Constructor
    public RecursiveBounding(RunParameters runParameters) {
        this.runParameters = runParameters;

        if (runParameters.getHC().clusterTree.isEmpty() || runParameters.getHC().clusterTree.get(0).isEmpty()){
            throw new IllegalArgumentException("ClusterTree of HC is empty, RecursiveBounding can only be initialized when HC is run");
        }
        this.rootCluster = runParameters.getHC().clusterTree.get(0).getFirst();

//        Initialize boundDiscounting
        runParameters.initializeBD();
    }

    public ResultSet run() {
//        Expand topK when running topk or progressive query
        QueryTypeEnum queryType = runParameters.getQueryType();
        boolean expandTopK = queryType == QueryTypeEnum.TOPK || queryType == QueryTypeEnum.PROGRESSIVE;

        try {
            complexityClimb(true, expandTopK);
        } catch (ProgressiveStopException e) {
            Logger.getGlobal().info(e.getMessage());
        }

//        Set statistics
        runParameters.getStatBag().addStat(runParameters.getStatBag().getNPosDCCs(), runParameters.getResultSet()::size);

//        Convert to tuples
        return runParameters.getResultSet();
    }

    public void complexityClimb(boolean fullClimb, boolean expandTopK) throws ProgressiveStopException {
        // Setup first iteration
        int pLeft = runParameters.getMaxPRight() > 0 ? 1 : 2;
        int pRight = runParameters.getMaxPRight() > 0 ? 1 : 0;

        ClusterCombination rootCandidate = getRootCandidate(pLeft,pRight);

        double requestedShrinkFactor = runParameters.getShrinkFactor();
        runParameters.setShrinkFactor(1);

        while (true) {
            int headPLeft = rootCandidate.getLHS().length;
            int headPRight = rootCandidate.getRHS().length;

//            -------------- Handle complexity level --------------
            Logger.getGlobal().info(String.format("Starting on combinations with complexity (%d,%d)", headPLeft, headPRight));
            mineSimilarityPattern(rootCandidate);
            Logger.getGlobal().info(String.format("----- Done with complexity level, current resultSet size: %d", runParameters.getResultSet().size()));

//            -------------- Prepare for next complexity level -------------

//            Check if we are done
            if (headPLeft == runParameters.getMaxPLeft() && headPRight == runParameters.getMaxPRight())
                break;

//            Always expand left first
            boolean expandLeft = headPRight == runParameters.getMaxPRight() || (headPLeft == headPRight && headPLeft < runParameters.getMaxPLeft());

            //  Expand topK if necessary
            if (expandTopK){
                expandTopK(expandLeft, headPLeft, headPRight);
            }

            //  Increase complexity by expanding the rootCandidate (i.e. fully traverse combination tree)
            int nextPLeft = fullClimb ? (expandLeft ? headPLeft + 1 : headPLeft): runParameters.getMaxPLeft();
            int nextPRight = fullClimb ? (expandLeft ? headPRight : headPRight + 1): runParameters.getMaxPRight();
            rootCandidate = getRootCandidate(nextPLeft, nextPRight);

            //   Set shrink factor back to original value
            runParameters.setShrinkFactor(requestedShrinkFactor);
        }
    }

    public ClusterCombination getRootCandidate(int pLeft, int pRight){
        Cluster[] LHS = new Cluster[pLeft];
        Cluster[] RHS = new Cluster[pRight];
        Arrays.fill(LHS, rootCluster);
        Arrays.fill(RHS, rootCluster);
        return new ClusterCombination(LHS, RHS, 0, (int) Math.pow(rootCluster.size(), pLeft+pRight), runParameters.isAllowVectorOverlap());
    }

    public void mineSimilarityPattern(ClusterCombination rootCandidate) throws ProgressiveStopException {
        double shrinkFactor = runParameters.getShrinkFactor();
        RunningThreshold runningThreshold = runParameters.getRunningThreshold();
        ForkJoinPool forkJoinPool = runParameters.getForkJoinPool();

        try {
//            Start with initial BFS scan with given shrinkFactor, postponing CCs if necessary
            forkJoinPool.invoke(new RecursiveBoundingTask(rootCandidate, runParameters));

            Logger.getGlobal().info("Done with initial scan, now starting DFS with threshold " +runningThreshold.get());

//            Skip if not postponing
            if (shrinkFactor == 1)
                return;

            long startTime = System.nanoTime();

            // Now iterate over approximated DCCs without shrinking, starting with highest priority
            runParameters.setShrinkFactor(1);
            runCCs(new FastArrayList<>(RecursiveBoundingTask.postponedDCCs));

            DFSTime += (System.nanoTime() - startTime) / 1e9;
            Logger.getGlobal().info("Done with full scan, threshold now " +runningThreshold.get());

        } catch (RuntimeException e) {
            Exception rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = (Exception) rootCause.getCause();
            }
            if (rootCause instanceof ProgressiveStopException) {
                throw (ProgressiveStopException) rootCause;
            } else {
                throw e;
            }
        }
    }

    public void runCCs(FastArrayList<ClusterCombination> ccQueue) throws ProgressiveStopException{

        if (runParameters.isParallel()){
            //  Invoke all tasks
            List<RecursiveBoundingTask> tasks = ccQueue.stream().map(cc -> {
                RecursiveBoundingTask task = new RecursiveBoundingTask(cc, runParameters);
                runParameters.getForkJoinPool().execute(task);
                return task;
            }).collect(Collectors.toList());

            //  Wait for all tasks to finish
            for (RecursiveBoundingTask task : tasks) {
                task.join();
            }
        } else {
            //  Run sequentially
            for (ClusterCombination cc : ccQueue) {
                new RecursiveBoundingTask(cc, runParameters).compute();
            }
        }
    }

    //      Expand the topmost combinations in the topk and compute their similarity exactly to increase threshold
    public void expandTopK(boolean expandLeft, int currentLSize, int currentRSize) throws ProgressiveStopException {
        int topK = runParameters.getTopK();
        int n = runParameters.getNVectors();

        int nPeak = runParameters.getResultSet().size();
        if (nPeak < 1) return;

        boolean equalSideLength = currentLSize == currentRSize;

//          Copy old ResultDCCs so that we can reset it after exhaustivePrioritization (to avoid duplicates)
        PriorityQueue<ResultObject> oldResultDCCs = new PriorityQueue<>(runParameters.getResultSet().getResultObjects());

//        Get topK combinations (of maximal size!) and sort them by their similarity in descending order
        FastArrayList<ClusterCombination> topKCombinations = new FastArrayList<>(topK*2);
        for (ResultObject res: runParameters.getResultSet().getResultObjects()){
            ClusterCombination cc = (ClusterCombination) res;

//            Check if maximal
            if (!(cc.getLHS().length == currentLSize && cc.getRHS().length == currentRSize)){
                continue;
            }

            topKCombinations.add(cc);

//        Expand both sides for topK combinations with equally-sized sides (e.g. (1,1) or (2,2))
            if (equalSideLength){
                topKCombinations.add(cc.getMirror());
            }
        }

//        Create this to avoid duplicates
        HashSet<ClusterCombination> expandedCombinations = new HashSet<>(nPeak *n);

//        Iterate over array in reverse order (descending on similarity)
        FastArrayList<RecursiveBoundingTask> tasks = new FastArrayList<>(topKCombinations.size() * n);
        for (ClusterCombination topCC: topKCombinations) {
            if (!topCC.isSingleton()){
               Logger.getGlobal().severe("Something went wrong in topK updating; found non-singleton combination");
            }

            long oldSize = topCC.size();

//            Expand topCC by adding all singleton clusters and compute their similarity exactly
            for (Cluster c: runParameters.getHC().singletonClusters){

                Cluster[] newLHS = expandLeft ? lib.add(topCC.getLHS(), c) : topCC.getLHS();
                Cluster[] newRHS = expandLeft ? topCC.getRHS() : lib.add(topCC.getRHS(), c);

                //        Check if the new CC will comply to all splitting rules
                if (!ClusterCombination.symmetryChecks(newLHS, newRHS)) continue;

//                Create new CC and sort sides in descending order (to avoid duplicates)
                ClusterCombination newCC = new ClusterCombination(newLHS, newRHS, 0, oldSize * c.size(), runParameters.isAllowVectorOverlap());

//                Skip duplicates
                if (expandedCombinations.contains(newCC)) continue;
                expandedCombinations.add(newCC);
            }
        }

//        Compute similarities of expanded combinations
        runCCs(new FastArrayList<>(expandedCombinations));

       Logger.getGlobal().info("Threshold after topK expanding: " + runParameters.getRunningThreshold().get());

//      Reset ResultDCCs
        runParameters.getResultSet().setResultObjects(oldResultDCCs);
    }
}