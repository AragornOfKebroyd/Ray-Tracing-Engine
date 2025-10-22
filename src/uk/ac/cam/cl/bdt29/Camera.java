package uk.ac.cam.cl.bdt29;

import uk.ac.cam.cl.bdt29.data_structures.SquareMatrix3;
import uk.ac.cam.cl.bdt29.data_structures.Vector3;

public class Camera {

    public Vector3 origin;
    public SquareMatrix3 rotationMatrix;

    //Dimensions of image plane in pixels (px) - i.e. screen units
    private int width_px, height_px;

    // Dimensions of image plane in metres (m) - i.e. world units
    private double width_m, height_m;

    // Horizontal field of view in degrees
    double fov = 75;

    // Aspect ration of image - ratio of width to height
    double aspectRatio;

    // The distance in world units between each screen-space pixel
    private double x_step_m, y_step_m;

    public Camera(int width, int height, Vector3 origin, Vector3 rotation) {
        this.width_px = width;
        this.height_px = height;
        this.origin = origin;
        this.rotationMatrix = this.FindRotationMatrix(rotation);

        this.aspectRatio = ((double) width) / ((double) height);

        this.width_m = 2 * Math.tan(Math.toRadians(this.fov) / 2);
        this.height_m = width_m / aspectRatio;

        this.x_step_m = this.width_m / this.width_px;
        this.y_step_m = this.height_m / this.height_px;
    }

    private SquareMatrix3 FindRotationMatrix(Vector3 rotation) {
        double deg_to_rad = (2*Math.PI)/360;
        double cosThetaX = Math.cos(rotation.x * deg_to_rad);
        double sinThetaX = Math.sin(rotation.x * deg_to_rad);
        double cosThetaY = Math.cos(rotation.y * deg_to_rad);
        double sinThetaY = Math.sin(rotation.y * deg_to_rad);
        double cosThetaZ = Math.cos(rotation.z * deg_to_rad);
        double sinThetaZ = Math.sin(rotation.z * deg_to_rad);


        double[][] rotX = {
                {1,0,0},
                {0,cosThetaX,-sinThetaX},
                {0,sinThetaX,cosThetaX},
        };
        double[][] rotY = {
                {cosThetaY,0,sinThetaY},
                {0,1,0},
                {-sinThetaY,0,cosThetaY},
        };
        double[][] rotZ = {
                {cosThetaZ,-sinThetaZ,0},
                {sinThetaZ,cosThetaZ,0},
                {0,0,1},
        };
        SquareMatrix3 RX = new SquareMatrix3(rotX);
        SquareMatrix3 RY = new SquareMatrix3(rotY);
        SquareMatrix3 RZ = new SquareMatrix3(rotZ);

        // as a convention we will first rotate X, then Y, then Z
        SquareMatrix3 rotMatrix = RZ.multiply(RY).multiply(RX);

        return rotMatrix;
    }

    private static double START_REFRACTIVE_INDEX = 1;

    // Casts a ray through a supplied pixel coordinate
    public Ray castRay(int x, int y) {
        double x_pos = (x_step_m - width_m) / 2 + x * x_step_m;
        double y_pos = (y_step_m + height_m) / 2 - y * y_step_m;
        // rotate the direction
        Vector3 direction = new Vector3(x_pos, y_pos, 1).normalised();
        // it should still be normalised, but we will renormalise because of floating point errors
        Vector3 rotatedDirection = this.rotationMatrix.leftMultiplyVector(direction).normalised();

        return new Ray(this.origin, rotatedDirection, START_REFRACTIVE_INDEX);
    }

    public SquareMatrix3 getRotationMatrix() {
        return this.rotationMatrix;
    }
}

