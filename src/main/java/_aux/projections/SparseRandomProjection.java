package _aux.projections;

import org.apache.commons.math3.util.FastMath;

import java.util.Random;

public class SparseRandomProjection extends RandomProjection {
    private Double density;

    public SparseRandomProjection(Integer nComponents, Double density) {
        super(nComponents);
        this.density = density;
    }

    public SparseRandomProjection(Double epsilon, Double delta, Double density) {
        super(epsilon, delta);
        this.density = density;
    }

    public SparseRandomProjection(int nComponents) {
        super(nComponents);
    }

    public SparseRandomProjection(double epsilon, double delta) {
        super(epsilon, delta);
    }

    public SparseRandomProjection(){}

    /**
     * Generalized Achlioptas random sparse matrix for random projection.
     * Setting density to 1 / 3 will yield the original matrix by Dimitris Achlioptas,
     * while setting a lower value will yield the generalization by Ping Li et al.
     * If we note s = 1 / density, the components of the random matrix are drawn from:
     * - -sqrt(s) / sqrt(n_components) with probability 1 / 2s
     * - 0 with probability 1 - 1 / s
     * - +sqrt(s) / sqrt(n_components) with probability 1 / 2s
     *
     * @param n_components Dimensionality of the target projection space.
     * @param n_features Dimensionality of the original source space.
     * @param density Ratio of non-zero component in the random projection matrix in the range (0, 1].
     * @param random_state Controls the pseudo random number generator used to generate the matrix at fit time.
     * @return The generated random matrix in SparseMatrix format.
     */
    public static double[][] sparseRandomMatrix(int n_components, int n_features, Double density, Long random_state) {
        checkInputSize(n_components, n_features);
        density = checkDensity(density, n_features);
        Random rng = checkRandomState(random_state);

        double[][] components = new double[n_features][n_components];

        boolean sparse = density < 1.0;

        double scaleFactor = sparse ? FastMath.sqrt(1d / density) / FastMath.sqrt(n_components):
                1 / Math.sqrt(n_components);
        for (int i = 0; i < n_features; i++) {
            for (int j = 0; j < n_components; j++) {
                if (!sparse || rng.nextDouble() < density) {
                    double sign = (rng.nextInt(2) == 0) ? -1.0 : 1.0;
                    components[i][j] = sign * scaleFactor;
                }
            }
        }
        return components;
    }

    @Override
    public double[][] makeRandomMatrix(int nComponents, int nFeatures){
        return sparseRandomMatrix(nComponents, nFeatures, this.density, this.randomState);
    }
}
