package clustering;

import _aux.GeneralTest;
import _aux.lib;
import _aux.lists.FastArrayList;
import core.RunParameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import similarities.DistanceFunction;

import java.util.stream.Collectors;
import java.util.stream.IntStream;


//    Test our kmeans++ clustering algorithm for multiple distance functions by testing if every point is always assigned to the closest cluster
public class ClusteringTest extends GeneralTest {
    private static FastArrayList<Integer> dataIds;

    @Before
    public void setUp(){
        super.setUp();
        dataIds = new FastArrayList<>(IntStream.range(0, runParameters.getNVectors()).boxed().collect(Collectors.toList()));
    }

    public void testClustering(FastArrayList<Cluster> clusters, DistanceFunction distFunc){
//        Test if more than one cluster
        Assert.assertTrue(clusters.size() > 1);

        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            for (int pid: cluster.tmpPointsIdx) {
                double distToCluster = cluster.distances.get(pid);
                for (int j = i+1; j < clusters.size(); j++) {
                    double distOtherCluster = distFunc.dist(runParameters.getData()[pid], clusters.get(j).getCentroid());
                    Assert.assertTrue(distToCluster <= distOtherCluster);
                }
            }
        }
    }

    @Test
    public void testKMeansClustering(){
        FastArrayList<Cluster> clusters = Clustering.getKMeansMaxClusters(dataIds,Math.PI / 4, 5, runParameters);

//        Check number of clusters for good threshold
        Assert.assertEquals(5, clusters.size());

        clusters = Clustering.getKMeansMaxClusters(dataIds,5, 5, runParameters);

//        Check number of clusters for bad threshold
        Assert.assertEquals(1, clusters.size());

    }


    @Test
    public void testKmeansEuclidean() {
        runParameters.getSimMetric().distFunc = lib::euclidean;
        runParameters.computePairwiseDistances();
        FastArrayList<Cluster> clusters = Clustering.getKMeansMaxClusters(dataIds,1, 5, runParameters);
        testClustering(clusters, runParameters.getSimMetric().distFunc);
    }

    //Test angle distance
    @Test
    public void testKmeansAngle() {
        FastArrayList<Cluster> clusters = Clustering.getKMeansMaxClusters(dataIds,Math.PI / 4, 5, runParameters);
        testClustering(clusters, runParameters.getSimMetric().distFunc);
    }

//    Test clustering with geometric centroid
    @Test
    public void testGeometricCentroid(){
        runParameters.setGeoCentroid(true);

        FastArrayList<Cluster> clusters = Clustering.getKMeansMaxClusters(dataIds,Math.PI / 4, 5, runParameters);

        for (Cluster cluster: clusters) {
//            Centroid idx still set
            Assert.assertNull(cluster.centroidIdx);

//            No saved distances -- only computed on the fly with cache
            Assert.assertEquals(cluster.pointsIdx.length, cluster.distances.size());

//            Centroid is not actually a Parameters.data point
            for (double[] point: cluster.getPoints(runParameters.getData())) {
                Assert.assertNotEquals(lib.euclidean(point, cluster.getCentroid()), 0, 0.0001);
            }
        }
    }

//    Test non geometric centroid
    @Test
    public void testNonGeometricCentroid(){
        FastArrayList<Cluster> clusters = Clustering.getKMeansMaxClusters(dataIds,Math.PI / 4, 5, runParameters);

        for (Cluster cluster: clusters) {
//            Centroid idx still set
            Assert.assertNotNull(cluster.centroidIdx);

//            Centroid is actually a Parameters.data point
            Assert.assertEquals(lib.euclidean(runParameters.getData()[cluster.centroidIdx], cluster.getCentroid()), 0, 0.0001);
        }
    }


}
