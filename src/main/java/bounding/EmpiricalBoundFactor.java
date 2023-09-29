package bounding;

import core.RunParameters;
import lombok.Getter;
import org.apache.commons.math3.util.FastMath;

/** Factor (i.e., 'ingredient') of an empirical bound of a cluster combination.
 * Primarily used for BOUND DISCOUNTING.
 * These are wrapper objects that contain the following info;
 * - The cluster pair, impacting the value of the factor
 * - Positive, boolean indicating the impact of this factors DISTANCES on similarity bounds of the cluster combination.
 * - Locations, the location of each cluster in the cluster pair in the list of clusters of the cluster combination.
 * - Rank of the factor, indicating the rank of the current extrema distance (i.e., 1st, 2nd, ...).
  */

@Getter
public class EmpiricalBoundFactor {
    private final ClusterPair clusterPair;
    private final boolean positiveImpact;
    private final int[] locations;
    private int rank = 0;

    public EmpiricalBoundFactor(ClusterPair clusterPair, boolean positiveImpact, int[] locations) {
        this.clusterPair = clusterPair;
        this.positiveImpact = positiveImpact;
        this.locations = locations;
    }

    public String toString(){
        return clusterPair.toString() + ", rank: " + rank;
    }

    public void incrementRank(){
        this.rank++;
    }
    public void addToRank(int n){
        this.rank += n;
    }

    public void decrementRank(){
        this.rank = FastMath.max(0, this.rank - 1);
    }

    public void decrementRank(int n){
        this.rank = FastMath.max(0, this.rank - n);
    }

    public void setRank(int rank){
        this.rank = rank;
    }

    public int[][] getExtremaPairs(){
        return positiveImpact ? clusterPair.getMaxDistances() : clusterPair.getMinDistances();
    }

    public int[] getExtremaPair(){
        int[][] extremaPairs = getExtremaPairs();
        return extremaPairs[rank];
    }

    public double getExtremaDistance(double[][] distanceMatrix){
        int[] extremaPair = getExtremaPair();
        if (extremaPair.length == 1){
            return distanceMatrix[extremaPair[0]][extremaPair[0]];
        } else {
            return distanceMatrix[extremaPair[0]][extremaPair[1]];
        }
    }

    //    Check if this factor is at max rank (i.e., rank == exDistances.length)
    public boolean isMaxRank(int discountStep){
        int[][] extremaPairs = getExtremaPairs();
        return rank + discountStep >= extremaPairs.length;
    }
}
