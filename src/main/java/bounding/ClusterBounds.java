package bounding;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.math3.util.FastMath;

public class ClusterBounds {
    @NonNull @Getter @Setter private double LB;
    @NonNull @Getter @Setter private double UB;
    @NonNull @Getter private double maxLowerBoundSubset;
    @Getter private long timestamp;
    private double centerOfBounds;

    public ClusterBounds(double LB, double UB, double maxLowerBoundSubset){
        this.LB = LB;
        this.UB = UB;
        this.maxLowerBoundSubset = maxLowerBoundSubset;
        timestamp = System.nanoTime();
    }

    public String toString(){
        return String.format("%.2f, %.2f", LB, UB);
    }

    public double getCenterOfBounds(){
        if (centerOfBounds == 0){
            centerOfBounds = (LB + UB) / 2;
        }
        return centerOfBounds;
    }

    public void update(ClusterBounds other){
        LB = FastMath.max(LB, other.LB);
        UB = FastMath.min(UB, other.UB);
        maxLowerBoundSubset = FastMath.max(maxLowerBoundSubset, other.maxLowerBoundSubset);
        centerOfBounds = (LB + UB) / 2;
    }

    public ClusterBounds clone(){
        return new ClusterBounds(LB, UB, maxLowerBoundSubset);
    }
}