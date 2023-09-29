package similarities.functions;

import _aux.lib;
import core.RunParameters;

public class SpearmanCorrelation extends PearsonCorrelation {
    public SpearmanCorrelation(RunParameters runParameters) {
        super(runParameters);
    }

    @Override public double[][] preprocess(double[][] data) {
        double[][] ranks = new double[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            ranks[i] = lib.rank(data[i]);
        }
        lib.znorm(ranks);
        return ranks;
    }
}
