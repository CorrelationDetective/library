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

public class EuclideanSimilarity extends MultivariateSimilarityFunction {
    public EuclideanSimilarity(RunParameters runParameters) {
        super(runParameters);

        empiricalBounded = true;
        distFunc = lib::normedAngle;
//        distFunc = lib::euclidean;
        MAX_SIMILARITY = 1;
        MIN_SIMILARITY = 0;
        SIMRANGE = MAX_SIMILARITY - MIN_SIMILARITY;

        init();
    }
    @Override public double[] preprocess(double[] vector) {
//        return vector;
        return lib.l2norm(vector);
    }

    @Override public double sim(double[] x, double[] y) {
        return 1 / (1 + FastMath.sqrt(2 - 2*FastMath.cos(this.distFunc.dist(x, y))));
//        return 1 / (1 + this.distFunc.dist(x, y));
    }

    @Override public double simToDist(double sim) {
//        if (sim == 0) return Double.MAX_VALUE;
        double d = 1 / (sim) - 1;
////        d2 to dot
        return FastMath.acos(1 - ((d*d) / 2));
//        return 1 / sim - 1;
    }

    @Override public double distToSim(double dist) {
        return 1 / (1 + FastMath.sqrt(2 - 2*FastMath.cos(dist)));
//        return 1 / (1 + dist);
    }

    private double euclToSim(double dist){
        return 1 / (1 + dist);
    }

    private double distToDot(double dist){
//        return 1 - ((dist*dist) / 2);
        return FastMath.cos(dist);
    }

    public ClusterBounds getBounds(ClusterCombination CC){
        boolean empiricalBounding = runParameters.isEmpiricalBounding();

        double betweenLowerDot = 0;
        double betweenUpperDot = 0;

        double withinLowerDot = 0;
        double withinUpperDot = 0;

        double maxLowerBoundSubset = this.MIN_SIMILARITY;

        Cluster[] LHS = CC.getLHS();
        Cluster[] RHS = CC.getRHS();

//        Get all pairwise between cluster distances
        for (int i = 0; i < LHS.length; i++) {
            for (int j = 0; j < RHS.length; j++) {
                Cluster c1 = LHS[i];
                Cluster c2 = RHS[j];
                ClusterPair cp = empiricalBounding ? empiricalDistanceBounds(c1,c2): theoreticalDistanceBounds(c1,c2);

                int c1Loc = i;
                int c2Loc = LHS.length + j;
                int[] location = cp.getLeft().equals(c1) ? new int[]{c1Loc, c2Loc} : new int[]{c2Loc, c1Loc};

                double dot0 = distToDot(cp.getBounds().getLB());
                double dot1 = distToDot(cp.getBounds().getUB());
                betweenLowerDot += 2 * FastMath.min(dot0,dot1);
                betweenUpperDot += 2 * FastMath.max(dot0,dot1);
                maxLowerBoundSubset = FastMath.max(maxLowerBoundSubset, distToSim(cp.getBounds().getUB()));

//                Add bound factor for reverse engineering of bounds
                CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(cp, false, location));
            }
        }


//        Get all pairwise within cluster (side) distances LHS
        for (int i = 0; i < LHS.length; i++) {
            for (int j = i+1; j < LHS.length; j++) {
                Cluster c1 = LHS[i];
                Cluster c2 = LHS[j];
                ClusterPair cp = empiricalBounding ? empiricalDistanceBounds(c1,c2): theoreticalDistanceBounds(c1,c2);

                int c1Loc = i;
                int c2Loc = j;
                int[] location = cp.getLeft().equals(c1) ? new int[]{c1Loc, c2Loc} : new int[]{c2Loc, c1Loc};

                double dot0 = distToDot(cp.getBounds().getLB());
                double dot1 = distToDot(cp.getBounds().getUB());
                withinLowerDot += 2 * FastMath.min(dot0,dot1);
                withinUpperDot += 2 * FastMath.max(dot0,dot1);
                maxLowerBoundSubset = FastMath.max(maxLowerBoundSubset, distToSim(cp.getBounds().getUB()));

                //                Add bound factor for reverse engineering of bounds
                CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(cp, true, location));
            }
        }

        //        Get all pairwise within cluster (side) distances RHS
        for (int i = 0; i < RHS.length; i++) {
            for (int j = i+1; j < RHS.length; j++) {
                Cluster c1 = RHS[i];
                Cluster c2 = RHS[j];
                ClusterPair cp = empiricalBounding ? empiricalDistanceBounds(c1,c2): theoreticalDistanceBounds(c1,c2);

                int c1Loc = LHS.length+ i;
                int c2Loc = LHS.length+ j;
                int[] location = cp.getLeft().equals(c1) ? new int[]{c1Loc, c2Loc} : new int[]{c2Loc, c1Loc};

                double dot0 = distToDot(cp.getBounds().getLB());
                double dot1 = distToDot(cp.getBounds().getUB());
                withinLowerDot += 2 * FastMath.min(dot0,dot1);
                withinUpperDot += 2 * FastMath.max(dot0,dot1);
                maxLowerBoundSubset = FastMath.max(maxLowerBoundSubset, distToSim(cp.getBounds().getUB()));

                //    Add bound factor for reverse engineering of bounds
                CC.addEmpiricalBoundFactor(new EmpiricalBoundFactor(cp, true, location));
            }
        }


//        Compute bounds
        double lowerD = Math.sqrt(Math.max(0,LHS.length + RHS.length - betweenUpperDot + withinLowerDot));
        double upperD = Math.sqrt(Math.max(0,LHS.length + RHS.length - betweenLowerDot + withinUpperDot));

        double lower = euclToSim(upperD);
        double upper = euclToSim(lowerD);

        return new ClusterBounds(correctBound(lower), correctBound(upper), maxLowerBoundSubset);
    }

    //    Compute empirical similarity directly, based on a list of extrema distances, partitioned by their impact on the similarity
    public double empiricalSimilarity(FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors, int pLeft, int pRight, double[][] pairwiseDistances){
        double betweenDot = 0;
        double withinDot = 0;

//        Iterate over boundFactors and add to relevant bound element
        for (EmpiricalBoundFactor ebf: empiricalBoundFactors) {
            if (ebf.isPositiveImpact()){
                withinDot += 2*distToDot(ebf.getExtremaDistance(pairwiseDistances));
            } else {
                betweenDot += 2*distToDot(ebf.getExtremaDistance(pairwiseDistances));
            }
        }
        double euclideanDistance = Math.sqrt(Math.max(0,pLeft + pRight - betweenDot + withinDot));
        return euclToSim(euclideanDistance);
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
