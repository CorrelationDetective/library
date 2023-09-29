package similarities.functions;

import _aux.lib;
import _aux.lists.FastArrayList;
import bounding.ClusterBounds;
import bounding.ClusterCombination;
import bounding.ClusterPair;
import bounding.EmpiricalBoundFactor;
import clustering.Cluster;
import core.RunParameters;
import org.apache.commons.math3.util.FastMath;
import similarities.MultivariateSimilarityFunction;

import java.util.*;
import java.util.stream.IntStream;

public class TotalCorrelation extends MultivariateSimilarityFunction {

    private static final int bins = 10;
    private double[][] pairwiseEntropies;

    public TotalCorrelation(RunParameters runParameters) {
        super(runParameters);

        empiricalBounded = true;
        twoSided = false;

        distFunc = lib::euclidean;
//        distFunc = (a,b) -> 1 / (mutualInformation(a,b) + 1e-10);

        MAX_SIMILARITY = 3*lib.log2(bins);
        MIN_SIMILARITY = 0;
        SIMRANGE = MAX_SIMILARITY - MIN_SIMILARITY;
        init();
    }

    @Override
    public double getMaxApproximationSize(double ratio){
        return 6;
    }


    public static double[] discretize(double[] in){
        double[] out = new double[in.length];
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

//        Find min and max (edges of bin linspace)
        for (double v : in) {
            min = FastMath.min(min, v);
            max = FastMath.max(max, v);
        }

//        Create linspace between min and max and map values to it
        double xrange = (max - min);
        for (int i = 0; i < in.length; i++) {
            out[i] = FastMath.floor((in[i] - min) / xrange * bins);
            if (out[i] == bins) out[i] = bins - 1;
        }
        return out;
    }

    public static double entropy(double[] in){
        double[] hist = new double[bins];
        for (double v : in) {
            hist[(int) v]++;
        }
        double out = 0;
        for (double v : hist) {
            if (v > 0) {
                double p = v / in.length;
                out += p * FastMath.log(p);
            }
        }
        return -out;
    }

    public static double jointEntropy(double[] a, double[] b){
        double[] hist = new double[bins*bins];
        for (int i = 0; i < a.length; i++) {
            hist[(int) (a[i] * bins + b[i])]++;
        }

        double out = 0;
        for (int i=0; i<hist.length; i++) {
            if (hist[i] > 0) {
                double p = hist[i] / a.length;
                out += p * FastMath.log(p);
            }
        }
        return -out;
    }

    public static double mutualInformation(double[] a, double[] b){
        return entropy(a) + entropy(b) - jointEntropy(a,b);
    }

//    Compute joint entropy over full set of points -- M being a row-wise matrix of points
//    H(A,B,C) = H(A) + H(B|A) + H(C|A,B) = H(A) + H(A,B) - H(A) + H(A,B,C) - H(A,B)
    public static double jointEntropy(double[][] M){
        Map<Integer, Integer> hist = new HashMap<>((int) FastMath.pow(bins, M.length));
        int n = M.length;
        int m = M[0].length;

//        Compute histogram
        for (int j = 0; j < m; j++) {
            int key = 1;
            for (int i = 0; i < n; i++) {
                key = bins * key + (int) M[i][j];
            }
            hist.put(key, hist.getOrDefault(key, 0) + 1);
        }

//        Compute entropy
        double out = 0;
        for (int v : hist.values()) {
            double p = v / (double) m;
            out += p * FastMath.log(p);
        }

        return -out;
    }

    @Override public double[] preprocess(double[] vector) {
        return discretize(vector);
    }

    @Override public double sim(double[] x, double[] y) {
        return entropy(x) + entropy(y) - jointEntropy(x, y);
    }

    @Override public double simToDist(double sim) {
        return 100;
    }
    @Override public double distToSim(double dist) {

        return 1;
    }

    public static double totalCorrelation(Cluster[] clusters, double[][] pairwiseEntropies){
        double TC = 0;
//            Compute sum of entropies
        double[][] M = new double[clusters.length][clusters[0].getCentroid().length];
        for (int i = 0; i < clusters.length; i++) {
            Cluster c = clusters[i];
            double[] x = c.getCentroid();
            if (pairwiseEntropies == null){
                TC += entropy(x);
            } else {
                TC += pairwiseEntropies[c.centroidIdx][c.centroidIdx];
            }
            M[i] = x;
        }

//            Compute joint entropy
        double HC = jointEntropy(M);
        TC -= HC;
        return TC;
    }


    //    Pairwise distances in this case are the conditional entropies of the two variables
    @Override public double[][] computePairwiseDistances(double[][] data) {
        int n = data.length;
        pairwiseEntropies = new double[n][n];
        boolean parallel = runParameters.isParallel();

//        First get all the single entropies
        for (int i = 0; i < n; i++) {
            pairwiseEntropies[i][i] = entropy(data[i]);
        }

//        Then compute the conditional entropies
        lib.getStream(IntStream.range(0, n).boxed(), parallel).forEach(i -> {
            lib.getStream(IntStream.range(i+1, n).boxed(), parallel).forEach(j -> {
                pairwiseEntropies[i][j] = pairwiseEntropies[j][i] = jointEntropy(data[i], data[j]);
            });
        });

//        Lastly compute pairwiseDistances like any other metric (with euclidean distance in this case)
        return super.computePairwiseDistances(data);
    }

    @Override
    protected double getDistanceForEmpiricalDistanceBounds(int i, int j){
        return pairwiseEntropies[i][j];
    }

    @Override
    public ClusterBounds empiricalSimilarityBounds(ClusterCombination CC) {
//        Mind that we do not have a RHS
//        LB = sum(lb_entropy(LHS)) - sum_ordered_i_to_p-1(ub_cEntropy(LHS[i], LHS[i+1]))
//        UB = sum(ub_entropy(LHS)) - max(lb_entropy(LHS))

        Cluster[] LHS = CC.getLHS();
        int p = LHS.length;

//        Exact computation if only singleton clusters
        if (Arrays.stream(LHS).noneMatch(c -> c.size() > 1)){
            double TC = totalCorrelation(LHS, pairwiseEntropies);
            return new ClusterBounds(TC,TC,0);
        }

        double lb = 0;
        double ub = 0;
//        TODO IMPLEMENT
        double maxLowerBoundSubset = 0;

//        Compute/get joint entropy bounds
        double[][] jointEntropyUBs = new double[p][p];
        double maxJointEntropyLB = 0;
        ClusterPair maxJointEntropyLBPair = null;
        for (int i = 0; i < p; i++) {
            for (int j = i; j < p; j++) {
                ClusterBounds entropyBounds;
                if (i==j){ // individual entropy
                    ClusterPair entropyClusterPair = LHS[i].getEntropyBounds(pairwiseEntropies, runParameters.isAllowVectorOverlap());
                    entropyBounds = entropyClusterPair.getBounds();
                    lb += entropyBounds.getLB();
                    ub += entropyBounds.getUB();
                    //  Add bound factor for reverse engineering of bounds
                    CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(entropyClusterPair, true, new int[]{i}));
                } else { // joint entropy
                    Cluster c1 = LHS[i];
                    Cluster c2 = LHS[j];

                    ClusterPair cp = empiricalDistanceBounds(c1,c2);
                    entropyBounds = cp.getBounds();

                    if (entropyBounds.getLB() > maxJointEntropyLB){
                        maxJointEntropyLB = entropyBounds.getLB();
                        maxJointEntropyLBPair = cp;
                    }
                }
                jointEntropyUBs[i][j] = jointEntropyUBs[j][i] = entropyBounds.getUB();
            }
        }

//        Add bound factor for reverse engineering of bounds
        int[] location = new int[]{lib.indexOf(LHS, maxJointEntropyLBPair.getLeft()), lib.indexOf(LHS, maxJointEntropyLBPair.getRight())};
        CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(maxJointEntropyLBPair, false, location));

//        compute LB
        lb -= getJointEntropyUB(jointEntropyUBs);
        ub -= maxJointEntropyLB;

        return new ClusterBounds(lb, ub, maxLowerBoundSubset);
    }

    public double empiricalSimilarity(FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors, int pLeft, int pRight,
                                      double[][] pairwiseDistances){
        double ub = 0;
        for (EmpiricalBoundFactor ebf : empiricalBoundFactors) {
            if (ebf.isPositiveImpact()){
                ub += ebf.getExtremaDistance(pairwiseEntropies);
            } else {
                ub -= ebf.getExtremaDistance(pairwiseEntropies);
            }
        }
        return ub;
    }

    public double getJointEntropyUB(double[][] jointEntropyUBs){
//        Joint entropy UB = sum_i_to_p(lb_entropy(LHS[i]) - min_permutation sum_i_to_p-1(ub_cEntropy(LHS[i], LHS[i+1]))

        int p = jointEntropyUBs.length;

//        First compute conditional entropies H(I|J) = H(I,J) - H(J), and write to flat array
        double[] conditionalEntropiesFlat = new double[p*p];
        double[][] conditionalEntropies = new double[p][p];
        for (int i = 0; i < p; i++) {
            for (int j = 0; j < p; j++) {
                double ce = i==j ? Double.MAX_VALUE : Math.max(0,jointEntropyUBs[i][j] - jointEntropyUBs[j][j]);
                conditionalEntropies[i][j] = ce;
                conditionalEntropiesFlat[i*p + j] = ce;
            }
        }

//        Argsort the conditional entropies (ascending)
        int[] argSortedConditionalEntropiesFlat = IntStream.range(0, conditionalEntropiesFlat.length).boxed()
                .sorted(Comparator.comparingDouble(i -> conditionalEntropiesFlat[i])).mapToInt(ele -> ele).toArray();

        double jointEntropyUB = 0;

//        Iterate over sorted cEntropies and create ordering
        boolean[] used = new boolean[p];
        int nUsed = 0;
        for (int i = 0; i < argSortedConditionalEntropiesFlat.length; i++) {
            if (nUsed == p) break;

            int idx = argSortedConditionalEntropiesFlat[i];
            int i1 = idx / p;
            int i2 = idx % p;
            if (used[i1] || used[i2]) {
                continue;
            }
            used[i1] = true;
            nUsed++;
            jointEntropyUB += i1 == i2 ? jointEntropyUBs[i1][i1]: conditionalEntropies[i1][i2];
        }

        return jointEntropyUB;
    }

    //    Theoretical bounds -- not implemented for this similarity measure
    @Override public ClusterBounds theoreticalSimilarityBounds(ClusterCombination CC){
        return empiricalSimilarityBounds(CC);
    }



}
