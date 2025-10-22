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

public class Cylinder extends SceneObject {

    // Cylinder constants
    public static final double DEFAULT_CYLINDER_KD = 0.8;
    public static final double DEFAULT_CYLINDER_KS = 1.2;
    public static final double DEFAULT_CYLINDER_ALPHA = 10;
    public static final double DEFAULT_CYLINDER_REFLECTIVITY = 0.3;
    public static final double DEFAULT_CYLINDER_LENGTH = 1;
    public static final double DEFAULT_CYLINDER_RADUS = 1;


    private Vector3 position;
    private Vector3 axis;
    private Vector3 angleStart;
    private Vector3 point;
    private double length;
    private double radius;


    public Vector3 getPosition() {
        return position;
    }

    // bumpy and textured booleans
    public boolean bumpy = false;
    public boolean textured = false;

    public Cylinder(Vector3 point, Vector3 axis, double length, double radius, ColorRGB colour) {
        this.point = point;
        this.axis = axis;
        this.colour = colour;
        this.length = length;
        this.radius = radius;

        this.phong_kD = DEFAULT_CYLINDER_KD;
        this.phong_kS = DEFAULT_CYLINDER_KS;
        this.phong_alpha = DEFAULT_CYLINDER_ALPHA;
        this.reflectivity = DEFAULT_CYLINDER_REFLECTIVITY;

        this.angleStart = createAngleStartVector();
    }

    public Cylinder(Vector3 point, Vector3 axis, double length, double radius, ColorRGB colour, double kD, double kS, double alphaS, double reflectivity, String textureMapImg) {
        this.point = point;
        this.axis = axis;
        this.colour = colour;
        this.length = length;
        this.radius = radius;


        this.phong_kD = kD;
        this.phong_kS = kS;
        this.phong_alpha = alphaS;
        this.reflectivity = reflectivity;


        this.angleStart = this.createAngleStartVector();

        // if it has a texture map load the texture_map and set textured to be true
        if (textureMapImg != null) {
            loadTextureMap(textureMapImg);
            this.textured = true;
        }
    }

    public Vector3 createAngleStartVector() {
        if (axis.x == 0) {
            return new Vector3(1,0,0);
        } else if(axis.y == 0) {
            return new Vector3(0,1,0);
        } else if (axis.z == 0) {
            return new Vector3(0,0,1);
        }
        // none of the axis components are 0
        // arbitrarily set x = 1, y=0 , solve for z
        // axis . angleStart = 0
        // vX + vZ * z = 0
        double z = (-axis.x)/axis.z;
        return new Vector3(1,0,z).normalised();
    }

    public RaycastHit intersectionWith(Ray ray) {
        // http://lousodrome.net/blog/light/2017/01/03/intersection-of-a-ray-and-a-cone/
        Vector3 O = ray.getOrigin();
        Vector3 D = ray.getDirection();

        Vector3 C = this.point;
        Vector3 V = this.axis;

        Vector3 OC = C.subtract(O);
        // https://en.wikipedia.org/wiki/Line-cylinder_intersection
        // but then I worked out a b and c as it doesnt say them
        Vector3 VxD = V.cross(D);
        Vector3 VxOC = V.cross(OC);


        double a = VxD.dot(VxD);
        double b = -2 * VxD.dot(VxOC);
        double c = VxOC.dot(VxOC) - Math.pow(this.radius,2);

        // check discriminant
        double disc = Math.pow(b,2) - 4 * a * c;

        if (disc < 0) {
            // disc < 0, so there was no intersection
            return new RaycastHit();
        }

        double intersect1 = (-b - Math.sqrt(disc)) / (2 * a);
        double intersect2 = (-b + Math.sqrt(disc)) / (2 * a);
        double intersectionS;

        // Check whether it is intersecting the shadow cylinder opposite to the defined cylinder, if so, no hit
        boolean oneIsShadow = checkShadowIntersection(intersect1, ray);
        boolean twoIsShadow = checkShadowIntersection(intersect2, ray);

        if (intersect1 < 0 && intersect2 < 0 || oneIsShadow && twoIsShadow || oneIsShadow && intersect2 < 0 || twoIsShadow && intersect1 < 0) {
            return new RaycastHit();
        } else if (oneIsShadow && intersect2 >= 0) {
            intersectionS = intersect2;
        } else if (twoIsShadow && intersect1 >= 0) {
            intersectionS = intersect1;
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
        Vector3 intersectionPoint = ray.evaluateAt(intersectionS);

        // get the normal vector
        Vector3 normal = this.getNormalAt(intersectionPoint);

        return new RaycastHit(this, intersectionS, intersectionPoint, normal);
    }

    private boolean checkShadowIntersection(double t, Ray ray) {
        Vector3 P = ray.evaluateAt(t);
        Vector3 C = this.point;
        Vector3 V = this.axis;

        // distance = axis.dot(relative position)
        double distance = P.subtract(C).dot(V);

        if (distance <= 0 || distance >= this.length) {
            return true;
        }
        return false;
    }

    // Get normal to the plane
    @Override
    public Vector3 getNormalAt(Vector3 position) {
        // r - axis * t  normalised
        Vector3 r = position.subtract(this.point);
        double t = r.dot(this.axis);

        Vector3 normal = r.subtract(this.axis.scale(t)).normalised();

        return normal;
    }

    @Override
    public ColorRGB getColourAt (Vector3 position) {
        if (textured) {
            return getTextureColourAt(position);
        }
        return super.getColourAt(position);
    }

    public Tuple<Double> getHTheta (Vector3 position) {
        Vector3 across = getNormalAt(position);

        // the angle between this and angleStart can be calculated
        double cosTheta = across.dot(this.angleStart);

        // this will mean that the texture will be mapped on 2 sides of the cylinder the same as it does not distinguish between clockwise and anticlockwise
        // coordinate system is h,theta = x,y
        double H = position.subtract(this.point).dot(this.axis);
        double theta = Math.acos(cosTheta);

        return new Tuple<>(H,theta);
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
        Tuple<Double> hTheta = this.getHTheta(position);
        double theta = hTheta.a;
        double h = hTheta.b;

        int x = (int)((double)textureMapWidth * (theta/Math.PI)); // theta can range from 0 to PI because of arccos
        // z should be textureMapWidth when h=length
        int z = (int)((double)textureMapHeight * (h/length));

        x = x % textureMapWidth;
        z = z % textureMapHeight;
        if (z < 0) {
            z += textureMapHeight;
        }

        // make the texture the correct orientation.
        z = textureMapHeight - z - 1;

        // get colour at those coordinates and return it
        return textureMap[z][x];
    }
}

