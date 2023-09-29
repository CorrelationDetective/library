package algorithms.baselines;

import _aux.Pair;
import _aux.lib;
import core.RunParameters;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import similarities.SimEnum;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleBaseline extends Baseline{
    public SimpleBaseline(@NonNull RunParameters runParameters) {
        super(runParameters);
    }

    public SimpleBaseline(@NonNull String inputPath, @NonNull SimEnum simMetricName, int maxPLeft, int maxPRight) {
        super(inputPath, simMetricName, maxPLeft, maxPRight);
    }

    @RequiredArgsConstructor
    private class SimCacheValue{
        @NonNull public int[] left;
        @NonNull public int[] right;
        @NonNull public double sim;
    }

    ConcurrentHashMap<Long, SimCacheValue> simCache = new ConcurrentHashMap<>();



    @Override public void prepare(){}

//    Compute similarities exhaustively
    public double computeSimilarity(Pair<int[], int[]> candidate){
        double[] v1 = lib.rowSum(Arrays.stream(candidate.x).mapToObj(i -> runParameters.getData()[i]).toArray(double[][]::new));
        double[] v2 = lib.rowSum(Arrays.stream(candidate.y).mapToObj(i -> runParameters.getData()[i]).toArray(double[][]::new));
        double sim = runParameters.getSimMetric().sim(v1,v2);
        return sim;
    }

    public long hashCandidate(int[] left, int[] right){
        int[] hashList = Arrays.copyOf(left, left.length + right.length + 1);
        hashList[left.length] = -1;
        System.arraycopy(right, 0, hashList, left.length + 1, right.length);
        return hashList.hashCode();
    }
}
