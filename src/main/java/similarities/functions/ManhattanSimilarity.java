package similarities.functions;

import _aux.lib;
import _aux.lists.FastArrayList;
import _aux.projections.CauchyRandomProjection;
import bounding.ClusterBounds;
import bounding.ClusterCombination;
import bounding.EmpiricalBoundFactor;
import core.RunParameters;
import lombok.NonNull;
import org.apache.commons.math3.util.FastMath;
import similarities.MultivariateSimilarityFunction;

public class ManhattanSimilarity extends MultivariateSimilarityFunction {

    private double liCorrectionFactor;

    public ManhattanSimilarity(RunParameters runParameters) {
        super(runParameters);
        this.transformer = new CauchyRandomProjection();
        this.empiricalBounded = false;

//        VARIABLE DIST FUNCTION
        this.distFunc = this::variableDist;
//        this.distFunc = Parameters.dimensionalityReduction ? lib::absDiffMedian: lib::manhattan;
        MAX_SIMILARITY = 1;
        MIN_SIMILARITY = 0;
        SIMRANGE = MAX_SIMILARITY - MIN_SIMILARITY;

        init();
    }

//    Manhattan distance when no dimensionality reduction, otherwise Li distance
    private double variableDist(double[] in1, double[] in2){
        return runParameters.isDimensionalityReduction() ? liDist(in1, in2) : lib.manhattan(in1, in2);
    }

//    Bias-corrected geometric mean estimator of Li 2007, used to estimate l1-distance with cauchy random projections
    private double liDist(double[] in1, double[] in2){
        int m = runParameters.getNDimensions();
//        Compute correction factor if not yet computed -- cos(pi / 2m)^m
        if (liCorrectionFactor == 0){
            this.liCorrectionFactor = FastMath.pow(FastMath.cos(FastMath.PI / (2 * m)), m);
        }
        return lib.absDiffGeoMean(in1, in2) * liCorrectionFactor;
    }


    @Override public double[] preprocess(double[] vector) {
        return vector;
    }

    @Override public double sim(double[] x, double[] y) {
        return 1 / (1 + this.distFunc.dist(x, y));
    }

    @Override public double simToDist(double sim) {
        return 1 / sim - 1;
    }

    @Override public double distToSim(double dist) {
        return 1 / (1 + dist);
    }

    @Override public ClusterBounds empiricalSimilarityBounds(ClusterCombination CC) {
        throw new RuntimeException("Empirical bounds not implemented for this similarity function");
    }

    public double empiricalSimilarity(FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors, int pLeft, int pRight,
                                               double[][] pairwiseDistances){
        throw new RuntimeException("Empirical bounds not implemented for this similarity function");
    }
}
