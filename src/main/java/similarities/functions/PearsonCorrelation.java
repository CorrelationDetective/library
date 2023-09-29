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


public class PearsonCorrelation extends MultivariateSimilarityFunction {
    public PearsonCorrelation(RunParameters runParameters) {
        super(runParameters);

        empiricalBounded = true;
        distFunc = lib::normedAngle;
        init();
    }

    @Override public double[] preprocess(double[] vector) {
        return lib.l2norm(vector);
    }
//    Angle distance

//    Cosine similarity - normalized dot product
    @Override public double sim(double[] x, double[] y) {
        return FastMath.min(FastMath.max(lib.dot(x, y),-1),1);
    }

    @Override public double simToDist(double sim) {
        return FastMath.acos(sim);
    }
    @Override public double distToSim(double dist) {return FastMath.cos(dist);}

    public ClusterBounds getBounds(ClusterCombination CC){
        boolean empiricalBounding = runParameters.isEmpiricalBounding();

        double lower;
        double upper;
        double maxLowerBoundSubset = -1;

        double nominator_lower = 0;
        double nominator_upper = 0;

        Cluster[] LHS = CC.getLHS();
        Cluster[] RHS = CC.getRHS();

        int lSize = LHS.length;
        int rSize = RHS.length;

        // nominator -> distances have negative impact on similarity
        for (int i = 0; i < lSize; i++) {
            for (int j = 0; j < rSize; j++) {
                Cluster c1 = LHS[i];
                Cluster c2 = RHS[j];

                ClusterPair cp = empiricalBounding ? empiricalDistanceBounds(c1,c2): theoreticalDistanceBounds(c1,c2);

                int c1Loc = i;
                int c2Loc = lSize + j;
                int[] location = cp.getLeft().equals(c1) ? new int[]{c1Loc, c2Loc} : new int[]{c2Loc, c1Loc};

                double[] simBounds = new double[]{this.distToSim(FastMath.min(FastMath.PI, cp.getBounds().getUB())),
                        this.distToSim(cp.getBounds().getLB())};
                nominator_lower += simBounds[0];
                nominator_upper += simBounds[1];
                maxLowerBoundSubset = FastMath.max(maxLowerBoundSubset, simBounds[0]);

                //                Add bound factor for reverse engineering of bounds
                CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(cp, false, location));
            }
        }

        //denominator: first sqrt -> distances have positive impact on similarity; bigger dot -> smaller angle -> bigger similarity
        double denominator_lower_left = lSize;
        double denominator_upper_left = lSize;

        for(int i=0; i< lSize; i++){
            for(int j=i+1; j< lSize; j++){
                Cluster c1 = LHS[i];
                Cluster c2 = LHS[j];

                ClusterPair cp =empiricalBounding ? empiricalDistanceBounds(c1,c2): theoreticalDistanceBounds(c1,c2);

                int c1Loc = i;
                int c2Loc = j;
                int[] location = cp.getLeft().equals(c1) ? new int[]{c1Loc, c2Loc} : new int[]{c2Loc, c1Loc};

                double[] simBounds = new double[]{this.distToSim(FastMath.min(FastMath.PI, cp.getBounds().getUB())),
                        this.distToSim(cp.getBounds().getLB())};
                denominator_lower_left += 2 * simBounds[0];
                denominator_upper_left += 2 * simBounds[1];
                maxLowerBoundSubset = FastMath.max(maxLowerBoundSubset, simBounds[0]);

                //                Add bound factor for reverse engineering of bounds
                CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(cp, true, location));
            }
        }

        //denominator: second sqrt -> distances have positive impact on similarity; bigger dot -> smaller angle -> bigger similarity
        double denominator_lower_right = rSize;
        double denominator_upper_right = rSize;

        for(int i=0; i< rSize; i++){
            for(int j=i+1; j< rSize; j++){
                Cluster c1 = RHS[i];
                Cluster c2 = RHS[j];

                ClusterPair cp = empiricalBounding ? empiricalDistanceBounds(c1,c2): theoreticalDistanceBounds(c1,c2);

                int c1Loc = lSize + i;
                int c2Loc = lSize + j;
                int[] location = cp.getLeft().equals(c1) ? new int[]{c1Loc, c2Loc} : new int[]{c2Loc, c1Loc};

                double[] simBounds = new double[]{this.distToSim(FastMath.min(Math.PI, cp.getBounds().getUB())),
                        this.distToSim(cp.getBounds().getLB())};
                denominator_lower_right += 2 * simBounds[0];
                denominator_upper_right += 2 * simBounds[1];
                maxLowerBoundSubset = Math.max(maxLowerBoundSubset, simBounds[0]);

                //                Add bound factor for reverse engineering of bounds
                CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(cp, true, location));
            }
        }

        //denominator: whole. note that if bounds are too loose we could get a non-positive value, while this is not possible due to Pos. Def. of variance.
        double denominator_lower = Math.sqrt(Math.max(denominator_lower_left, 1e-7)*Math.max(denominator_lower_right, 1e-7));
        double denominator_upper = Math.sqrt(Math.max(denominator_upper_left, 1e-7)*Math.max(denominator_upper_right, 1e-7));

        //case distinction for final bound
        if (nominator_lower >= 0) {
            lower = nominator_lower / denominator_upper;
            upper = nominator_upper / denominator_lower;
        } else if (nominator_lower < 0 && nominator_upper >= 0) {
            lower = nominator_lower / denominator_lower;
            upper = nominator_upper / denominator_lower;
        } else if (nominator_upper < 0) {
            lower = nominator_lower / denominator_lower;
            upper = nominator_upper / denominator_upper;
        } else {
            lower = -1000;
            upper = 1000;
        }

        return new ClusterBounds(lower, upper, maxLowerBoundSubset);
    }

    //    Compute empirical similarity directly, based on a list of extrema distances, partitioned by their impact on the similarity
    public double empiricalSimilarity(FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors, int pLeft, int pRight,
                                      double[][] pairwiseDistances){
//        Numerator elements
        double num = 0;

//        Denominator elements
        int nWithinLeft = pLeft * (pLeft - 1) / 2;
        int positiveCounter = 0;
        double denomLeft = pLeft;
        double denomRight = pRight;

//        Iterate over all bound factors and recompose all bound elements (numerator, withinLeft and withinRight denoms)
        for (EmpiricalBoundFactor ebf: empiricalBoundFactors) {
            if (ebf.isPositiveImpact()){ // denom
                if (positiveCounter < nWithinLeft){ // first add to left, then right
                    denomLeft += 2 * distToSim(ebf.getExtremaDistance(pairwiseDistances));
                } else {
                    denomRight += 2 * distToSim(ebf.getExtremaDistance(pairwiseDistances));
                }
                positiveCounter++;
            } else { // num
                num += distToSim(ebf.getExtremaDistance(pairwiseDistances));
            }
        }

//        Compute similarity
        double denom = Math.sqrt(denomLeft) * Math.sqrt(denomRight);
        return num / denom;
    }

//    Empirical bounds
    @Override public ClusterBounds empiricalSimilarityBounds(ClusterCombination CC) {
        return getBounds(CC);
    }

//    Theoretical bounds
    @Override public ClusterBounds theoreticalSimilarityBounds(ClusterCombination CC){
        return getBounds(CC);
    }
}
