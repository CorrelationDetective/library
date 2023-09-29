package clustering;

import _aux.lib;
import _aux.lists.FastArrayList;
import bounding.ClusterBounds;
import bounding.ClusterPair;
import core.RunParameters;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.util.FastMath;
import similarities.DistanceFunction;

import java.util.*;

public class Cluster {
    @Setter @Getter public int id;
    private DistanceFunction distFunc;

    //    runParameters
    public final RunParameters runParameters;

    public FastArrayList<Integer> tmpPointsIdx;
    @Getter public Integer[] pointsIdx;
    private int size = 0;

    @Getter HashMap<Integer, Double> distances;
    public boolean finalized = false;

//    Hypersphere statistics
    @Setter @Getter private Double radius;
    @Getter private double[] centroid;
    public Integer centroidIdx;

    //    Relations
    @Setter @Getter public Cluster parent;
    @Getter public FastArrayList<Cluster> children;
    @Setter @Getter public int level;

    //    Misc
    public Double score;

    //    For total correlation only currently (for saving entropy bounds)
    private ClusterPair entropyClusterPair;

    public Cluster(int centroidIdx, RunParameters runParameters) {
        this.centroidIdx = centroidIdx;
        this.distFunc = runParameters.getSimMetric().distFunc;
        tmpPointsIdx = new FastArrayList<>(runParameters.getNVectors());
        tmpPointsIdx.add(centroidIdx);
        this.runParameters = runParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cluster)) return false;
        Cluster cluster = (Cluster) o;
        return id == cluster.id;
    }

    public int size() {
        if (size == 0) {
            size = pointsIdx.length;
        }
        return size;
    }

    public boolean contains(Integer o) {
        if (finalized) {
            return lib.contains(pointsIdx, o);
        } else {
            throw new RuntimeException("Cluster is not finalized, not allowed to call contains yet");
        }
    }

    public String toString(){
        return Integer.toString(id);
    }

    public int get(int i) {
        return pointsIdx[i];
    }

    public void addPoint(int i){
        if (finalized) throw new RuntimeException("Cannot add points to a finalized cluster");
        tmpPointsIdx.add(i);
    }

    public void addChild(Cluster sc){
        if (children == null) children = new FastArrayList<>(runParameters.getKMeans());
        this.children.add(sc);
    }

    public double[][] getPoints(double[][] data){
        if (!finalized) throw new RuntimeException("Cannot get points from a non-finalized cluster");
        return Arrays.stream(pointsIdx).map(i -> data[i]).toArray(double[][]::new);
    }

//    Compute radius
    public void computeRadius(double[][] data, double[][] pairwiseDistances){
        double maxDist = 0;

//        Remove floating point error
        if (pointsIdx.length == 1){
            radius = 0.0;
            return;
        }

//        If using geometric Centroid, re-initialize all distances to centroid, otherwise, just pick largest distance from cached distances
        distances = new HashMap<>(pointsIdx.length, 1);

        for (int i = 0; i < pointsIdx.length; i++) {
            int pid = this.get(i);
            double dist = runParameters.isGeoCentroid() | pairwiseDistances == null ? distFunc.dist(data[pid], this.getCentroid()):
                     pairwiseDistances[pid][centroidIdx];
            distances.put(pid, dist);
            maxDist = FastMath.max(maxDist, dist);
        }
        radius = maxDist;
    }

//    Compute centroid
    public double[] computeGeometricCentroid(double[][] data) {
        return lib.elementwiseAvg(getPoints(data));
    }

    public void finalize(){
        if (finalized) throw new RuntimeException("Cluster already finalized");
        finalized = true;
        double[][] data = runParameters.getData();
        double[][] pairwiseDistances = runParameters.getPairwiseDistances();
        boolean geoCentroid = runParameters.isGeoCentroid();

//        Create final content array
        this.pointsIdx = tmpPointsIdx.toArray(new Integer[0]);

//        Initialize actual centroid
        if (geoCentroid) {
            centroid = computeGeometricCentroid(data);
            centroidIdx = null;
        } else {
            centroid = data[centroidIdx];
        }

//        Compute distances from centroid and determine radius
        computeRadius(data, pairwiseDistances);
    }

    public double getDistance(int pId){
//        If cluster is not final, compute distance from point centroid, otherwise get from local cache (geometric centroid)
        if (finalized && runParameters.isGeoCentroid()){
            return distances.get(pId);
        } else {
            return runParameters.getPairwiseDistances()[pId][centroidIdx];
        }
    }

    public Double getScore(){
        if (score==null) {
            score = this.radius / this.size();
        }
        return score;
    }

    public ClusterPair getEntropyBounds(double[][] pairwiseEntropies, boolean allowVectorOverlap){
        boolean discounting = runParameters.isDiscounting();
        int discountTopK = runParameters.getDiscountTopK();

        if (entropyClusterPair == null){
            double lb = Double.MAX_VALUE;
            double ub = 0;

//            (Optionally keep topk largest/smallest distances)
            TreeMap<Double, int[]> minDistances = discounting ? new TreeMap<>() : null;
            TreeMap<Double, int[]> maxDistances = discounting ? new TreeMap<>(Collections.reverseOrder()): null;

            for (Integer i: pointsIdx){
                double e = pairwiseEntropies[i][i];
                lb = FastMath.min(lb,e);
                ub = FastMath.max(ub,e);

                //                    Keep topk distances for discounting later
                if (discounting){
                    if (minDistances.size() < discountTopK){
                        minDistances.put(e, new int[]{i});
                    } else {
                        if (e < minDistances.lastKey()){
                            minDistances.pollLastEntry();
                            minDistances.put(e, new int[]{i});
                        }
                    }

                    if (maxDistances.size() < discountTopK){
                        maxDistances.put(e, new int[]{i});
                    } else {
                        if (e > maxDistances.lastKey()){
                            maxDistances.pollLastEntry();
                            maxDistances.put(e, new int[]{i});
                        }
                    }
                }
            }
            ClusterPair cp = new ClusterPair(new Cluster[]{this}, new Cluster[]{}, this.level, this.size(), allowVectorOverlap);
            cp.setBounds(new ClusterBounds(lb, ub, 0));

            int[][] minDistancesArr = minDistances == null ? null : minDistances.values().toArray(new int[minDistances.size()][]);
            int[][] maxDistancesArr = maxDistances == null ? null : maxDistances.values().toArray(new int[maxDistances.size()][]);
            cp.setMinDistances(minDistancesArr);
            cp.setMaxDistances(maxDistancesArr);
            entropyClusterPair = cp;
        }
        return entropyClusterPair;
    }
}
