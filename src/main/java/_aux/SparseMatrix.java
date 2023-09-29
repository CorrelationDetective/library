package _aux;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
public class SparseMatrix {
    private int nRows;
    private int nCols;
    private List<Integer> rowIndices;
    private List<Integer> colIndices;
    private List<Double> values;

    public SparseMatrix(int nRows, int nCols, List<Integer> rowIndices, List<Integer> colIndices, List<Double> values) {
        this.nRows = nRows;
        this.nCols = nCols;
        this.rowIndices = rowIndices;
        this.colIndices = colIndices;
        this.values = values;
    }

    public SparseMatrix(int nRows, int nCols) {
        this.nRows = nRows;
        this.nCols = nCols;
        this.rowIndices = new ArrayList<>();
        this.colIndices = new ArrayList<>();
        this.values = new ArrayList<>();
    }

    public void add(int row, int col, double value) {
        this.rowIndices.add(row);
        this.colIndices.add(col);
        this.values.add(value);
    }

    public double get(int row, int col) {
        for (int i = 0; i < this.rowIndices.size(); i++) {
            if (this.rowIndices.get(i) == row && this.colIndices.get(i) == col) {
                return this.values.get(i);
            }
        }
        return 0.0;
    }

    public void set(int row, int col, double value) {
        for (int i = 0; i < this.rowIndices.size(); i++) {
            if (this.rowIndices.get(i) == row && this.colIndices.get(i) == col) {
                this.values.set(i, value);
                return;
            }
        }
        this.add(row, col, value);
    }

    public SparseMatrix transpose() {
        SparseMatrix transpose = new SparseMatrix(this.nCols, this.nRows);
        for (int i = 0; i < this.rowIndices.size(); i++) {
            transpose.add(this.colIndices.get(i), this.rowIndices.get(i), this.values.get(i));
        }
        return transpose;
    }

    public double[][] toDense() {
        double[][] dense = new double[this.nRows][this.nCols];
        for (int i = 0; i < this.rowIndices.size(); i++) {
            dense[this.rowIndices.get(i)][this.colIndices.get(i)] = this.values.get(i);
        }
        return dense;
    }

    public static SparseMatrix fromDense(double[][] dense) {
        SparseMatrix sparse = new SparseMatrix(dense.length, dense[0].length);
        for (int i = 0; i < dense.length; i++) {
            for (int j = 0; j < dense[0].length; j++) {
                if (dense[i][j] != 0) {
                    sparse.add(i, j, dense[i][j]);
                }
            }
        }
        return sparse;
    }
}
