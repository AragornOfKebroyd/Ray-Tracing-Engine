package uk.ac.cam.cl.bdt29.data_structures;

import java.io.IOException;
import java.io.UncheckedIOException;

public class SquareMatrix3 {
    private double[][] Mat;
    private SquareMatrix3 Inverse;

    public SquareMatrix3() {
        this.Mat = new double[3][3];
    }

    public SquareMatrix3(double[][] matrix) {
        // check it is a 3x3 matrix
        boolean rows = matrix.length == 3;
        boolean columns = matrix[0].length == 3;
        if (!rows || !columns) {
            throw new UncheckedIOException(new IOException("must be a 3x3 matrix"));
        }
        this.Mat = matrix;
        this.calculateInverse();
    }

    public void setValue(int row, int column, double value) {
        if (row < 0 || row > 2 || column < 0 || column > 2) {
            throw new UncheckedIOException(new IOException("row and column must be between 0 and 2"));
        }
        Mat[row][column] = value;
    }

    public double getValue(int row, int column) {
        if (row < 0 || row > 2 || column < 0 || column > 2) {
            throw new UncheckedIOException(new IOException("row and column must be between 0 and 2"));
        }
        return Mat[row][column];
    }

    public double determinant() {
        return Mat[0][0] * minor(0,0) - Mat[0][1] * minor(0,1) + Mat[0][2] * minor(0,2);
    }

    private double minor(int row, int column) {
        double[] minor = new double[4];
        int index = 0;
        // gets the value that aren't in thar row or column
        for (int r=0;r<3;r++) {
            for (int c=0;c<3;c++) {
                // loop through the matrix, if r=row or c=col, skip over
                if (r==row || c==column) continue;
                minor[index] = Mat[r][c];
                index++;
            }
        }
        // calculate the determinant
        // now the minor will be [0,1
        //                        2,3]
        //0*3-1*2
        return minor[0] * minor[3] - minor[1] * minor[2];
    }

    public void transpose() {
        double[][] temp = this.Mat;
        for (int r=0;r<3;r++) {
            for (int c=0;c<3;c++) {
                // loop through the matrix, if r=row or c=col, skip over
                Mat[c][r] = temp[r][c];
            }
        }
    }

    public void cofactorise() {
        for (int r=0;r<3;r++) {
            for (int c=0;c<3;c++) {
                // loop through the matrix, if c+r is even, its +ve, else -ve
                double scale = ((c+r)%2==0) ? 1. : -1.;
                Mat[r][c] *= scale;
            }
        }
    }

    public void calculateInverse() {
        double det = this.determinant();
        SquareMatrix3 Minors = new SquareMatrix3();

        for (int r=0;r<3;r++) {
            for (int c=0;c<3;c++) {
                // loop through the matrix, calculate minors
                Minors.setValue(r,c,minor(r,c));
            }
        }

        Minors.cofactorise();
        Minors.transpose();
        Minors.scale(1/det);
        this.Inverse = Minors;
    }

    public void scale(double scalar) {
        for (int r=0;r<3;r++) {
            for (int c=0;c<3;c++) {
                // loop through the matrix, calculate minors
                Mat[r][c] *= scalar;
            }
        }
    }

    public SquareMatrix3 getInverse() {
        if (this.Inverse == null) {
            this.calculateInverse();
        }
        return this.Inverse;
    }

    public Vector3 getRow(int r) {
        return new Vector3(Mat[r][0],Mat[r][1],Mat[r][2]);
    }

    public Vector3 getColumn(int c) {
        return new Vector3(Mat[0][c],Mat[1][c],Mat[2][c]);
    }

    public Vector3 leftMultiplyVector (Vector3 v) {
        Vector3 row1 = getRow(0);
        Vector3 row2 = getRow(1);
        Vector3 row3 = getRow(2);

        return new Vector3(row1.dot(v), row2.dot(v), row3.dot(v));
    }

    public SquareMatrix3 multiply( SquareMatrix3 M) {
        Vector3 c0 = M.getColumn(0);
        Vector3 c1 = M.getColumn(1);
        Vector3 c2 = M.getColumn(2);

        Vector3 nc0 = this.leftMultiplyVector(c0);
        Vector3 nc1 = this.leftMultiplyVector(c1);
        Vector3 nc2 = this.leftMultiplyVector(c2);

        double[][] matInit = {
                {nc0.x,nc1.x,nc2.x},
                {nc0.y,nc1.y,nc2.y},
                {nc0.z,nc1.z,nc2.z}
        };

        return new SquareMatrix3(matInit);
    }
}

