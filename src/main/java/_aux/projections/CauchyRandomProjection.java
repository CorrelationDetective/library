package _aux.projections;

import _aux.lib;
import org.apache.commons.math3.util.FastMath;

import java.util.Random;

public class CauchyRandomProjection extends RandomProjection {

    public CauchyRandomProjection(Integer nComponents) {
        super(nComponents);
    }

    public CauchyRandomProjection(double epsilon, double delta) {
        super(epsilon, delta);
    }

    public CauchyRandomProjection(){}

    /**
     * Generate a dense Cauchy random matrix.
     * The components of the random matrix are drawn from N(0, 1.0 / n_components).
     *
     * @param n_components Dimensionality of the target projection space.
     * @param n_features Dimensionality of the original source space.
     * @param random_state Controls the pseudo random number generator used to generate the matrix at fit time.
     * @return The generated Cauchy random matrix.
     */
    public static double[][] cauchyRandomMatrix(int n_components, int n_features, Long random_state) {
        checkInputSize(n_components, n_features);
        Random rng = checkRandomState(random_state);
        double[][] components = new double[n_features][n_components];

        for (int i = 0; i < n_features; i++) {
            for (int j = 0; j < n_components; j++) {
                components[i][j] = lib.nextCauchy(rng, 1);
            }
        }
        return components;
    }

    @Override
    public double[][] makeRandomMatrix(int nComponents, int nFeatures){
        return cauchyRandomMatrix(nComponents, nFeatures, this.randomState);
    }

//    Find a safe number of components to use in the projection
    @Override
    public int johnsonLindenstraussMinDim(double n, double epsilon, double delta) {
        if (epsilon <= 0.0 || epsilon >= 1.0) {
            throw new IllegalArgumentException("The JL bound is defined for eps in [0,1], got " + epsilon);
        }
        if (delta <= 0.0 || delta >= 1) {
            throw new IllegalArgumentException("The JL bound is defined for delta in [0,1], got " + delta);
        }
        double logMin = FastMath.log(1 - epsilon);
        double pi2 = 2 / FastMath.PI;

        double numerator = 2*FastMath.log(n) - FastMath.log(delta);
        double denominator = -.5*FastMath.log(1 + (pi2 * logMin) * (pi2 * logMin)) + pi2 * FastMath.atan(pi2*logMin) * logMin;
        return (int) Math.ceil(numerator / denominator);
    }

}
