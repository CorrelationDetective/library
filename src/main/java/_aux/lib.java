package _aux;

import _aux.lists.FastArrayList;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.FastMath;
import queries.ResultTuple;

import java.io.*;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class contains a number of static methods that are used throughout the codebase.
 */
public class lib {


    /**
     * Transposes a 2D matrix.
     *
     * @param matrix The input matrix to be transposed.
     * @return The transposed matrix.
     */
    public static double[][] transpose(double[][] matrix) {
        double[][] t = new double[matrix[0].length][matrix.length];
        for (int i=0;i<matrix.length;i++)
            for (int j=0;j<matrix[0].length;j++)
                t[j][i]=matrix[i][j];
        return t;
    }

    /**
     * Calculates the base-2 logarithm of a number.
     *
     * @param x The input number.
     * @return The base-2 logarithm of the input number.
     */
    public static double log2(double x) {
        return FastMath.log(x)/FastMath.log(2);
    }

    /**
     * Adds two arrays element-wise.
     *
     * @param in1 The first input array.
     * @param in2 The second input array.
     * @return The resulting array after element-wise addition.
     */
    public static double[] add(double[] in1, double[] in2) {
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) res[i]=in1[i]+in2[i];
        return res;
    }

    /**
     * Computes the sum of each column in a 2D array.
     *
     * @param in The input 2D array.
     * @return An array containing the sums of each column.
     */
    public static double[] colSum(double[][] in) {
        double[] res = new double[in.length];
        for (int i=0;i<in.length;i++) res[i]=Arrays.stream(in[i]).sum();
        return res;
    }

    /**
     * Computes the sum of each row in a 2D array.
     *
     * @param in The input 2D array.
     * @return An array containing the sums of each row.
     */
    public static double[] rowSum(double[][] in) {
        int n = in.length;
        int m = in[0].length;
        double[] res = new double[m];
        for (int i=0;i<m;i++) {
            double sum = 0;
            for (int j=0;j<n;j++) sum+=in[j][i];
            res[i]=sum;
        }
        return res;
    }

    /**
     * Calculates the average value of an array.
     *
     * @param z The input array.
     * @return The average value of the input array.
     */
    public static double avg(double[] z){return Arrays.stream(z).sum()/z.length;}

    /**
     * Calculates the sum of an array.
     *
     * @param z The input array.
     * @return The sum of the input array.
     */
    public static double sum(double[] z){return Arrays.stream(z).sum();}

    /**
     * Calculates the variance of an array.
     *
     * @param z The input array.
     * @return The variance of the input array.
     */
    public static double var(double[] z){
        double sum = Arrays.stream(z).reduce(0, Double::sum);
        double sumSquare = Arrays.stream(z).reduce(0, (a,b) -> a+b*b);
        double avg = sum/z.length;
        return sumSquare/z.length - avg*avg;
    }

    /**
     * Calculates the standard deviation of an array.
     *
     * @param z The input array.
     * @return The standard deviation of the input array.
     */
    public static double std(double [] z){return FastMath.sqrt(var(z));}

    /**
     * Calculates the minimum value of an array.
     *
     * @param z The input array.
     * @return The minimum value of the input array.
     */
    public static double min(double[] z){return Arrays.stream(z).min().getAsDouble();}

    /**
     * Calculates the maximum value of an array.
     *
     * @param z The input array.
     * @return The maximum value of the input array.
     */
    public static double max(double[] z){return Arrays.stream(z).max().getAsDouble();}

    /**
     * Subtracts two arrays element-wise.
     *
     * @param in1 The first input array.
     * @param in2 The second input array.
     * @return The resulting array after element-wise subtraction.
     */
    public static double[] sub(double[] in1, double[] in2) {
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) res[i]=in1[i]-in2[i];
        return res;
    }

    /**
     * Scales an array by a scalar factor.
     *
     * @param in1  The input array.
     * @param in2  The scalar factor.
     * @return The scaled array.
     */
    public static double[] scale(double[] in1, double in2) {
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) res[i]=in1[i]*in2;
        return res;
    }

    /**
     * Adds a scalar to each element of an array.
     *
     * @param in1 The input array.
     * @param in2 The scalar to be added.
     * @return The array after adding the scalar to each element.
     */
    public static double[] sadd(double[] in1, double in2) {
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) res[i]=in1[i]+in2;
        return res;
    }

    /**
     * Calculates the element-wise mean of a list of vectors.
     *
     * @param in The list of vectors.
     * @return An array containing the element-wise mean of the vectors.
     */
    public static double[] elementwiseAvg(double[][] in) {
        double[] res = new double[in[0].length];
        for (int i=0;i<in[0].length;i++) {
            double sum = 0;
            for (int j=0;j<in.length;j++) sum+=in[j][i];
            res[i]=sum/in.length;
        }
        return res;
    }

    /**
     * Calculates the L2 norm (Euclidean norm) of a vector.
     *
     * @param in1 The input vector.
     * @return The L2 norm of the input vector.
     */
    public static double l2(double[] in1) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = in1[i];
            d+=(dd*dd);
        }
        return FastMath.sqrt(d);
    }

    /**
     * Calculates the L1 norm (Manhattan norm) between two vectors.
     *
     * @param in1 The first input vector.
     * @param in2 The second input vector.
     * @return The L1 norm between the two input vectors.
     */
    public static double l1(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = FastMath.abs(in1[i]-in2[i]);
            d+=dd;
        }
        return d;
    }

    /**
     * Computes the ranks of elements in an array.
     *
     * @param in The input array.
     * @return An array of ranks for the elements in the input array.
     */
    public static double[] rank(double[] in) {
        Integer[] indexes = new Integer[in.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, new Comparator<Integer>() {
            @Override
            public int compare(final Integer i1, final Integer i2) {
                return Double.compare(in[i1], in[i2]);
            }
        });
        return IntStream.range(0, indexes.length).mapToDouble(i -> indexes[i]).toArray();
    }

    /**
     * Calculates the dot product between two vectors.
     *
     * @param in1 The first input vector.
     * @param in2 The second input vector.
     * @return The dot product between the two input vectors.
     */
    public static double dot(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = in1[i]*in2[i];
            d+=dd;
        }
        return d;
    }

//    public static double geoMean(double[] in){
//        double mean = 1;
//        for (int i = 0; i < m; i++) {
//            mean *= in[i];
//        }
//        mean = FastMath.pow(mean, 1.0/m);
//        return mean;
//    }

    /**
     * Computes the geometric mean of elements in an array.
     *
     * @param in The input array.
     * @return The geometric mean of the elements in the input array.
     */
    public static double geoMean(double[] in){
        double M = 1e100;
        double mean = 1;
        double c = 0;
        for (int i = 0; i < in.length; i++) {
            mean *= in[i];
            if (FastMath.abs(mean) > M){
                mean /= M;
                c++;
            }
        }
        mean = FastMath.pow(mean, 1.0/in.length);
        if (c > 0) mean *= FastMath.pow(M, c/in.length);
        return mean;
    }

    /**
     * Computes the geometric mean of elements in an array after taking the logarithm.
     *
     * @param in The input array.
     * @return The geometric mean of the logarithmic values of the elements.
     */
    public static double geoMeanLogged(double[] in) {
        double d = 0;
        for (int i=0;i<in.length;i++) {
            double dd = FastMath.log(in[i]);
            d+=dd;
        }
        return FastMath.exp(d/in.length);
    }

    /**
     * Calculates the absolute differences between elements of two arrays.
     *
     * @param in1 The first input array.
     * @param in2 The second input array.
     * @return An array of absolute differences between the corresponding elements.
     */
    public static double[] absDiff(double[] in1, double[] in2){
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) {
            res[i] = FastMath.abs(in1[i]-in2[i]);
        }
        return res;
    }

    /**
     * Computes the geometric mean of absolute differences between elements of two arrays.
     *
     * @param in1 The first input array.
     * @param in2 The second input array.
     * @return The geometric mean of the absolute differences.
     */
    public static double absDiffGeoMean(double[] in1, double[] in2){
        double M = 1e100;
        double mean = 1;
        double c = 0;
        for (int i = 0; i < in1.length; i++) {
            mean *= FastMath.abs(in1[i] - in2[i]);
            if (FastMath.abs(mean) > M){
                mean /= M;
                c++;
            }
        }
        mean = FastMath.pow(mean, 1.0/in1.length);
        if (c > 0) mean *= FastMath.pow(M, c/in1.length);
        return mean;
    }

    /**
     * Sorts an array in ascending order.
     *
     * @param in The input array.
     * @return The sorted array in ascending order.
     */
    public static double[] sort(double[] in){
        Arrays.sort(in);
        return in;
    }

    /**
     * Calculates the median of absolute differences between elements of two arrays.
     *
     * @param in1 The first input array.
     * @param in2 The second input array.
     * @return The median of absolute differences.
     */
    public static double absDiffMedian(double[] in1, double[] in2){
        double[] diffs = lib.sort(absDiff(in1, in2));
        return diffs[diffs.length/2];
    }

    /**
     * Calculates the normed angle (angle in radians) between two vectors.
     *
     * @param in1 The first input vector.
     * @param in2 The second input vector.
     * @return The normed angle between the two input vectors in radians.
     */
    public static double normedAngle(double[] in1, double[] in2) {
        return FastMath.acos(FastMath.min(FastMath.max(lib.dot(in1, in2), -1),1));
    }

    /**
     * Calculates the Euclidean distance between two vectors.
     *
     * @param in1 The first input vector.
     * @param in2 The second input vector.
     * @return The Euclidean distance between the two input vectors.
     */
    public static double euclidean(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = in1[i]-in2[i];
            d+=(dd*dd);
        }
        double out = FastMath.sqrt(d);
        return out;
    }

    /**
     * Calculates the Euclidean distance between two vectors.
     *
     * @param in1 The first input vector.
     * @param in2 The second input vector.
     * @return The Euclidean distance between the two input vectors.
     */
    public static double euclidean(Complex[] in1, Complex[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            Complex sub = in1[i].subtract(in2[i]);
            d+= sub.multiply(sub.conjugate()).getReal();
        }
        return FastMath.sqrt(d);
    }


    /**
     * Calculates the squared Euclidean distance between two vectors.
     *
     * @param in1 The first input vector.
     * @param in2 The second input vector.
     * @return The squared Euclidean distance between the two input vectors.
     */
    public static double euclideanSquared(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = in1[i]-in2[i];
            d+=(dd*dd);
        }
        return d;
    }

    /**
     * Calculates the Minkowski distance between two vectors with a given parameter 'p'.
     *
     * @param in1 The first input vector.
     * @param in2 The second input vector.
     * @param p   The parameter 'p' for the Minkowski distance.
     * @return The Minkowski distance between the two input vectors with the given 'p'.
     */
    public static double minkowski(double[] in1, double[] in2, double p) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = FastMath.pow(FastMath.abs(in1[i]-in2[i]), p);
            d+=dd;
        }
        return FastMath.pow(d, 1/p);
    }

    /**
     * Calculates the Manhattan distance between two vectors.
     *
     * @param in1 The first input vector.
     * @param in2 The second input vector.
     * @return The Manhattan distance between the two input vectors.
     */
    public static double manhattan(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = FastMath.abs(in1[i]-in2[i]);
            d+=dd;
        }
        return d;
    }

    /**
     * Calculates the Chebyshev distance between two vectors.
     *
     * @param in1 The first input vector.
     * @param in2 The second input vector.
     * @return The Chebyshev distance between the two input vectors.
     */
    public static double chebyshev(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = FastMath.abs(in1[i]-in2[i]);
            if (dd>d) d=dd;
        }
        return d;
    }

    /**
     * Multiplies a vector by a matrix.
     *
     * @param v The input vector.
     * @param M The input matrix.
     * @return The result of multiplying the vector by the matrix.
     * @throws DimensionMismatchException When the dimensions are incompatible for multiplication.
     */
    public static double[] mmul(double[] v, double[][] M) throws DimensionMismatchException {
        if (v.length != M[0].length){throw new DimensionMismatchException(v.length, M[0].length);}

        int m = M.length;
        double[] out = new double[m];

        for (int i = 0; i < m; i++) {
            out[i] = lib.dot(v,M[i]);
        }
        return out;
    }

    /**
     * Multiplies two matrices.
     *
     * @param X The first input matrix.
     * @param Y The second input matrix.
     * @return The result of multiplying the two matrices.
     * @throws DimensionMismatchException When the dimensions are incompatible for multiplication.
     */
    public static double[][] mmul(double[][] X, double[][] Y) throws DimensionMismatchException {
        if (X[0].length != Y.length){throw new DimensionMismatchException(X[0].length, Y.length);}

        int m = X.length;
        int n = Y[0].length;
        int o = Y.length;
        double[][] out = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < o; j++) {
                for (int k = 0; k < n; k++) {
                    out[i][k] += X[i][j] * Y[j][k];
                }
            }
        }
        return out;
    }

    /**
     * Parallelized matrix multiplication using streams.
     *
     * @param X The first input matrix.
     * @param Y The second input matrix.
     * @return The result of parallelized matrix multiplication.
     * @throws DimensionMismatchException When the dimensions are incompatible for multiplication.
     */
    public static double[][] parallelMmul(double[][] X, double[][] Y) throws DimensionMismatchException {
        if (X[0].length != Y.length){throw new DimensionMismatchException(X[0].length, Y.length);}

        int m = X.length;
        int n = Y[0].length;
        int o = Y.length;
        double[][] out = new double[m][n];

        IntStream.range(0, m).parallel().forEach(i -> {
            for (int j = 0; j < o; j++) {
                for (int k = 0; k < n; k++) {
                    out[i][k] += X[i][j] * Y[j][k];
                }
            }
        });
        return out;
    }

    /**
     * Z-normalizes a vector.
     *
     * @param v The input vector.
     * @return The Z-normalized vector.
     */
    public static double[] znorm(double[] v) {
        double[] z = v.clone();
        double sum = 0;
        double sumSquare = 0;
        for (int i=0;i<z.length;i++) {
            sum+=z[i];
            sumSquare+=z[i]*z[i];
        }
        double avg = sum/z.length;
        double var = sumSquare/z.length - avg*avg;
        var=FastMath.max(var + 1E-16, -var); // for floating point errors
        double stdev = FastMath.sqrt(var);

        for (int i=0;i<z.length;i++){
            z[i]=(z[i]-avg)/stdev;
            if(Double.isNaN(z[i])){
                System.out.println("debug: NaN result of znorm");
            }
        }
        return z;
    }

    /**
     * Z-normalizes a matrix by row.
     *
     * @param Z The input matrix.
     * @return The Z-normalized matrix.
     */
    public static double[][] znorm(double[][] Z) {
        for (int i=0;i<Z.length;i++) {
            Z[i]=znorm(Z[i]);
        }
        return Z;
    }


    /**
     * Zero-sum and L2-normalize a vector.
     *
     * @param v The input vector.
     * @return The zero-sum and L2-normalized vector.
     */
    public static double[] l2norm(double[] v) {
        double[] z = v.clone();
        double sum = Arrays.stream(z).reduce(0, Double::sum);
        double avg = sum / v.length;
        z = lib.sadd(v,-1*avg);
        double sumSquare = Arrays.stream(z).reduce(0, (a,b) -> a+b*b);
        double norm = FastMath.sqrt(sumSquare);
        z = lib.scale(z,1/norm);
        return z;
    }

    /**
     * L2-normalizes a matrix by row.
     *
     * @param Z The input matrix.
     * @return The L2-normalized matrix by row.
     */
    public static double[][] l2norm(double[][] Z) {
        for (int i=0;i<Z.length;i++) {
            Z[i]=l2norm(Z[i]);
        }
        return Z;
    }

    /**
     * Converts an array of real numbers to an array of complex numbers.
     *
     * @param x The input array of real numbers.
     * @return The array of complex numbers with real parts from the input array.
     */
    public static Complex[] toComplex(double[] x) {
        Complex[] res = new Complex[x.length];
        for (int i=0;i<x.length;i++) res[i]=new Complex(x[i],0);
        return res;
    }

    /**
     * Performs the Fast Fourier Transform (FFT) on an array of real numbers.
     *
     * @param x The input array of real numbers.
     * @return The FFT result as an array of complex numbers.
     */
    public static Complex[] fft(double[] x) {
        return new FastFourierTransformer(DftNormalization.STANDARD).transform(x, TransformType.FORWARD);
    }


    /**
     * Get a stream from an iterable collection.
     *
     * @param collection The input iterable collection.
     * @param parallel   True if the stream should be parallel, false if sequential.
     * @param <T>        The type of elements in the collection.
     * @return A stream of elements from the iterable collection.
     */
    public static <T> Stream<T> getStream(Iterable<T> collection, boolean parallel){
        return StreamSupport.stream(collection.spliterator(), parallel);
    }

    /**
     * Get a stream from a collection.
     *
     * @param collection The input collection.
     * @param parallel   True if the stream should be parallel, false if sequential.
     * @param <T>        The type of elements in the collection.
     * @return A stream of elements from the collection.
     */
    public static <T> Stream<T> getStream(Collection<T> collection, boolean parallel){
        if(parallel){
            return collection.parallelStream().parallel();
        }else{
            return collection.stream().sequential();
        }
    }

    /**
     * Get a stream from a stream.
     *
     * @param stream   The input stream.
     * @param parallel True if the stream should be parallel, false if sequential.
     * @param <T>      The type of elements in the stream.
     * @return A stream of elements from the stream.
     */
    public static <T> Stream<T> getStream(Stream<T> stream, boolean parallel){
        return parallel ? stream.parallel(): stream.sequential();
    }

    /**
     * Get a stream from a BitSet.
     *
     * @param bitSet   The input BitSet.
     * @param parallel True if the stream should be parallel, false if sequential.
     * @return A stream of elements from the BitSet.
     */
    public static IntStream getStream(BitSet bitSet, boolean parallel){
        return parallel ? bitSet.stream().parallel(): bitSet.stream().sequential();
    }

    /**
     * Get a stream from an array.
     *
     * @param array    The input array.
     * @param parallel True if the stream should be parallel, false if sequential.
     * @param <T>      The type of elements in the array.
     * @return A stream of elements from the array.
     */
    public static <T> Stream<T> getStream(T[] array, boolean parallel){
        if(parallel){
            return Arrays.stream(array).parallel();
        }else{
            return Arrays.stream(array).sequential();
        }
    }

    /**
     * Find the index of a value in an array.
     *
     * @param array The input array.
     * @param value The value to search for.
     * @param <T>   The type of elements in the array.
     * @return The index of the value in the array, or -1 if not found.
     */
    public static <T> int indexOf(T[] array, T value){
        for(int i=0;i<array.length;i++){
            if(array[i].equals(value)){
                return i;
            }
        }
        return -1;
    }

    /**
     * Check if an array contains a specific value.
     *
     * @param array The input array.
     * @param value The value to check for.
     * @param <T>   The type of elements in the array.
     * @return True if the array contains the value, otherwise false.
     */
    public static <T> boolean contains(T[] array, T value){
        for(T v: array){
            if(v.equals(value)){
                return true;
            }
        }
        return false;
    }

    /**
     * Check if an integer array contains a specific value.
     *
     * @param array The input integer array.
     * @param value The value to check for.
     * @return True if the integer array contains the value, otherwise false.
     */
    public static boolean contains(int[] array, int value){
        for(int v: array){
            if(v == value){
                return true;
            }
        }
        return false;
    }

    /**
     * Concatenate two arrays.
     *
     * @param a The first input array.
     * @param b The second input array.
     * @param <T> The type of elements in the arrays.
     * @return The concatenated array containing elements from both input arrays.
     */
    public static <T> T[] concat(T[] a, T[] b){
        T[] c = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    /**
     * Add a value to an array.
     *
     * @param a The input array.
     * @param b The value to add.
     * @param <T> The type of elements in the array.
     * @return The array with the added value.
     */
    public static <T> T[] add(T[] a, T b){
        T[] c = Arrays.copyOf(a, a.length + 1);
        c[a.length] = b;
        return c;
    }

    /**
     * Remove an element from an array.
     * @param a The input array.
     * @param index The index of the element to remove.
     * @param <T> The type of elements in the array.
     * @return The array with the element removed.
     */
    public static <T> T[] remove(T[] a, int index){
        T[] c = Arrays.copyOf(a, a.length - 1);
        System.arraycopy(a, index + 1, c, index, a.length - index - 1);
        return c;
    }

//    Checks if a certain path string is valid
    public static boolean isValidPath(String path){
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }

    /**
     * Generate a random number from the Cauchy distribution.
     *
     * @param random The random number generator.
     * @param gamma The scale parameter of the Cauchy distribution.
     * @return A random number from the Cauchy distribution.
     */
    public static double nextCauchy(Random random, double gamma){
        return gamma * Math.tan(Math.PI * (random.nextDouble() - 0.5));
    }

    /**
     * Convert nanoseconds to seconds.
     *
     * @param nano The time in nanoseconds.
     * @return The time in seconds.
     */
    public static double nanoToSec(long nano){return nano/1E9;}

    /**
     * Print a horizontal bar to the logger.
     *
     * @param logger The logger to print to.
     */
    public static void printBar(Logger logger){
        logger.fine("-------------------------------------");
    }

    /**
     * Read lines from a CSV file and store them in a list.
     *
     * @param filename The name of the CSV file to read.
     * @return A list containing the lines from the CSV file.
     */
    public static LinkedList<String> readCSV(String filename){
        LinkedList<String> lines = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    /**
     * Read a matrix from a CSV file and store it as a 2D array.
     *
     * @param filename The name of the CSV file to read.
     * @return A 2D array containing the matrix data from the CSV file.
     */
    public static double[][] readMatrix(String filename){
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            LinkedList<double[]> matrix = new LinkedList<>();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                double[] row = new double[values.length];
                for (int i = 0; i < values.length; i++) {
                    row[i] = Double.parseDouble(values[i]);
                }
                matrix.add(row);
            }
            return matrix.toArray(new double[matrix.size()][]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Write a matrix to a CSV file.
     *
     * @param filename The name of the CSV file to write to.
     * @param matrix   The 2D array representing the matrix data.
     */
    public static void writeMatrix(String filename, double[][] matrix) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    bw.write(matrix[i][j] + ",");
                }
                bw.write("\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write a matrix to a CSV file using a FastArrayList.
     *
     * @param filename The name of the CSV file to write to.
     * @param matrix   The FastArrayList representing the matrix data.
     */
    public static void writeMatrix(String filename, FastArrayList<double[]> matrix) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            for (int i = 0; i < matrix.size(); i++) {
                for (int j = 0; j < matrix.get(i).length; j++) {
                    bw.write(matrix.get(i)[j] + ",");
                }
                bw.write("\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get a list of result tuples from a file in CSV format.
     *
     * @param filename The name of the CSV file containing result tuples.
     * @return A list of result tuples parsed from the file.
     */
    public static List<ResultTuple> getResultsFromFile(String filename) {
        LinkedList<ResultTuple> results = new LinkedList<>();
        LinkedList<String> lines = lib.readCSV(filename);

//        Skip header
        lines.poll();

        while(!lines.isEmpty()){
            String[] split = lines.poll().split(",");
            if (split[2].equals("end")){break;}

            int[] LHS = Arrays.stream(split[0].split("-")).mapToInt(Integer::parseInt).toArray();

            int[] RHS = new int[split[1].length()];
            if (split[1].length() > 0) {
                RHS = Arrays.stream(split[1].split("-")).mapToInt(Integer::parseInt).toArray();
            }
            double sim = Double.parseDouble(split[4]);
            results.add(new ResultTuple(LHS,RHS, sim));
        }
        return new ArrayList<>(results);
    }

    /**
     * Get the total garbage collection time in nanoseconds.
     *
     * @return The total garbage collection time in nanoseconds.
     */
    public static long getGCTime(){
        long msTime = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        return msTime * 1000000;
    }

    /**
     * Check if a string represents a numeric value.
     *
     * @param s The input string to check.
     * @return True if the string represents a numeric value, otherwise false.
     */
    public static boolean isNumeric(String s) {
        return s.matches("[-+]?\\d*\\.?\\d+");
    }

    /**
     * Parse a string into its appropriate data type (int, double, boolean, or string).
     *
     * @param s The input string to parse.
     * @return The parsed value as an Object.
     */
    public static Object parseString(String s){
        if(isNumeric(s)){
            if(s.contains(".")){
                return Double.parseDouble(s);
            }else{
                return Integer.parseInt(s);
            }
        }else if (s.equals("true") || s.equals("false")){
            return Boolean.parseBoolean(s);
        }else{
            return s;
        }
    }

    /**
     * Parse a config file into a map of key-value pairs.
     * @param filename The name of the config file to parse.
     *                 The config file should be in the format of key-value pairs separated by an equals sign.
     * @return A map of key-value pairs parsed from the config file.
     */
    public static Properties parseConfig(String filename){
        Properties config = new Properties();
        try {
            config.load(new FileInputStream(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }




}
