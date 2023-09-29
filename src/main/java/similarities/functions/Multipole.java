package similarities.functions;

import Jama.Matrix;
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


public class Multipole extends MultivariateSimilarityFunction {
    public Multipole(RunParameters runParameters) {
        super(runParameters);

        empiricalBounded = true;
        twoSided = false;
        distFunc = lib::normedAngle;
        init();
    }
    @Override public boolean isTwoSided() {return false;}
    @Override public double[] preprocess(double[] vector) {
        return lib.l2norm(vector);
    }

    @Override public double sim(double[] x, double[] y) {
        return FastMath.min(FastMath.max(lib.dot(x, y), -1),1);
    }

    @Override public double simToDist(double sim) {
        return FastMath.acos(sim);
    }
    @Override public double distToSim(double dist) {return FastMath.cos(dist);}

    @Override public ClusterBounds empiricalSimilarityBounds(ClusterCombination CC) {
        return getBounds(CC);
    }

    //    Theoretical bounds
    @Override public ClusterBounds theoreticalSimilarityBounds(ClusterCombination CC) {
        return getBounds(CC);
    }

    private double[] distancesToBounds(double[][] lowerBounds, double[][] upperBounds){
        // Calculate bounds on multipoles as described in Section: Application to Multipoles
        // Use jama for linear algebra. possible alternative: OjAlgo backend
        Matrix upperPairwise2 = new Matrix(upperBounds);
        Matrix lowerPairwise2 = new Matrix(lowerBounds);

        Matrix estimateMat2 = upperPairwise2.plus(lowerPairwise2).times(0.5);
        Matrix slackMat2 = upperPairwise2.minus(lowerPairwise2);

        double[] eigenVals2 = estimateMat2.eig().getRealEigenvalues();
        double slack2 = slackMat2.norm2();

        double smallestEig = 1;
        for(double e : eigenVals2){
            if(e < smallestEig){
                smallestEig = e;
            }
        }

        double lower = 1 - (smallestEig + 0.5 * slack2);
        double upper = 1 - (smallestEig - 0.5 * slack2);
        return new double[]{lower, upper};
    }

    public ClusterBounds getBounds(ClusterCombination CC){
        double lower;
        double upper;

        Cluster[] LHS = CC.getLHS();
        Cluster[] RHS = CC.getRHS();

        if (RHS.length > 0){
            throw new IllegalArgumentException("RHS must be empty for one-sided bounds");
        }

        double[][] lowerBoundsArray = new double[LHS.length][LHS.length];
        double[][] upperBoundsArray = new double[LHS.length][LHS.length];
        double highestAbsLowerBound = -1;

        // create upper and lower bound matrices U and L as described in paper
        for(int i=0; i< LHS.length; i++) {
            // we can fill the diagonal with 1's since we always pick one vector from each cluster
            lowerBoundsArray[i][i] = 1;
            upperBoundsArray[i][i] = 1;
            Cluster c1 = LHS[i];
            for (int j = i + 1; j < LHS.length; j++) {
                Cluster c2 = LHS[j];
                ClusterPair cp = runParameters.isEmpiricalBounding() ? empiricalDistanceBounds(c1, c2) :
                        theoreticalDistanceBounds(c1, c2);

                int[] location = cp.getLeft().equals(c1) ? new int[]{i,j} : new int[]{j,i};

                double[] simBounds = new double[]{this.distToSim(FastMath.min(FastMath.PI, cp.getBounds().getUB())),
                        this.distToSim(cp.getBounds().getLB())};

                if (simBounds[0] > 0) {
                    highestAbsLowerBound = FastMath.max(highestAbsLowerBound, simBounds[0]); // smaller angle = higher similarity
                } else if (simBounds[1] < 0) {
                    highestAbsLowerBound = Math.max(highestAbsLowerBound, Math.abs(simBounds[1]));
                }

                lowerBoundsArray[i][j] = simBounds[0];
                lowerBoundsArray[j][i] = simBounds[0];

                upperBoundsArray[i][j] = simBounds[1];
                upperBoundsArray[j][i] = simBounds[1];

                //                Add bound factor for reverse engineering of bounds (negativeImpact -> UB, positiveImpact -> LB)
                CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(cp, false, location));
                CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(cp, true, location));
            }
        }

        double[] bounds = distancesToBounds(lowerBoundsArray, upperBoundsArray);
        return new ClusterBounds(correctBound(bounds[0]), correctBound(bounds[1]), highestAbsLowerBound);
    }

    public double empiricalSimilarity(FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors, int pLeft, int pRight,
                                      double[][] pairwiseDistances){
//        Collect upper and lower bound sims
        int nPairs = pLeft * (pLeft - 1) / 2;
        double[] lbSims = new double[nPairs];
        double[] ubSims = new double[nPairs];
        int lbCounter = 0;
        int ubCounter = 0;
        for (EmpiricalBoundFactor ebf : empiricalBoundFactors){
            try{
                if (ebf.isPositiveImpact()){
                    lbSims[lbCounter++] = distToSim(ebf.getExtremaDistance(pairwiseDistances));
                } else {
                    ubSims[ubCounter++] = distToSim(ebf.getExtremaDistance(pairwiseDistances));
                }
            } catch (ArrayIndexOutOfBoundsException e){
                throw new RuntimeException("Too many bound factors for reconstructing the Multipole bounds");
            }
        }

        if (lbCounter != ubCounter){
            throw new RuntimeException("Number of lower and upper bound factors do not match");
        }
        if (lbCounter != nPairs){
            throw new RuntimeException("Number of bound factors does not match the number of pairs");
        }

//        Recompose distance matrix
        double[][] lbSimArray = new double[pLeft][pLeft];
        double[][] ubSimArray = new double[pLeft][pLeft];

//        Fill matrices with sims
        int c = 0;
        for (int i = 0; i < pLeft; i++) {
//            Fill diagonal with 1's
            lbSimArray[i][i] = 1;
            ubSimArray[i][i] = 1;
            for (int j = i+1; j < pLeft; j++) {
                lbSimArray[i][j] = lbSimArray[j][i] = lbSims[c];
                ubSimArray[i][j] = ubSimArray[j][i] = ubSims[c];
                c++;
            }
        }
        double[] bounds = distancesToBounds(lbSimArray, ubSimArray);
        return bounds[1];
    }

}
