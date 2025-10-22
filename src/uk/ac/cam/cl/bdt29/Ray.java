package uk.ac.cam.cl.bdt29;

import uk.ac.cam.cl.bdt29.data_structures.Vector3;

public class Ray {

    // Ray parameters
    private Vector3 origin, direction;

    // the refractive index that the ray is currently in
    private double refractiveIndex;

    public Ray(Vector3 origin, Vector3 direction, double refractiveIndex) {
        this.origin = origin;
        this.direction = direction;
        this.refractiveIndex = refractiveIndex;
    }

    public Vector3 getOrigin() {
        return origin;
    }

    public Vector3 getDirection() {
        return direction;
    }

    public double getRefractiveIndex() {
        return refractiveIndex;
    }
    // Determine position for certain scalar parameter distance i.e. (origin + direction * distance)
    public Vector3 evaluateAt(double distance) {
        return origin.add(direction.scale(distance));
    }
}

