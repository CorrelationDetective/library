package _aux;

import _aux.Stage;
import _aux.Tuple3;
import _aux.lib;
import _aux.lists.FastArrayList;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;

import java.util.function.Supplier;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class StageRunner {
//    <Name, Duration, ExpectedDuration>
    public final FastArrayList<Stage> stageDurations = new FastArrayList<>(10);
    public double stageDurationSum = 0;

    public <T> T run(String name, Supplier<T> stage, StopWatch stopWatch) {
        Logger.getGlobal().fine(String.format("----------- %d. %s --------------",stageDurations.size(), name));

        try {
            return stage.get();
        } finally {
            logDuration(name, stopWatch);
        }

    }



    public void run(String name, Runnable stage, StopWatch stopWatch) {
        Logger.getGlobal().fine(String.format("----------- %d. %s --------------",stageDurations.size(), name));

        try {
            stage.run();
        } finally {
            logDuration(name, stopWatch);

        }
    }

    private void logDuration(String name, StopWatch stopWatch){
        stopWatch.split();
        double splitTime = lib.nanoToSec(stopWatch.getSplitNanoTime());
        double splitDuration = splitTime - stageDurationSum;
        stageDurations.add(new Stage(name, splitDuration));
        stageDurationSum += splitDuration;
    }
}
