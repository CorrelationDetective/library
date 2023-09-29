package algorithms.baselines;

import _aux.Pair;
import bounding.ClusterCombination;
import core.RunParameters;
import clustering.Cluster;
import lombok.NonNull;
import similarities.SimEnum;

import java.util.Arrays;

public class SmartBaseline extends Baseline{
    Cluster[] singletonClusters;

    public SmartBaseline(@NonNull RunParameters runParameters) {
        super(runParameters);
    }

    public SmartBaseline(@NonNull String inputPath, @NonNull SimEnum simMetricName, int maxPLeft, int maxPRight) {
        super(inputPath, simMetricName, maxPLeft, maxPRight);
    }


    @Override
    public void prepare(){
        runParameters.computePairwiseDistances();
        makeSingletonClusters();
    }

    private void makeSingletonClusters(){
        int n = runParameters.getNVectors();
        singletonClusters = new Cluster[n];
        for(int i = 0; i < n; i++){
            Cluster c = new Cluster(i, runParameters);
            c.setId(i);
            c.finalize();
            singletonClusters[i] = c;
        }
        runParameters.getHC().globalClusterID = n;
    }

//    Compute similarities based on pairwise similarities (if possible)
    public double computeSimilarity(Pair<int[], int[]> candidate){
//        Create cluster combination
        Cluster[] LHS = Arrays.stream(candidate.x).mapToObj(i -> singletonClusters[i]).toArray(Cluster[]::new);
        Cluster[] RHS = Arrays.stream(candidate.y).mapToObj(i -> singletonClusters[i]).toArray(Cluster[]::new);
        ClusterCombination cc = new ClusterCombination(LHS, RHS, 0, 1, runParameters.isAllowVectorOverlap());
        runParameters.getSimMetric().bound(cc);
        return cc.getBounds().getLB();
    }
}
