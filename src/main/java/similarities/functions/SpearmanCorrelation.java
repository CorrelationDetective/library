package similarities.functions;

import _aux.lib;
import core.RunParameters;

public class SpearmanCorrelation extends PearsonCorrelation {
    public SpearmanCorrelation(RunParameters runParameters) {
        super(runParameters);
    }

    public double[] preprocess(double[] data) {
        double[] ranks = lib.rank(data);
        return lib.l2norm(ranks);
    }
}
