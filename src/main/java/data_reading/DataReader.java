package data_reading;

import _aux.Pair;
import _aux.lib;
import _aux.lists.FastArrayList;
import org.apache.commons.math3.util.FastMath;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class DataReader {
    public static Pair<String[], double[][]> getData(String dataType, String inputPath, int n, int m, int partition) {
        String dataPath;
        Pair<String[], double[][]> dataPair;

        //        ---------------------------- DATA READING ------------------------------------------

        switch (dataType){
            case "weather_slp": {
                dataPath = String.format("%s/weather/1620_daily/slp_1620daily_filled_T.csv", inputPath);
                dataPair = DataReader.readRowMajorCSV(dataPath, n, m, true, partition);
            } break;
            case "weather_tmp": {
                dataPath = String.format("%s/weather/1620_daily/tmp_1620daily_filled_T.csv", inputPath);
                dataPair = DataReader.readRowMajorCSV(dataPath, n, m, true, partition);
            } break;
            case "weather_comet": {
                dataPath = String.format("%s/weather/comet/comet_slp_row.csv", inputPath);
                dataPair = DataReader.readRowMajorCSV(dataPath, n, m, true, partition);
            } break;
            case "fmri": {
                int[] n_steps = new int[]{237, 509, 1440, 3152, 9700};
                String[] dataPaths = new String[]{
                        String.format("%s/fmri/fmri_res8x10x8-237_row.csv", inputPath),
                        String.format("%s/fmri/fmri_res11x13x11-509_row.csv", inputPath),
                        String.format("%s/fmri/fmri_res16x19x16-1440_row.csv", inputPath),
                        String.format("%s/fmri/fmri_res22x26x22-3152_row.csv", inputPath),
                        String.format("%s/fmri/fmri_res32x38x32-9700_row.csv", inputPath),
                };

                dataPath = dataPaths[FastMath.min(n, 4)];

                n = n_steps[n];
                dataPair = DataReader.readRowMajorCSV(dataPath, n, m, n < 9700, partition);
            } break;
            case "crypto": {
                dataPath = String.format("%s/crypto/crypto_3h_logreturn_new.csv", inputPath);
                dataPair = DataReader.readRowMajorCSV(dataPath, n, m, true, partition);
            }break;
            case "deep": {
                dataPath = String.format("%s/deep/deep10K.csv", inputPath);
                dataPair = DataReader.readRowMajorCSV(dataPath, n, m, true, partition);
            } break;
            case "stocklog": default: {
                dataPath = String.format("%s/stock/1620daily/stocks_1620daily_logreturn_deduped.csv", inputPath);
                dataPair = DataReader.readRowMajorCSV(dataPath, n, m, true, partition);
            } break;
            case "stock": {
                dataPath = String.format("%s/stock/1620daily/stocks_1620daily_interpolated_deduped.csv", inputPath);
                dataPair = DataReader.readRowMajorCSV(dataPath, n, m, true, partition);
            } break;
            case "random": {
                dataPath = String.format("%s/random/random_n50000_m1000_seed0.csv", inputPath);
                dataPair = DataReader.readRowMajorCSV(dataPath, n, m, true, partition);
            } break;
            case "stocklog_old": {
                dataPath = String.format("%s/stock/202004_10min/logreturn/base.csv", inputPath);
                dataPair = DataReader.readColumnMajorCSV(dataPath, n, m, true, partition);
            } break;
            case "stock_old": {
                dataPath = String.format("%s/stock/202004_10min/interpolated/base.csv", inputPath);
                dataPair = DataReader.readColumnMajorCSV(dataPath, n, m, true, partition);
            } break;
        }

        return dataPair;
    }


//    TODO MAKE THIS ADAPTIVE -- AUTOMATICALLY DETECT COLUMN/ROW MAJOR DATA
    public static Pair<String[], double[][]> readColumnMajorCSV(String path, int n, int maxDim, boolean skipVar, int partition) {
        String delimiter = ",";
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));

//            Get Header
            String firstLine = br.readLine();
            String[] header = firstLine.split(delimiter);
            int maxN = header.length;
            int effN = FastMath.min(maxN, n);

//            Parse data
            LinkedList<Double>[] rows = new LinkedList[maxN];
            IntStream.range(0, maxN).forEach(i -> rows[i] = new LinkedList<>());

//            Skip all non-partition rows
            for (int i = 0; i < maxDim*partition; i++) {
                br.readLine();
            }
            int m=0;
            while (br.ready() & m < maxDim) {
                String[] line = br.readLine().split(delimiter);
//                Distribute values over columns
                for (int i = 0; i < maxN; i++) {
                    if (line[i].equals("nan")) {
                        System.out.println("nan value");
                    }

                    rows[i].add(Double.parseDouble(line[i]));
                }
                m++;
            }

            int effDim = rows[0].size();

//            Remove the rows that have too low variance (if skipvar on)
            double[][] finalRows = new double[effN][effDim];
            if (skipVar) {
                int i=0;
                int j=0;
                while (i < effN) {
                    double[] row = rows[j].stream().mapToDouble(Double::doubleValue).toArray();
                    j++;
                    if (lib.std(row) >= 1e-3 && lib.max(row) - lib.min(row) > 0) {
                        finalRows[i] = row; i++;
                    }
                }
            } else{
                finalRows = IntStream.range(0, effN).mapToObj(i -> rows[i].stream().mapToDouble(Double::doubleValue).toArray()).toArray(double[][]::new);
            }

            return new Pair<>(header, finalRows);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Pair<String[], double[][]> readRowMajorCSV(String path, int maxN, int maxDim, boolean skipVar, int partition) {
        String delimiter = ",";

        try {
            BufferedReader br = new BufferedReader(new FileReader(path));

//            Discard first line
            br.readLine().split(delimiter);

//            Get Header
            String[] headers = new String[maxN];

//            Parse data
            FastArrayList<double[]> rows = new FastArrayList<>(maxN);
            int n = 0;
            while (br.ready() & n < maxN) {
                String[] line = br.readLine().split(delimiter);

                maxDim = FastMath.min(line.length - 1, maxDim);
                double[] row = IntStream.rangeClosed(partition*maxDim + 1,(partition+1)*maxDim).mapToDouble(i -> Double.parseDouble(line[i])).toArray();

//                Skip rows if variance is too low
                if (!skipVar || (lib.std(row) >= 1e-3 && lib.max(row) - lib.min(row) > 0)){
                    rows.add(row);
                    headers[n] = line[0];
                    n++;
                }
            }

//            Convert rows FastArrayList to array
            int effN = rows.size();

            double[][] res = rows.toArray( new double[effN][maxDim]);

            return new Pair<>(headers, res);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
