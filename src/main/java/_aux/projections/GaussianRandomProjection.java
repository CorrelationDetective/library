package _aux.projections;

import java.util.Random;

public class GaussianRandomProjection extends RandomProjection {

    public GaussianRandomProjection(Integer nComponents) {
        super(nComponents);
    }

    public GaussianRandomProjection(double epsilon, double delta) {
        super(epsilon, delta);
    }

    public GaussianRandomProjection(){}


    /**
     * Generate a dense Gaussian random matrix.
     * The components of the random matrix are drawn from N(0, 1.0 / n_components).
     *
     * @param n_components Dimensionality of the target projection space.
     * @param n_features Dimensionality of the original source space.
     * @param random_state Controls the pseudo random number generator used to generate the matrix at fit time.
     * @return The generated Gaussian random matrix.
     */
    public static double[][] gaussianRandomMatrix(int n_components, int n_features, Long random_state) {
        checkInputSize(n_components, n_features);
        Random rng = checkRandomState(random_state);
        double[][] components = new double[n_features][n_components];

        for (int i = 0; i < n_features; i++) {
            for (int j = 0; j < n_components; j++) {
                components[i][j] = rng.nextGaussian() / Math.sqrt(n_components);
            }
        }
        return components;
    }

    @Override
    public double[][] makeRandomMatrix(int nComponents, int nFeatures){
        return gaussianRandomMatrix(nComponents, nFeatures, this.randomState);
    }
}
