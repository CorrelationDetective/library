package bounding;

import _aux.lists.FastArrayList;
import core.RunParameters;
import core.StatBag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.util.FastMath;
import queries.ProgressiveStopException;
import queries.QueryTypeEnum;
import queries.ResultObject;
import queries.RunningThreshold;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RecursiveBoundingTask extends RecursiveAction {
    @NonNull private final ClusterCombination CC;
    @NonNull private final RunParameters runParameters;

    @Override
    protected void compute() {
        try {
            assessCC(CC);
        } catch (ProgressiveStopException e) {
            throw new RuntimeException(e);
        }


        if (!CC.isDecisive()) {
//            Discount bounds, if number of ignored points are too large, still split
            if (runParameters.getBD().checkDiscounting(CC)) return;

//            Create new tasks of the split CCs
//            FastArrayList<ClusterCombination> subCCs = StatBag.timeit(CC::split, statBag.splittingTime);
            FastArrayList<ClusterCombination> subCCs = CC.split();
            List<RecursiveBoundingTask> tasks = subCCs.stream().map(cc -> new RecursiveBoundingTask(cc, runParameters)).collect(Collectors.toList());

//            If task is sufficiently small, run sequentially, otherwise fork
            if (runParameters.isParallel() && CC.size() > 20) {
                runParameters.getStatBag().addStat(runParameters.getStatBag().getNParallelCCs(), subCCs::size);
                ForkJoinTask.invokeAll(tasks);
            } else {
                runParameters.getStatBag().addStat(runParameters.getStatBag().getNSecCCs(), subCCs::size);
                for (RecursiveBoundingTask task : tasks) {
                    task.compute();
                }
            }
        }
    }

//    Returned list is the list of new candidates -- in case this candidate is not decisive.
    private void assessCC(ClusterCombination canCC) throws ProgressiveStopException {
        int p = canCC.getLHS().length + canCC.getRHS().length;
        StatBag statBag = runParameters.getStatBag();
        RunningThreshold runningThreshold = runParameters.getRunningThreshold();
        double minJump = runParameters.getMinJump();
        boolean irreducibility = runParameters.isIrreducibility();
        double shrinkFactor = runParameters.getShrinkFactor();
        double BFSFactor = runParameters.getBFSFactor();
        QueryTypeEnum queryType = runParameters.getQueryType();

        //        Compute/get bounds
        runParameters.getSimMetric().bound(canCC);

        //      Update statistics
        statBag.incrementStat(statBag.getNCCs());
        statBag.addStat(statBag.getTotalCCSize(), () -> (int) canCC.size());

        double threshold = runningThreshold.get();

        //        Update threshold based on minJump and irreducibility if we have canCC > 2
        if (p > 2){
            if (minJump > 0){
                threshold = FastMath.max(threshold, canCC.bounds.getMaxLowerBoundSubset() + minJump);
            }
            if (irreducibility && canCC.bounds.getMaxLowerBoundSubset() >= threshold){
                threshold = Double.MAX_VALUE;
            }
        }

        //        Shrink upper bound for progressive bounding
        double shrunkUB = queryType.equals(QueryTypeEnum.TOPK) ? canCC.getShrunkUB(shrinkFactor, BFSFactor): canCC.getBounds().getUB();
        canCC.setCriticalShrinkFactor(threshold);

//        Check if canCC is (in)decisive
        if ((canCC.bounds.getLB() < threshold) && (shrunkUB > threshold)){
            canCC.setDecisive(false);
        } else { // canCC is decisive, add to DCCs
            canCC.setDecisive(true);

//            Negative DCC, postpone for later if actual UB is above threshold (actually indecisive)
            if (shrunkUB < threshold) {
                if (canCC.bounds.getUB() > threshold) {
                    PriorityQueue<ClusterCombination> postponedDCCs = runParameters.getPostponedDCCs();
                    synchronized (postponedDCCs) {
                        postponedDCCs.add(canCC);
                    }
                }
            //  Positive DCC
            } else if (canCC.bounds.getLB() >= threshold){
                canCC.setPositive(true);

                FastArrayList<ResultObject> newPositives = canCC.unpackAndCheckConstraints(runParameters);
                if (!newPositives.isEmpty()) {
                    runParameters.getResultSet().addAll(newPositives);
                }
            }
        }
    }


}
