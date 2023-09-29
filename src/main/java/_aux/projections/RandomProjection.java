package _aux.projections;

import _aux.lib;
import core.RunParameters;
import lombok.Setter;

import java.util.Random;
import java.util.logging.Logger;


public abstract class RandomProjection {
    @Setter public Integer nComponents;
    @Setter public double epsilon;
    @Setter public double delta;

    @Setter protected Long randomState;

    public double[][] projectionMatrix;
    public boolean fitted = false;

    public RandomProjection(Integer nComponents){
        this.nComponents = nComponents;
    }

    public RandomProjection(double epsilon, double delta){
        this.epsilon = epsilon;
        this.delta = delta;
    }

    public RandomProjection(){}

    public abstract double[][] makeRandomMatrix(int nComponents, int nFeatures);

    /**
     * Generate a sparse random projection matrix.
     * @param X: Training set: only the shape is used to find optimal random
     *             matrix dimensions based on the theory referenced in the
     *             afore mentioned papers.
     * @return Sparse random projection matrix.
     */
    public RandomProjection fit(double[][] X) {
        int n_features = X[0].length;
        int n_points = X.length;

        int targetDim;
        if (this.nComponents == null) {
            targetDim = johnsonLindenstraussMinDim(n_points, this.epsilon, this.delta);
            if (targetDim <= 0) {
                throw new IllegalArgumentException(String.format("eps=%.2f and delta=%.2f lead to a target dimension of %d which is invalid",
                        this.epsilon, this.delta, targetDim));
            } else if (targetDim > n_features) {
                Logger.getGlobal().warning(String.format("The number of components is higher than the number of features. "
                        + "n_features < n_components (%d < %d)."
                        + "The dimensionality of the problem will not be reduced.", n_features, targetDim));
            }
        } else {
            if (this.nComponents <= 0) {
                throw new IllegalArgumentException("n_components must be greater than 0, got " + this.nComponents);
            }

            if (this.nComponents > n_features) {
                Logger.getGlobal().warning(String.format("The number of components is higher than the number of features. "
                        + "n_features < n_components (%d < %d)."
                        + "The dimensionality of the problem will not be reduced.", n_features, nComponents));
            }
            targetDim = this.nComponents;
        }

        this.projectionMatrix = this.makeRandomMatrix(targetDim, n_features);
        this.fitted = true;

        return this;
    }

    /**
     * Apply the random projection on X.
     * @param X: The input data to project into a smaller dimensional space.
     * @return X_new: The transformed data.
     */
    public double[][] transform(double[][] X) {
//        Check if fitted
        if (!this.fitted) {
            throw new IllegalStateException("The projection matrix has not been fitted yet. Call 'fit' with " +
                    "appropriate arguments before using this method.");
        }

        int n_samples = X.length;
        int n_features = X[0].length;

        if (n_features != this.projectionMatrix.length) {
            throw new IllegalArgumentException("The number of features of the dataset to transform must match the " +
                    "number of rows of the projection matrix. Got " + n_features + " features, expected " +
                    this.projectionMatrix.length + ".");
        }

        double[][] X_new;
        X_new = lib.mmul(X, this.projectionMatrix);
        return X_new;
    }

    /**
     * Fit to data, then transform it.
     * @param X: The input data.
     * @return X_new: The transformed data.
     */
    public double[][] fitTransform(double[][] X) {
        this.fit(X);
        return this.transform(X);
    }

    /**
     Find a 'safe' number of components to randomly project to.

     The distortion introduced by a random projection `p` only changes the
     distance between two points by a factor (1 +- eps) in an euclidean space
     with good probability. The projection `p` is an eps-embedding as defined
     by:

     (1 - eps) ||u - v||^2 < ||p(u) - p(v)||^2 < (1 + eps) ||u - v||^2

     Where u and v are any rows taken from a dataset of shape (n_samples,
     n_features), eps is in ]0, 1[ and p is a projection by a random Gaussian
     N(0, 1) matrix of shape (n_components, n_features) (or a sparse
     Achlioptas matrix).

     The minimum number of components to guarantee the eps-embedding is
     given by:

     n_components >= 4 log(n_samples) / (eps^2 / 2 - eps^3 / 3)

     Note that the number of dimensions is independent of the original
     number of features but instead depends on the size of the dataset:
     the larger the dataset, the higher is the minimal dimensionality of
     an eps-embedding.

     @param epsilon : double, \
     Maximum distortion rate in the range (0,1 ) as defined by the
     Johnson-Lindenstrauss lemma.

     @param delta : double, \
     Probability that the random projection preserves the norm
     (1 - delta) for each sample.

     @return nComponents : int
     The minimal number of components to guarantee with delta probability
     an eps-embedding.

     References
     ----------

     .. [1] https://en.wikipedia.org/wiki/Johnson%E2%80%93Lindenstrauss_lemma

     .. [2] Sanjoy Dasgupta and Anupam Gupta, 1999,
     "An elementary proof of the Johnson-Lindenstrauss Lemma."
     http://citeseer.ist.psu.edu/viewdoc/summary?doi=10.1.1.45.3654
     **/
    public int johnsonLindenstraussMinDim(double n, double epsilon, double delta) {
        if (epsilon <= 0.0 || epsilon >= 1.0) {
            throw new IllegalArgumentException("The JL bound is defined for eps in [0,1], got " + epsilon);
        }
        if (delta <= 0.0 || delta >= 0.5) {
            throw new IllegalArgumentException("The JL bound is defined for eps in [0,0.5], got " + delta);
        }
        double nominator = Math.log(delta / 2);
        double denominator = (-epsilon * epsilon / 4) + (epsilon * epsilon * epsilon / 6);
        return (int) Math.ceil(nominator / denominator);
    }

    /**
     * Factorize density check according to Li et al.
     * @param density the density value to be checked
     * @param n_features the number of features
     * @return the validated density value
     */
    protected static double checkDensity(Double density, int n_features) {
        if (density == null) {
            density = 1 / Math.sqrt(n_features);
        } else if (density <= 0 || density > 1) {
            throw new IllegalArgumentException("Expected density in range ]0, 1], got: " + density);
        }
        return density;
    }

    /**
     * Factorize argument checking for random matrix generation.
     * @param n_components the number of components
     * @param n_features the number of features
     */
    protected static void checkInputSize(int n_components, int n_features) {
        if (n_components <= 0) {
            throw new IllegalArgumentException("n_components must be strictly positive, got " + n_components);
        }
        if (n_features <= 0) {
            throw new IllegalArgumentException("n_features must be strictly positive, got " + n_features);
        }
    }

    protected static Random checkRandomState(Long randomState){
        return randomState == null ? new Random() : new Random(randomState);
    }
}
