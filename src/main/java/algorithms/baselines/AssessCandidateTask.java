package algorithms.baselines;

import _aux.Pair;
import _aux.lib;
import _aux.lists.FastArrayList;
import core.RunParameters;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.util.FastMath;
import queries.ProgressiveStopException;
import queries.ResultTuple;

import java.util.Arrays;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.function.Function;

@RequiredArgsConstructor
public class AssessCandidateTask extends RecursiveAction {
    @NonNull int[] LHS;
    @NonNull int[] RHS;
    @NonNull double maxSubsetSimilarity;
    @NonNull Function<Pair<int[], int[]>, Double> similarityComputer;
    @NonNull RunParameters runParameters;

    @Override
    public void compute(){
//        Assess this candidate
        boolean isResult;
        try {
            isResult = assessCandidate(LHS,RHS);
        } catch (ProgressiveStopException e){
            throw new RuntimeException(e);
        }

        int leftSize = LHS.length;
        int rightSize = RHS.length;
        int p = leftSize + rightSize;

//        Expand candidate if necessary
//        Expand candidate if not at max P and necessary (considering potential irreducibility constraint)
        if (p < runParameters.getMaxPLeft() + runParameters.getMaxPRight() && !(runParameters.isIrreducibility() && isResult)){
            // Expand candidate
            boolean expandLeft = rightSize == runParameters.getMaxPRight() || (leftSize == rightSize && leftSize < runParameters.getMaxPLeft());

            // Expand left
            FastArrayList<AssessCandidateTask> newTasks = new FastArrayList<>(runParameters.getNVectors());
            if (expandLeft){
                for (int i = LHS[leftSize-1] + 1; i < runParameters.getNVectors(); i++) {
                    if (lib.contains(RHS,i)) continue;

                    int[] newLHS = Arrays.copyOf(LHS, LHS.length + 1);
                    newLHS[LHS.length] = i;
                    newTasks.add(new AssessCandidateTask(newLHS, RHS, maxSubsetSimilarity, similarityComputer, runParameters));
                }
            } else {
                for (int i = RHS[rightSize-1] + 1; i < runParameters.getNVectors(); i++) {
                    if (lib.contains(LHS,i)) continue;

                    int[] newRHS = Arrays.copyOf(RHS, RHS.length + 1);
                    newRHS[RHS.length] = i;
                    newTasks.add(new AssessCandidateTask(LHS, newRHS, maxSubsetSimilarity, similarityComputer, runParameters));
                }
            }

//            Invoke all new tasks
            ForkJoinTask.invokeAll(newTasks.toList());
        }
    }

    public boolean assessCandidate(int[] LHS, int[] RHS) throws ProgressiveStopException {
        // Compute similarity of this candidate
        double sim = similarityComputer.apply(new Pair<>(LHS, RHS));

        // Add candidate to result dependent of similarity and query parameters
        double threshold = runParameters.getTau();

        //  Increase for minJump
        if (LHS.length + RHS.length > 2){
            double jumpBasedThreshold = maxSubsetSimilarity + runParameters.getMinJump();
            threshold = FastMath.max(threshold, jumpBasedThreshold);
        }
        maxSubsetSimilarity = FastMath.max(maxSubsetSimilarity, sim);

        //        Check if above threshold, if so add to results
        boolean isResult = sim >= threshold;
        if (isResult) {
            synchronized (runParameters.getResultSet()){
                runParameters.getResultSet().add(new ResultTuple(LHS, RHS, sim));
            }
        }
        return isResult;
    }
}
