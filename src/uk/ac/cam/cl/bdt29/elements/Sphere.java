package uk.ac.cam.cl.bdt29.elements;

import uk.ac.cam.cl.bdt29.Ray;
import uk.ac.cam.cl.bdt29.RaycastHit;
import uk.ac.cam.cl.bdt29.data_structures.ColorRGB;
import uk.ac.cam.cl.bdt29.data_structures.Tuple;
import uk.ac.cam.cl.bdt29.data_structures.Vector3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Sphere extends SceneObject {

    // Sphere coefficients
    public static final double DEFAULT_SPHERE_KD = 0.8;
    public static final double DEFAULT_SPHERE_KS = 1.2;
    public static final double DEFAULT_SPHERE_ALPHA = 10;
    public static final double DEFAULT_SPHERE_REFLECTIVITY = 0.3;
    public static final double DEFAULT_SPHERE_REFRACTIVE_INDEX = 0.0;
    // The world-space position of the sphere
    private Vector3 position;

    public Vector3 getPosition() {
        return position;
    }

    // The radius of the sphere in world units
    private final double radius;

    // bumpy and textured booleans
    public boolean bumpy = false;
    public boolean textured = false;

    public Sphere(Vector3 position, double radius, ColorRGB colour) {
        this.position = position;
        this.radius = radius;
        this.colour = colour;

        this.phong_kD = DEFAULT_SPHERE_KD;
        this.phong_kS = DEFAULT_SPHERE_KS;
        this.phong_alpha = DEFAULT_SPHERE_ALPHA;
        this.reflectivity = DEFAULT_SPHERE_REFLECTIVITY;
        this.transmittance = new ColorRGB(0);
        this.refractive_index = DEFAULT_SPHERE_REFRACTIVE_INDEX;
    }

    public Sphere(Vector3 position, double radius, ColorRGB colour, double kD, double kS, double alphaS, double reflectivity, ColorRGB transmittance, double refractiveIndex, String bumpMapImg, String textureMapImg) {
        this.position = position;
        this.radius = radius;
        this.colour = colour;

        this.phong_kD = kD;
        this.phong_kS = kS;
        this.phong_alpha = alphaS;
        this.reflectivity = reflectivity;
        this.transmittance = transmittance;
        this.refractive_index = refractiveIndex;

        // if it has a bump map load the bump_map and set bumpy to be true, same for texturemap
        if (bumpMapImg != null) {
            loadBumpMap(bumpMapImg);
            this.bumpy = true;
        };
        if (textureMapImg != null) {
            loadTextureMap(textureMapImg);
            this.textured = true;
        };
    }

    /*
     * Calculate intersection of the sphere with the ray. If the ray starts inside the sphere,
     * intersection with the surface is also found.
     */
    public RaycastHit intersectionWith(Ray ray) {
        // Get ray parameters
        Vector3 O = ray.getOrigin();
        Vector3 D = ray.getDirection();

        // Get sphere parameters
        Vector3 C = position;
        double r = radius;

        // Calculate quadratic coefficients
        double a = D.dot(D);
        double b = 2 * D.dot(O.subtract(C));
        double c = (O.subtract(C)).dot(O.subtract(C)) - Math.pow(r, 2);

        // quadratic in s, where the ray is O+sD.
        // check discriminant
        double disc = Math.pow(b,2) - 4 * a * c;

        if (disc < 0) {
            // disc < 0, so there was no intersection
            return new RaycastHit();
        }

        double intersect1 = (-b - Math.sqrt(disc)) / (2 * a);
        double intersect2 = (-b + Math.sqrt(disc)) / (2 * a);
        double intersectionS;

        if (intersect1 < 0 && intersect2 < 0) {
            return new RaycastHit();
        } else if (intersect1 >= 0 && intersect1 <= intersect2) {
            intersectionS = intersect1;
        } else if (intersect2 >= 0 && intersect2 <= intersect1) {
            intersectionS = intersect2;
        } else if (intersect1 > intersect2) { // case where one is +ve, and the other negative, take the +ve
            intersectionS = intersect1;
        } else {
            intersectionS = intersect2;
        }

        // get the point of intersection
        Vector3 intersectionPoint = O.add(D.scale(intersectionS));

        // check whether the ray started inside or outside the sphere
        double distance = O.subtract(C).magnitude();
        int sign = (distance >= radius) ? 1 : -1;

        // get the normal vector
        Vector3 normal = this.getNormalAt(intersectionPoint).scale(sign);

        // as the ray direction vector is normalised, the ray distance is intersectionS
        return new RaycastHit(this, intersectionS, intersectionPoint, normal);
    }

    // Get normal to surface at position
    public Vector3 getNormalAt(Vector3 position) {
        if (bumpy) {
            return getBumpyNormalAt(position);
        }

        return position.subtract(this.position).normalised();
    }
    @Override
    public ColorRGB getColourAt (Vector3 position) {
        if (textured) {
            return getTextureColourAt(position);
        }
        return super.getColourAt(position);
    }

    // Bump map and texture map functions
    private float BUMP_FACTOR = 5f;
    private float[][] heightMap = null;
    private int bumpMapHeight;
    private int bumpMapWidth;

    private void loadBumpMap(String bumpMapImg) {
        try {
            BufferedImage inputImg = ImageIO.read(new File(bumpMapImg));
            bumpMapHeight = inputImg.getHeight();
            bumpMapWidth = inputImg.getWidth();
            heightMap = new float[bumpMapHeight][bumpMapWidth];
            for (int row = 0; row < bumpMapHeight; row++) {
                for (int col = 0; col < bumpMapWidth; col++) {
                    float height = (float) (inputImg.getRGB(col, row) & 0xFF) / 0xFF;
                    heightMap[row][col] = BUMP_FACTOR * height;
                }
            }
        } catch (IOException e) {
            System.err.println("Error creating bump map");
            e.printStackTrace();
        }
    }

    public double getHeightAt(int u, int v) {
        u = u % bumpMapWidth;
        v = v % bumpMapHeight;

        return heightMap[v][u]; // gets the row first then the column, so y,x indexing
    };

    public Tuple<Double> getPhiTheta (Vector3 position) {
        Vector3 normal = position.subtract(this.position).normalised();
        // We are going to use phi as the rotation around the xz from x anticlockwise plane and theta as the declination from y+.
        // |r| = 1
        // phi varies from 0 to 2 pi
        // theta varies from 0 to pi

        // Fx = sin theta * cos phi
        // Fy = cos theta
        // Fz = sin theta * sin phi

        // Get the phi and theta arguments
        double theta = Math.acos(normal.y); // 0 - pi
        double sin_theta = Math.sin(theta); // >= 0
        double phi; // 0 - 2pi
        if (normal.z >= 0) {
            phi = Math.acos(normal.x / sin_theta); // 0 to pi
        } else {
            phi = 2 * Math.PI - Math.acos(normal.x / sin_theta); // 2pi - acos is z is negative
        }
        return new Tuple<>(phi, theta);
    };

    public Tuple<Integer> getUV (double phi, double theta) {
        int u = (int)((double)textureMapWidth * (phi / (2 * Math.PI)));
        int v = (int)((double)textureMapHeight * (theta / Math.PI));
        return new Tuple<>(u,v);
    }

    public Vector3 getBumpyNormalAt(Vector3 position) {
        Vector3 normal = position.subtract(this.position).normalised();
        Tuple<Double> phiTheta = getPhiTheta(position);
        double phi = phiTheta.a;
        double theta = phiTheta.b;

        // Pu is the unit vector is the direction of d phi
        // d/dphi (Fx, Fy, Fz) = (-sin theta * sin phi, 0, sin theta * cos phi), normalised = (cos phi, 0, - sin phi)
        Vector3 Pu = new Vector3(-Math.sin(phi), 0, Math.cos(phi));

        // Pv is the unit vector in the direction of d theta
        // d/d theta (Fx, Fy, Fz) = (cos theta * cos phi, -sin theta, cos theta * sin phi)
        Vector3 Pv = new Vector3(Math.cos(theta) * Math.cos(phi), -Math.sin(theta), Math.cos(theta) * Math.sin(phi));

        Tuple<Integer> uv = getUV(phi, theta);
        int u = uv.a;
        int v = uv.b;

        // Bu and Bv are the changes of height in the u and v directions
        // this way round makes the bumps go the correct way, not sure why its different
        double Bu = getHeightAt(u,v) - getHeightAt(u+1,v);
        double Bv = getHeightAt(u,v) - getHeightAt(u,v+1);

        return normal.add(Pu.scale(Bu).add(Pv.scale(Bv))).normalised();
    }

    private ColorRGB[][] textureMap;
    private int textureMapHeight;
    private int textureMapWidth;

    private void loadTextureMap(String textureMapImg) {
        try {
            BufferedImage inputImg = ImageIO.read(new File(textureMapImg));
            textureMapHeight = inputImg.getHeight();
            textureMapWidth = inputImg.getWidth();
            textureMap = new ColorRGB[textureMapHeight][textureMapWidth];
            for (int row = 0; row < textureMapHeight; row++) {
                for (int col = 0; col < textureMapWidth; col++) {
                    var colour = inputImg.getRGB(col, row);
                    double blue = colour & 0xff;
                    double green = (colour & 0xff00) >> 8;
                    double red = (colour & 0xff0000) >> 16;

                    ColorRGB c = new ColorRGB(red, green, blue).scale(1./255.);
                    textureMap[row][col] = c;
                }
            }
        } catch (IOException e) {
            System.err.println("Error creating texture map");
            e.printStackTrace();
        }
    }

    public ColorRGB getTextureColourAt(Vector3 position) {
        Tuple<Double> phiTheta = getPhiTheta(position);
        double phi = phiTheta.a;
        double theta = phiTheta.b;

        // get coordinates
        Tuple<Integer> uv = getUV(phi, theta);
        int u = uv.a;
        int v = uv.b;

        // get colour at those coordinates and return it
        return textureMap[v][u];
    };

}

