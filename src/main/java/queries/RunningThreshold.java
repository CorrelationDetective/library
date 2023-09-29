package queries;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.logging.Logger;

@RequiredArgsConstructor
public class RunningThreshold {
    @Expose
    @NonNull public double threshold;
    private boolean locked = false;

    public String toString(){return String.format("%.6f", threshold);}

    public double get(){return threshold;}

    synchronized public void setThreshold(double newThreshold) {
        if (newThreshold > threshold) {
            threshold = newThreshold;
        }
    }

}
