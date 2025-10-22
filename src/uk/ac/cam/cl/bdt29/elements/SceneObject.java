package uk.ac.cam.cl.bdt29.elements;

import uk.ac.cam.cl.bdt29.Ray;
import uk.ac.cam.cl.bdt29.RaycastHit;
import uk.ac.cam.cl.bdt29.data_structures.ColorRGB;
import uk.ac.cam.cl.bdt29.data_structures.Vector3;

public abstract class SceneObject {

    // The diffuse colour of the object
    protected ColorRGB colour;

    // Coefficients for calculating Phong illumination
    protected double phong_kD, phong_kS, phong_alpha;

    // How reflective this object is
    protected double reflectivity;

    // How much light is transmitted through the object (between 0 and 1)
    protected ColorRGB transmittance;
    protected double refractive_index;

    protected SceneObject() {
        colour = new ColorRGB(1);
        phong_kD = phong_kS = phong_alpha = reflectivity = 0;
        refractive_index = 1.5;
        transmittance = new ColorRGB(0);
    }

    // Intersect this object with ray
    public abstract RaycastHit intersectionWith(Ray ray);

    // Get normal to object at position
    public abstract Vector3 getNormalAt(Vector3 position);

    public ColorRGB getColourAt(Vector3 position) {
        return colour;
    }

    public void setColour(ColorRGB colour) {
        this.colour = colour;
    }

    public double getPhong_kD() {
        return phong_kD;
    }

    public double getPhong_kS() {
        return phong_kS;
    }

    public double getPhong_alpha() {
        return phong_alpha;
    }

    public double getReflectivity() {
        return reflectivity;
    }

    public boolean isTransmissive() { return !transmittance.isZero(); }

    public ColorRGB getTransmittance() { return transmittance; }

    public double getRefractiveIndex() {
        return refractive_index;
    }

    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }
}

