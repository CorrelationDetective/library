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
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class lib {
    public static double[][] transpose(double[][] matrix) {
        double[][] t = new double[matrix[0].length][matrix.length];
        for (int i=0;i<matrix.length;i++)
            for (int j=0;j<matrix[0].length;j++)
                t[j][i]=matrix[i][j];
        return t;
    }

    public static double log2(double x) {
        return FastMath.log(x)/FastMath.log(2);
    }

    public static double[] add(double[] in1, double[] in2) {
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) res[i]=in1[i]+in2[i];
        return res;
    }

    public static double[] colSum(double[][] in) {
        double[] res = new double[in.length];
        for (int i=0;i<in.length;i++) res[i]=Arrays.stream(in[i]).sum();
        return res;
    }

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

    public static double avg(double[] z){return Arrays.stream(z).sum()/z.length;}
    public static double sum(double[] z){return Arrays.stream(z).sum();}
    public static double var(double[] z){
        double sum = Arrays.stream(z).reduce(0, Double::sum);
        double sumSquare = Arrays.stream(z).reduce(0, (a,b) -> a+b*b);
        double avg = sum/z.length;
        return sumSquare/z.length - avg*avg;
    }

    public static double std(double [] z){return FastMath.sqrt(var(z));}

    public static double min(double[] z){return Arrays.stream(z).min().getAsDouble();}
    public static double max(double[] z){return Arrays.stream(z).max().getAsDouble();}

    public static double[] sub(double[] in1, double[] in2) {
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) res[i]=in1[i]-in2[i];
        return res;
    }

    //    Multiply by scalar
    public static double[] scale(double[] in1, double in2) {
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) res[i]=in1[i]*in2;
        return res;
    }

    public static double[] sadd(double[] in1, double in2) {
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) res[i]=in1[i]+in2;
        return res;
    }

//    Get element-wise mean of list of vectors
    public static double[] elementwiseAvg(double[][] in) {
        double[] res = new double[in[0].length];
        for (int i=0;i<in[0].length;i++) {
            double sum = 0;
            for (int j=0;j<in.length;j++) sum+=in[j][i];
            res[i]=sum/in.length;
        }
        return res;
    }

    public static double l2(double[] in1) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = in1[i];
            d+=(dd*dd);
        }
        return FastMath.sqrt(d);
    }
    public static double l1(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = FastMath.abs(in1[i]-in2[i]);
            d+=dd;
        }
        return d;
    }

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

    public static double geoMeanLogged(double[] in) {
        double d = 0;
        for (int i=0;i<in.length;i++) {
            double dd = FastMath.log(in[i]);
            d+=dd;
        }
        return FastMath.exp(d/in.length);
    }

    public static double[] absDiff(double[] in1, double[] in2){
        double[] res = new double[in1.length];
        for (int i=0;i<in1.length;i++) {
            res[i] = FastMath.abs(in1[i]-in2[i]);
        }
        return res;
    }

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

    public static double[] sort(double[] in){
        Arrays.sort(in);
        return in;
    }

//    Median of absolute differences
    public static double absDiffMedian(double[] in1, double[] in2){
        double[] diffs = lib.sort(absDiff(in1, in2));
        return diffs[diffs.length/2];
    }

    public static double normedAngle(double[] in1, double[] in2) {
        return FastMath.acos(FastMath.min(FastMath.max(lib.dot(in1, in2), -1),1));
    }

    public static double euclidean(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = in1[i]-in2[i];
            d+=(dd*dd);
        }
        double out = FastMath.sqrt(d);
        return out;
    }

    public static double euclidean(Complex[] in1, Complex[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            Complex sub = in1[i].subtract(in2[i]);
            d+= sub.multiply(sub.conjugate()).getReal();
        }
        return FastMath.sqrt(d);
    }



    public static double euclideanSquared(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = in1[i]-in2[i];
            d+=(dd*dd);
        }
        return d;
    }

    public static double minkowski(double[] in1, double[] in2, double p) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = FastMath.pow(FastMath.abs(in1[i]-in2[i]), p);
            d+=dd;
        }
        return FastMath.pow(d, 1/p);
    }

    public static double manhattan(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = FastMath.abs(in1[i]-in2[i]);
            d+=dd;
        }
        return d;
    }

    public static double chebyshev(double[] in1, double[] in2) {
        double d = 0;
        for (int i=0;i<in1.length;i++) {
            double dd = FastMath.abs(in1[i]-in2[i]);
            if (dd>d) d=dd;
        }
        return d;
    }

    public static double[] mmul(double[] v, double[][] M) throws DimensionMismatchException {
        if (v.length != M[0].length){throw new DimensionMismatchException(v.length, M[0].length);}

        int m = M.length;
        double[] out = new double[m];

        for (int i = 0; i < m; i++) {
            out[i] = lib.dot(v,M[i]);
        }
        return out;
    }

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

    public static double[][] znorm(double[][] Z) {
        for (int i=0;i<Z.length;i++) {
            Z[i]=znorm(Z[i]);
        }
        return Z;
    }

    //    zero-sum and l2-normalize a vector
    public static double[] l2norm(double[] v){
        double[] z = v.clone();
        double sum = Arrays.stream(z).reduce(0, Double::sum);
        double avg = sum / v.length;
        z = lib.sadd(v,-1*avg);
        double sumSquare = Arrays.stream(z).reduce(0, (a,b) -> a+b*b);
        double norm = FastMath.sqrt(sumSquare);
        z = lib.scale(z,1/norm);
        return z;
    }

    public static double[][] l2norm(double[][] Z) {
        for (int i=0;i<Z.length;i++) {
            Z[i]= l2norm(Z[i]);
        }
        return Z;
    }

    public static Complex[] toComplex(double[] x){
        return Arrays.stream(x)
                .mapToObj(v -> new Complex(v, 0))
                .toArray(Complex[]::new);
    }

    public static Complex[] fft(double[] x){
        return new FastFourierTransformer(DftNormalization.STANDARD).transform(x, TransformType.FORWARD);
    }

    public static <T> Stream<T> getStream(Iterable<T> collection, boolean parallel){
        return StreamSupport.stream(collection.spliterator(), parallel);
    }

    public static <T> Stream<T> getStream(Collection<T> collection, boolean parallel){
        if(parallel){
            return collection.parallelStream().parallel();
        }else{
            return collection.stream().sequential();
        }
    }

    public static <T> Stream<T> getStream(Stream<T> stream, boolean parallel){
        return parallel ? stream.parallel(): stream.sequential();
    }

    public static IntStream getStream(BitSet bitSet, boolean parallel){
        return parallel ? bitSet.stream().parallel(): bitSet.stream().sequential();
    }

    public static <T> Stream<T> getStream(T[] array, boolean parallel){
        if(parallel){
            return Arrays.stream(array).parallel();
        }else{
            return Arrays.stream(array).sequential();
        }
    }

    public static <T> int indexOf(T[] array, T value){
        for(int i=0;i<array.length;i++){
            if(array[i].equals(value)){
                return i;
            }
        }
        return -1;
    }

    public static <T> boolean contains(T[] array, T value){
        for(T v: array){
            if(v.equals(value)){
                return true;
            }
        }
        return false;
    }

    public static boolean contains(int[] array, int value){
        for(int v: array){
            if(v == value){
                return true;
            }
        }
        return false;
    }

    public static <T> T[] concat(T[] a, T[] b){
        T[] c = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static <T> T[] add(T[] a, T b){
        T[] c = Arrays.copyOf(a, a.length + 1);
        c[a.length] = b;
        return c;
    }

    public static double nextCauchy(Random random, double gamma){
        return gamma * Math.tan(Math.PI * (random.nextDouble() - 0.5));
    }

    public static double nanoToSec(long nano){return nano/1E9;}

    public static void printBar(Logger logger){
        logger.fine("-------------------------------------");
    }

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

    public static long getGCTime(){
        long msTime = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        return msTime * 1000000;
    }

    public static boolean isNumeric(String s) {
        return s.matches("[-+]?\\d*\\.?\\d+");
    }

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

    public static void stringToFile(String string, String outPath){
        try {
//            Optionally make root directories
            String rootdirname = outPath.substring(0, outPath.lastIndexOf("/"));
            new File(rootdirname).mkdirs();

            FileWriter resultWriter = new FileWriter(outPath);
            resultWriter.write(string);
            resultWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
