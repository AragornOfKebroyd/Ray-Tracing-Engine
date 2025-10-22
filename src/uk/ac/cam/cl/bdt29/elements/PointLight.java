package uk.ac.cam.cl.bdt29.elements;

import uk.ac.cam.cl.bdt29.data_structures.ColorRGB;
import uk.ac.cam.cl.bdt29.data_structures.Vector3;

public class PointLight {

    // Point light parameters
    private Vector3 position;
    private ColorRGB colour;
    private double intensity;

    public PointLight(Vector3 position, ColorRGB colour, double intensity) {
        this.position = position;
        this.colour = colour;
        this.intensity = intensity;
    }

    public Vector3 getPosition() {
        return position;
    }

    public ColorRGB getColour() {
        return colour;
    }

    public double getIntensity() {
        return intensity;
    }

    // Get colour of light at a certain distance away
    public ColorRGB getIlluminationAt(double distance) {
        return colour.scale(intensity / (Math.PI * 4 * Math.pow(distance, 2)));
    }
}
