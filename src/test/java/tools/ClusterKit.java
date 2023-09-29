package tools;

import _aux.Pair;
import _aux.lib;
import clustering.Cluster;
import clustering.HierarchicalClustering;
import core.RunParameters;
import similarities.SimEnum;

public class ClusterKit {
    public Cluster C1;
    public Cluster C2;
    public Cluster C3;
    public Cluster C4;
    private RunParameters runParameters;
    private HierarchicalClustering HC;

    public ClusterKit(RunParameters runParameters){
        this.runParameters = runParameters;
        HC = runParameters.initializeHC();
        readClusters("src/test/resources/clusters/dim100/");
        runParameters.computePairwiseDistances();
    }

    private Pair<Cluster, double[][]> readCluster(String filename, int i){
//        Read vectors from file
        double[][] vectors = lib.l2norm(lib.readMatrix(filename));

        runParameters.setNVectors(vectors.length);

//        Add points to cluster
        Cluster C = new Cluster(i++, runParameters);
        for (int j = 1; j < vectors.length; j++) {
            C.addPoint(i++);
        }
        return new Pair<>(C, vectors);
    }

    private void readClusters(String rootdir) {
        int n = 0;

        String c1filename = rootdir + "midClusters0.csv";
        String c2filename = rootdir + "midClusters1.csv";
        String c3filename = rootdir + "midClusters2.csv";
        String c4filename = rootdir + "midClusters3.csv";

        Pair<Cluster, double[][]> out = readCluster(c1filename,n);
        C1 = out.x;
        C1.setId(1);
        double[][] c1data = out.y;
        n += c1data.length;

        out = readCluster(c2filename,n);
        C2 = out.x;
        C2.setId(2);
        double[][] c2data = out.y;
        n += c2data.length;

        out = readCluster(c3filename,n);
        C3 = out.x;
        C3.setId(3);
        double[][] c3data = out.y;
        n += c3data.length;

        out = readCluster(c4filename, n);
        C4 = out.x;
        C4.setId(4);
        double[][] c4data = out.y;
        n += c4data.length;

        double[][] data = new double[n][c1data[0].length];
        for (int i = 0; i < c1data.length; i++) {
            data[i] = c1data[i];
        }
        for (int i = 0; i < c2data.length; i++) {
            data[i + c1data.length] = c2data[i];
        }
        for (int i = 0; i < c3data.length; i++) {
            data[i + c1data.length + c2data.length] = c3data[i];
        }
        for (int i = 0; i < c4data.length; i++) {
            data[i + c1data.length + c2data.length + c3data.length] = c4data[i];
        }
        data = lib.l2norm(data);
        runParameters.setNVectors(data.length);

        HC.globalClusterID = 3;

        C1.finalize();
        C2.finalize();
        C3.finalize();
        C4.finalize();
    }
}
