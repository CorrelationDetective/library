package bounding;

import _aux.Pair;
import _aux.lists.FastArrayList;
import clustering.Cluster;
import clustering.HierarchicalClustering;
import core.RunParameters;
import core.StatBag;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import similarities.MultivariateSimilarityFunction;

import java.util.*;

@RequiredArgsConstructor
public class BoundDiscounting {
    @NonNull RunParameters runParameters;

    //    Data structure for discounting, array of nBins bins, each bin is a hashMap with key = point id 1, value = list of point id 2 (always sorted lexicographically)
    @Getter public static long nDiscountCuts = 0;

//    Check if CC is eligible for discounting
    public boolean checkDiscounting(ClusterCombination CC){
        if (runParameters.isDiscounting() &&
                runParameters.getSimMetric().allowsDiscounting() &&
                !CC.isDiscounted() &&
                CC.getLHS().length + CC.getRHS().length > 2 &&
                CC.getCriticalShrinkFactor() > runParameters.getDiscountThreshold()){

//            Discount bounds
            boolean success = StatBag.timeit(() -> discountBounds(CC), runParameters.getStatBag().getDiscountingTime());

//            Update stats
            runParameters.getStatBag().incrementStat(runParameters.getStatBag().getNDiscountedCCs());
            return success;
        }
        return false;
    }



    /**
     * Get the empiricalBoundFactor that will decrease the similarity the most,
     * while limiting the number of combinations that are ignored (i.e., pareto-optimal)
     * @param empiricalBoundFactors the empirical bound factors to consider.
     * @return Pair object containing the empirical bound factor and the similarity that would be achieved if this factor was re-ranked.
     */
    public Pair<EmpiricalBoundFactor, Double> optimalDiscountFactor(FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors,
                                                                           double currSim, int pL, int pR){
        int discountStep = runParameters.getDiscountStep();
        MultivariateSimilarityFunction simMetric = runParameters.getSimMetric();

        // Get bound factor with maximum weight
        double maxWeightSim = currSim;
        double maxWeight = Double.NEGATIVE_INFINITY;
        EmpiricalBoundFactor maxFactor = empiricalBoundFactors.get(0);

        for (EmpiricalBoundFactor factor : empiricalBoundFactors) {
//                Skip factors which are already at max rank
            if (factor.isMaxRank(runParameters.getDiscountStep())) continue;

//                Check what bound would be if this factor was re-ranked
            factor.addToRank(discountStep);
            double newSim = simMetric.empiricalSimilarity(empiricalBoundFactors, pL, pR, runParameters.getPairwiseDistances());
            factor.decrementRank(discountStep);

//                Compute weight of factor (weight = expected decrease in similarity / expected number of ignored combinations)
            double delta = (currSim - newSim);
            double weight = delta * delta * factor.getClusterPair().size();

//                Update max weight if necessary
            if (weight > maxWeight){
                maxWeight = weight;
                maxFactor = factor;
                maxWeightSim = newSim;
            }
        }
        return new Pair<>(maxFactor, maxWeightSim);
    }

    // Check cut materializations in standard recursively bounded fashion
    public void checkCutCombs(int[] ids, int[] locations, ClusterCombination CC) {
        boolean isPair = ids.length == 2;
        HierarchicalClustering HC = runParameters.getHC();

        int newSize = 0;
        Cluster[] allClusters = CC.getClusters();
        Cluster[] newClusters = Arrays.copyOf(allClusters, allClusters.length);

        if (isPair) {
            Cluster c1 = HC.singletonClusters[ids[0]];
            Cluster c2 = HC.singletonClusters[ids[1]];
            newClusters[locations[0]] = c1;
            newClusters[locations[1]] = c2;
            newSize = (int) CC.size() / allClusters[locations[0]].size() / allClusters[locations[1]].size();
        } else {
            Cluster c1 = HC.singletonClusters[ids[0]];
            newClusters[locations[0]] = c1;
            newSize = (int) CC.size() / allClusters[locations[0]].size();
        }
        Cluster[] newLHS = Arrays.copyOfRange(newClusters, 0, CC.getLHS().length);
        Cluster[] newRHS = Arrays.copyOfRange(newClusters, CC.getLHS().length, newClusters.length);

//        Check if the new CC will comply to all splitting rules
        if (!ClusterCombination.symmetryChecks(newLHS, newRHS)) return;

//          Make new CC
        ClusterCombination canCC = new ClusterCombination(newLHS, newRHS, CC.getLevel() + 1, newSize, runParameters.isAllowVectorOverlap());

//      Make sure that the canCC does not get discounted itself
        canCC.setDiscounted(true);

//        Put CC in check queue
        RecursiveBoundingTask task = new RecursiveBoundingTask(canCC, runParameters);
        if (runParameters.isParallel()){
            runParameters.getForkJoinPool().execute(task);
        } else {
            task.compute();
        }

        runParameters.getStatBag().incrementStat(runParameters.getStatBag().getNDiscountCuts());
    }

    public boolean discountBounds(ClusterCombination CC){

        /* Greedy discounting algorithm that discounts the bounds of a CC until they are decisive.
         * Returns: the decisive (sub) CCs in this CC.
         */

//        Can only discount already bounded CCs
        if (!CC.isBounded()) {
            throw new RuntimeException("Cannot discount bounds of unbounded CC " + CC);
        }

//        Unnecessary to discount positive CCs
        if (CC.getSimilarity() >= runParameters.getTau() | CC.isPositive()) {
            return false;
        }

        FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors = CC.getEmpiricalBoundFactors();

//        If there are no empirical bound factors, return
        if (empiricalBoundFactors == null) {
            throw new RuntimeException("No empirical bound factors for CC " + CC);
        }

        CC.setDiscounted(true);

//        Running similarity
        int pL = CC.getLHS().length;
        int pR = CC.getRHS().length;
        double sim = runParameters.getSimMetric().empiricalSimilarity(empiricalBoundFactors, pL, pR, runParameters.getPairwiseDistances());

//      Greedily traverse rank configurations until bound is decisive
        while (sim > runParameters.getTau()){
//            Get optimal discount factor
            Pair<EmpiricalBoundFactor, Double> pair = optimalDiscountFactor(empiricalBoundFactors, sim, pL, pR);
            EmpiricalBoundFactor maxFactor = pair.getX();
            double maxWeightSim = pair.getY();

//            Stop if we would increase in similarity (i.e., no factor is worth discounting)
            if (maxWeightSim >= sim) return false;

//            Exhaustively check the cut materializations
            for (int i = 0; i < runParameters.getDiscountStep(); i++) {
                int[] extremaPair = maxFactor.getExtremaPair();
                int[] locations = maxFactor.getLocations();

                checkCutCombs(extremaPair, locations, CC);

//             If cp overlaps, also check mirror
                ClusterPair cp = maxFactor.getClusterPair();
                if (cp.isPair() && cp.getLeft().contains(extremaPair[1]) && cp.getRight().contains(extremaPair[0])){
                    int[] locationsMirror = new int[]{locations[1], locations[0]};
                    checkCutCombs(extremaPair, locationsMirror, CC);
                }

//            Stop if all factors have been exhausted
                if (maxFactor.isMaxRank(runParameters.getDiscountStep())) return false;

                //  Update rank of factor with maximum weight
                maxFactor.incrementRank();
            }

            //  Update sim to new sim
            sim = maxWeightSim;
        }

//        Only set discounted if we have actually discounted, otherwise we allow discounting of subCCs still.
//        CC.setDiscounted(true);

        return true;
    }


}
