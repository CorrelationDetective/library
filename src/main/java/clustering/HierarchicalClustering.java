package clustering;

import _aux.lib;
import _aux.lists.FastArrayList;
import core.RunParameters;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HierarchicalClustering {
    public int globalClusterID = 0;
    public FastArrayList<LinkedList<Cluster>> clusterTree;
    public Cluster[] singletonClusters;
    public Cluster[] allClusters;

    private RunParameters runParameters;
    private int kMeans;
    private int maxLevels;
    private double startEpsilon;
    private double epsilonMultiplier;
    private int clusteringRetries;
    private boolean geoCentroid;
    private ClusteringAlgorithmEnum clusteringAlgorithm;
    private int breakFirstKLevelsToMoreClusters;
    private int n;


    public HierarchicalClustering(RunParameters runParameters){
        this.runParameters = runParameters;
        this.kMeans = runParameters.getKMeans();
        this.maxLevels = runParameters.getMaxLevels();
        this.startEpsilon = runParameters.getStartEpsilon();
        this.epsilonMultiplier = runParameters.getEpsilonMultiplier();
        this.clusteringRetries = runParameters.getClusteringRetries();
        this.geoCentroid = runParameters.isGeoCentroid();
        this.clusteringAlgorithm = runParameters.getClusteringAlgorithm();
        this.breakFirstKLevelsToMoreClusters = runParameters.getBreakFirstKLevelsToMoreClusters();
        this.n = runParameters.getNVectors();

        this.clusterTree = new FastArrayList<>(maxLevels);
        for (int i = 0; i < maxLevels; i++) {
            this.clusterTree.add(new LinkedList<>());
        }
        this.singletonClusters = new Cluster[n];
    }

    public void run(){
//        Create root cluster
        Cluster root = new Cluster(0,runParameters);
        root.setId(globalClusterID++);
        root.setLevel(0);
        for (int i = 1; i <n; i++) {
            root.addPoint(i);
        }

//        Finalize root and add to cluster tree and allClusters
        root.finalize();

//        root.setRadius(Parameters.runParameters.getSimMetric().simToDist(Parameters.runParameters.getSimMetric().MIN_SIMILARITY));
        clusterTree.get(0).add(root);

//        Create clustering tree
        recursiveClustering(root,startEpsilon);
    }

    public void recursiveClustering(Cluster c, double distThreshold){
        FastArrayList<Cluster> subClusters = makeAndGetSubClusters(c, distThreshold);

        double nextThreshold = 0d;

        for (Cluster sc : subClusters) {
//            Depth first indexing of clusters (for anti-symmetry checks)
            sc.setId(globalClusterID++);

        // If under maxlevel, keep multiplying epsilon, otherwise change threshold such that we only get singletons
            if (sc.level <maxLevels - 2) {
                nextThreshold = sc.getRadius() *epsilonMultiplier;
            }
            if (sc.level <maxLevels - 1 && sc.size() > 1) {
                recursiveClustering(sc,nextThreshold);
            }
        }
    }

    public FastArrayList<Cluster> makeAndGetSubClusters(Cluster c, double epsilon){
        FastArrayList<Cluster>[] subClustersPerTry = new FastArrayList[clusteringRetries];

//        Try different clustering runs in parallel, return score for each run
        FastArrayList<Double> clusteringScores = new FastArrayList<>(lib.getStream(IntStream.range(0,clusteringRetries).boxed(),runParameters.isParallel()).unordered()
                .map(i -> {
                    FastArrayList<Integer> points = new FastArrayList<>(Arrays.asList(c.pointsIdx));
                    points.shuffle(runParameters.getRandomGenerator());

                    //  Variable cluster parameters
                    int nDesiredClusters = kMeans;
                    if (epsilon <= 0 || c.level ==maxLevels) nDesiredClusters = c.size();
                    if (c.level < breakFirstKLevelsToMoreClusters) nDesiredClusters *= 5;

                    FastArrayList<Cluster> localSubClusters;
                    switch (clusteringAlgorithm) {
                        default:
                        case KMEANS:
                            localSubClusters = Clustering.getKMeansMaxClusters(points, epsilon, nDesiredClusters, runParameters);
                            break;
                    }
                    subClustersPerTry[i] = new FastArrayList<>(localSubClusters);
                    return localSubClusters.stream().mapToDouble(Cluster::getScore).sum();
                }).collect(Collectors.toList()));

//        Get clustering with best score
        double bestScore = clusteringScores.get(0);
        FastArrayList<Cluster> bestSubClusters = subClustersPerTry[0];

        for (int i = 1; i <clusteringRetries; i++) {
            if (clusteringScores.get(i) < bestScore) {
                bestScore = clusteringScores.get(i);
                bestSubClusters = subClustersPerTry[i];
            }
        }


//        Set parent-child relationships
        c.children = new FastArrayList<>(bestSubClusters.size());
        for (Cluster sc : bestSubClusters) {
            sc.setParent(c);
            c.addChild(sc);

//            Update tree statistics
            sc.setLevel(c.level + 1);

            this.clusterTree.get(sc.level).add(sc);

            if (sc.size() == 1){
                singletonClusters[geoCentroid ? sc.pointsIdx[0]: sc.centroidIdx] = sc;
            }
        }

        return bestSubClusters;
    }

    public Cluster[] getAllClusters(){
        if (this.allClusters != null) return this.allClusters;

        allClusters = new Cluster[this.globalClusterID];
//        Iterate over all cluster levels and add to list (considering cid as index in list)
        for (LinkedList<Cluster> level : this.clusterTree){
            level.forEach(c -> allClusters[c.id] = c);
        }
        return allClusters;
    }

    public Cluster getCluster(int cid){
        return getAllClusters()[cid];
    }
}
