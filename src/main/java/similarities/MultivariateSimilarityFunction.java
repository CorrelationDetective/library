package similarities;

import _aux.Pair;
import _aux.lib;
import _aux.lists.FastArrayList;
import _aux.projections.GaussianRandomProjection;
import _aux.projections.RandomProjection;
import bounding.ClusterBounds;
import bounding.ClusterCombination;
import bounding.ClusterPair;
import bounding.EmpiricalBoundFactor;
import clustering.Cluster;
import core.RunParameters;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.util.FastMath;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public abstract class MultivariateSimilarityFunction {
    @NonNull protected RunParameters runParameters;

    @Getter public boolean empiricalBounded = false;
    @Getter public boolean twoSided = true;

    @Getter public DistanceFunction distFunc = lib::euclidean;
    public double MAX_SIMILARITY = 1d;
    public double MIN_SIMILARITY = -1d;
    public double SIMRANGE = MAX_SIMILARITY - MIN_SIMILARITY;
    public RandomProjection transformer = new GaussianRandomProjection();

    public ConcurrentHashMap<Long, ClusterPair> pairwiseClusterCache;
    public ConcurrentHashMap<Long, Pair<double[], Double>> centroidCache;

//    Initializer after constructor to make sure that default parameters are overwritten
    protected void init(){
        int hashSize = runParameters.getHashSize();
        pairwiseClusterCache = new ConcurrentHashMap<>(hashSize, .4f);
        centroidCache = isEmpiricalBounded() ? null: new ConcurrentHashMap<>(hashSize, .5f);
        this.initTransformer();
    }

//    ----------------------- METHODS --------------------------------

    public boolean allowsDiscounting(){
        return isEmpiricalBounded();
    }

    protected void initTransformer(){
        //        Set transformer parameters
        transformer.setDelta(runParameters.getDimredDelta());
        transformer.setEpsilon(runParameters.getDimredEpsilon());
        transformer.setNComponents(runParameters.getDimredComponents());
        if (!runParameters.isRandom()){
            transformer.setRandomState((long) runParameters.getSeed());
        }
    }

    public String toString(){
        return this.getClass().getSimpleName();
    }

//    Compute multivariate similarity based on two aggregated vectors
    public abstract double sim(double[] x, double[] y);

//        Compute (multivariate) similarity for a combination of vectors
    public double sim(double[][] X, double[][] Y){
        double[] x = X.length == 1 ? X[0] : lib.rowSum(X);
        double[] y = Y.length == 1 ? Y[0] : lib.rowSum(Y);
        return sim(x, y);
    }

    public double[][] preprocess(double[][] data){
//        Normal preprocessing of data
        for (int i = 0; i < data.length; i++) {
            data[i] = preprocess(data[i]);
        }
        runParameters.setOrgData(Arrays.copyOf(data, data.length));

//        Dimensionality reduction if needed
        if (runParameters.isDimensionalityReduction()) {
            data = transformer.fitTransform(data);
        }
        return data;
    };
    public abstract double[] preprocess(double[] vector);

    public abstract double simToDist(double sim);
    public abstract double distToSim(double dist);

    public double getMaxApproximationSize(double ratio){
        return simToDist(MIN_SIMILARITY + ratio*SIMRANGE);
    }

    public void clearCache(){
        pairwiseClusterCache = new ConcurrentHashMap<>(runParameters.getHashSize(), .4f);
    }

    public double[][] computePairwiseDistances(double[][] data) {
        int n = data.length;
        boolean parallel = runParameters.isParallel();

        double[][] pairwiseDistances = new double[n][n];
        lib.getStream(IntStream.range(0, n).boxed(), parallel).forEach(i -> {
            lib.getStream(IntStream.range(i+1, n).boxed(), parallel).forEach(j -> {
                double dist = distFunc.dist(data[i], data[j]);
                pairwiseDistances[i][j] = dist;
                pairwiseDistances[j][i] = dist;
            });
        });
        return pairwiseDistances;
    }

    public ClusterPair theoreticalDistanceBounds(Cluster C1, Cluster C2){
        boolean geoCentroid = runParameters.isGeoCentroid();
        double[][] pairwiseDistances = runParameters.getPairwiseDistances();

        long ccID = hashPairwiseCluster(C1.id, C2.id);

        ClusterPair cp = pairwiseClusterCache.get(ccID);
        if (cp == null) {
//            Only compute if centroids are geometric, otherwise get from cache
            double centroidDistance = geoCentroid ? this.distFunc.dist(C1.getCentroid(), C2.getCentroid()) :
                    pairwiseDistances[C1.centroidIdx][C2.centroidIdx];
            double r1 = C1.getRadius();
            double r2 = C2.getRadius();

            double lbDist = FastMath.max(0, centroidDistance - r1 - r2);
            double ubDist = FastMath.max(0, centroidDistance + r1 + r2);

            cp = new ClusterPair(new Cluster[]{C1}, new Cluster[]{C2}, 0, C1.size() * C2.size(), runParameters.isAllowVectorOverlap());
            cp.updateBounds(new ClusterBounds(lbDist, ubDist, 0));
            pairwiseClusterCache.put(ccID, cp);
        }
        return cp;
    }

//    Made variable to be able to change it in subclasses (e.g., using entropies instead of distances in TotalCorrelation)
    protected double getDistanceForEmpiricalDistanceBounds(int i, int j){
        return runParameters.getPairwiseDistances()[i][j];
    }

    public ClusterPair empiricalDistanceBounds(Cluster C1, Cluster C2){
        boolean discounting = runParameters.isDiscounting();
        int discountTopK = runParameters.getDiscountTopK();

//        Assign s.t. C1.id > C2.id
        Cluster Cl = C1.id > C2.id ? C1 : C2;
        Cluster Cr = C1.id > C2.id ? C2 : C1;

        long ccID = hashPairwiseCluster(Cl.id, Cr.id);

        ClusterPair cp = pairwiseClusterCache.get(ccID);
        if (cp == null) {
//            Initialize cluster pair (descending ids)
            cp = new ClusterPair(new Cluster[]{Cl}, new Cluster[]{Cr}, 0, Cl.size() * Cr.size(), runParameters.isAllowVectorOverlap());

//            (Optionally keep topk largest/smallest distances)
            TreeMap<Double, int[]> minDistances = discounting ? new TreeMap<>() : null;
            TreeMap<Double, int[]> maxDistances = discounting ? new TreeMap<>(Collections.reverseOrder()): null;

            //   Compute empirical bounds
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            boolean singletons = Cl.size() == 1 && Cr.size() == 1;
            for (int i: Cl.pointsIdx) {
                for (int j : Cr.pointsIdx) {
                    double dist = getDistanceForEmpiricalDistanceBounds(i,j);

                    if (!singletons && discounting && i==j) continue; // Skip self-distances as part of discounting

//                Update bounds
                    if (dist < min) min = dist;
                    if (dist > max) max = dist;

//                    Keep topk distances for discounting later
                    if (discounting){
                        if (minDistances.size() < discountTopK){
                            minDistances.put(dist, new int[]{i,j});
                        } else {
                            if (dist < minDistances.lastKey()){
                                minDistances.pollLastEntry();
                                minDistances.put(dist, new int[]{i,j});
                            }
                        }

                        if (maxDistances.size() < discountTopK){
                            maxDistances.put(dist, new int[]{i,j});
                        } else {
                            if (dist > maxDistances.lastKey()){
                                maxDistances.pollLastEntry();
                                maxDistances.put(dist, new int[]{i,j});
                            }
                        }
                    }
                }
            }
            int[][] minDistancesArr = minDistances == null ? null : minDistances.values().toArray(new int[minDistances.size()][]);
            int[][] maxDistancesArr = maxDistances == null ? null : maxDistances.values().toArray(new int[maxDistances.size()][]);

//            Add to cache
            cp.updateBounds(new ClusterBounds(min,max, 0));
            cp.setMinDistances(minDistancesArr);
            cp.setMaxDistances(maxDistancesArr);
            pairwiseClusterCache.put(ccID, cp);
            int cpSize = (int) cp.size();
            runParameters.getStatBag().addStat(runParameters.getStatBag().getNLookups(), () -> cpSize);
        }
        return cp;
    }

//    Compute empirical similarity bounds for a set of clusters
    public abstract ClusterBounds empiricalSimilarityBounds(ClusterCombination CC);

    //    Compute empirical similarity directly, based on a list of bound factors
    public abstract double empiricalSimilarity(FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors, int pLeft, int pRight,
                                               double[][] pairwiseDistances);

    //    Compute theoretical similarity bounds for a set of clusters
    public ClusterBounds theoreticalSimilarityBounds(ClusterCombination CC){
        boolean dimensionalityReduction = runParameters.isDimensionalityReduction();
        boolean dimredCorrect = runParameters.isDimredCorrect();
        double dimredEpsilon = runParameters.getDimredEpsilon();

        Cluster[] LHS = CC.getLHS();
        Cluster[] RHS = CC.getRHS();

        double lowerDist = 0;
        double upperDist = 0;
        double maxLowerBoundSubset = this.MIN_SIMILARITY;
        if (LHS.length + RHS.length == 2){
            Cluster C1 = LHS[0];
            Cluster C2 = RHS.length == 1 ? RHS[0] : LHS[1];
            ClusterPair cp = theoreticalDistanceBounds(C1,C2);
            lowerDist = cp.getBounds().getLB();
            upperDist = cp.getBounds().getUB();
        } else {
            //        Get representation of aggregated clusters
            Pair<double[],Double> CXcr = aggCentroidRadius(LHS);
            double[] CXc = CXcr.x;
            double CXr = CXcr.y;

            Pair<double[],Double> CYcr = aggCentroidRadius(RHS);
            double[] CYc = CYcr.x;
            double CYr = CYcr.y;

            double centroidDistance = this.distFunc.dist(CXc, CYc);

//            Compute distance bounds
            lowerDist = FastMath.max(0,centroidDistance - CXr - CYr);
            upperDist = FastMath.max(0,centroidDistance + CXr + CYr);

            //             Correct distance using epsilon (i.e., maximum relative error)
            if (dimredCorrect && dimensionalityReduction && lowerDist > 0){
                double corrFactor = 1 / FastMath.sqrt(1 + dimredEpsilon);
                boolean singleton = upperDist - lowerDist < 1e-6;
                lowerDist = FastMath.max(0, lowerDist * corrFactor);

//            Only correct upper dist if singleton
                if (singleton) upperDist = FastMath.max(0, upperDist * corrFactor);
            }

            //        Now get maxLowerBoundSubset
            for (int i = 0; i < LHS.length; i++) {
                for (int j = 0; j < RHS.length; j++) {
                    ClusterPair cp = theoreticalDistanceBounds(LHS[i], RHS[j]);
                    maxLowerBoundSubset = Math.max(maxLowerBoundSubset, 1 / (1 + cp.getBounds().getUB()));
                }
            }
        }

        double lowerSim = distToSim(upperDist);
        double upperSim = distToSim(lowerDist);

        return new ClusterBounds(correctBound(lowerSim), correctBound(upperSim), maxLowerBoundSubset);
    }


    public ClusterBounds bound(ClusterCombination CC){
        if (CC.isBounded()) return CC.getBounds();

        ClusterBounds bounds;
        if (runParameters.isEmpiricalBounding() && this.isEmpiricalBounded()) {
            bounds = empiricalSimilarityBounds(CC);
        } else {
            bounds = theoreticalSimilarityBounds(CC);
        }
        CC.updateBounds(bounds);
        CC.setBounded(true);
        return bounds;
    }

    public long hashPairwiseCluster(int id1, int id2) {
        return ((long) id2 << 31) | id1;
    }

    public double correctBound(double bound){
        return FastMath.min(FastMath.max(bound, MIN_SIMILARITY), MAX_SIMILARITY);
    }

    public double[] aggCentroid(Cluster... clusters){
        double[] centroid = new double[runParameters.getNDimensions()];
        for (int i = 0; i < centroid.length; i++) {
            for (int j = 0; j < clusters.length; j++) {
                centroid[i] += clusters[j].getCentroid()[i];
            }
        }
        return centroid;
    }

    public Pair<double[], Double> aggCentroidRadius(Cluster... clusters){
        long globalClusterId = runParameters.getHC().globalClusterID;

        Pair<double[], Double> centroidRadius;
        if (clusters.length == 1){
            centroidRadius = new Pair<>(clusters[0].getCentroid(), clusters[0].getRadius());
        }
        else {
//            Check cache first
            long hash = ClusterCombination.hashClusterList(globalClusterId, clusters);
            centroidRadius = centroidCache.get(hash);
            if (centroidRadius == null) {
//            Compute centroid iteratively by dropping the last cluster and recursively computing the centroid of the rest
//            Base case: 2 clusters
                double[] centroid;
                double radius;
                if (clusters.length == 2) {
                    centroid = lib.add(clusters[0].getCentroid(), clusters[1].getCentroid());
                    radius = clusters[0].getRadius() + clusters[1].getRadius();
                } else {
                    Cluster[] rest = Arrays.copyOfRange(clusters, 0, clusters.length - 1);
                    Cluster last = clusters[clusters.length - 1];
                    Pair<double[], Double> restCentroidRadius = aggCentroidRadius(rest);
                    centroid = lib.add(restCentroidRadius.x, last.getCentroid());
                    radius = restCentroidRadius.y + last.getRadius();
                }
                centroidRadius = new Pair<>(centroid, radius);
                centroidCache.put(hash, centroidRadius);
            }
        }
        return centroidRadius;
    }

//    Sum of radii
    public double aggRadius(Cluster... clusters){
        double radius = 0;
        for (int i = 0; i < clusters.length; i++) {
            radius += clusters[i].getRadius();
        }
        return radius;
    }
}

