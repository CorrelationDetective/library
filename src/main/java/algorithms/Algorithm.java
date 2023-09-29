package algorithms;

import com.google.gson.JsonObject;
import core.RunParameters;
import core.StatBag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import queries.ResultSet;
import similarities.SimEnum;

import java.util.logging.Logger;

@RequiredArgsConstructor
public abstract class Algorithm {
    @NonNull public RunParameters runParameters;

    public Algorithm(String inputPath, SimEnum simMetric, int maxPLeft, int maxPRight){
        this.runParameters = new RunParameters(inputPath, simMetric, maxPLeft, maxPRight);
    }

    public abstract ResultSet run();

    public void printStats(){
        Logger.getGlobal().fine("----------- Run statistics --------------");
        runParameters.getStatBag().printStats();
    }

    public StatBag getStatBag(){
        return runParameters.getStatBag();
    }

    public String getRunJson(){
//        Create a json object with stats, parameters, and resultset
        JsonObject tmpObject = new JsonObject();
        tmpObject.add("statistics", runParameters.getStatBag().toJsonElement());
        tmpObject.add("parameters", runParameters.toJsonElement());
        tmpObject.add("results",runParameters.getResultSet().toJsonElement(runParameters.getGson()));

//        Return the json object as a string
        return tmpObject.toString();
    }
}
