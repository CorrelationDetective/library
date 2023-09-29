package clustering;

import _aux.GeneralTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import similarities.functions.EuclideanSimilarity;

import java.util.LinkedList;

//    Test our kmeans++ clustering algorithm for multiple distance functions by testing if every point is always assigned to the closest cluster
public class HierarchicalClusteringTest extends GeneralTest {
        private HierarchicalClustering HC;


    @Before
    public void setUp(){
        super.setUp();
        runParameters.setBreakFirstKLevelsToMoreClusters(1);
        runParameters.setClusteringRetries(5);
        runParameters.setKMeans(5);
        runParameters.setMaxLevels(20);
        runParameters.setStartEpsilon(1/.6 - 1);
        runParameters.setEpsilonMultiplier(0.8);
        runParameters.setSimMetric(new EuclideanSimilarity(runParameters));

        runParameters.init();

        HC = new HierarchicalClustering(runParameters);
        HC.run();
    }

//    Run all tests (with setup once)
    @Test
    public void testHierarchicalClustering(){
        testRootCluster();
        testFirstLevelsClusterBreak();
        testNumberOfClustersPerLevel();
        testMaxClusterLevels();
        testLeaves();
    }

    //    Test root cluster size
    public void testRootCluster(){
        Assert.assertNotNull(HC.clusterTree.get(0));
        Assert.assertNotNull(HC.clusterTree.get(0).getFirst());
        Cluster root = HC.clusterTree.get(0).getFirst();
        Assert.assertTrue(root.finalized);
        Assert.assertEquals(runParameters.getNVectors(), root.pointsIdx.length);
    }

//    Test first k levels cluster breaks
    public void testFirstLevelsClusterBreak(){
        for(int i=0; i<runParameters.getBreakFirstKLevelsToMoreClusters(); i++){
            LinkedList<Cluster> currentKCLevel = HC.clusterTree.get(i);
            for(Cluster cluster : currentKCLevel){
                Assert.assertTrue(cluster.children.size() > runParameters.getBreakFirstKLevelsToMoreClusters());
            }
        }
    }

//    Test number of clusters per level
    public void testNumberOfClustersPerLevel(){
        for(int i=runParameters.getBreakFirstKLevelsToMoreClusters(); i < HC.clusterTree.size(); i++){
            LinkedList<Cluster> currentKCLevel = HC.clusterTree.get(i);
            for(Cluster cluster : currentKCLevel){
                Assert.assertTrue(cluster.finalized);

                if (cluster.getChildren() != null){
                    Assert.assertTrue(cluster.getChildren().size() <= runParameters.getKMeans());
                } else {
                    Assert.assertEquals(1, cluster.pointsIdx.length);
                }
            }
        }
    }

//    Test max cluster levels
    public void testMaxClusterLevels(){
        Assert.assertTrue(HC.clusterTree.size() <= runParameters.getMaxLevels());
    }

//    Test all leaves are singletons
    public void testLeaves(){
        LinkedList<Cluster> lastLevel = HC.clusterTree.get(HC.clusterTree.size()-1);
        for (Cluster cluster: lastLevel){
            Assert.assertTrue(cluster.finalized);
            Assert.assertEquals(1, cluster.pointsIdx.length);
        }
    }
}
