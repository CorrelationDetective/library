package bounding;

import _aux.lists.FastArrayList;
import clustering.Cluster;
import jdk.jfr.Description;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;
import java.util.TreeMap;

// Specialization of ClusterCombination with only two clusters
public class ClusterPair extends ClusterCombination {
    @Setter @Getter private int[][] maxDistances = null;
    @Setter @Getter private int[][] minDistances = null;

    public ClusterPair(@NonNull Cluster[] LHS, @NonNull Cluster[] RHS, int level, int size, boolean allowVectorOverlap) {
        super(LHS, RHS, level, size, allowVectorOverlap);
        assert LHS.length + RHS.length <= 2 : "ClusterPair can only have two clusters maximum";
    }

    public boolean isPair(){
        return this.getLHS().length == 1 && this.getRHS().length == 1;
    }

    public Cluster getLeft(){
        if (this.getLHS().length == 0){
            return null;
        }
        return this.getLHS()[0];
    }

    public Cluster getRight(){
        if (this.getRHS().length == 0){
            return null;
        }
        return this.getRHS()[0];
    }
}
