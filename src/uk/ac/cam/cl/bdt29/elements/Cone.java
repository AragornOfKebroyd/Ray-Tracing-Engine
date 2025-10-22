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

public class Cone extends SceneObject {

    // Cone constants
    public static final double DEFAULT_CONE_KD = 0.8;
    public static final double DEFAULT_CONE_KS = 1.2;
    public static final double DEFAULT_CONE_ALPHA = 10;
    public static final double DEFAULT_CONE_REFLECTIVITY = 0.3;
    public static final double DEFAULT_CONE_LENGTH = 1;
    public static final double DEFAULT_CONE_ANGLE = 45;

    private Vector3 axis;
    private Vector3 angleStart;
    private Vector3 point;
    private double angle;
    private double length;


    // bumpy and textured booleans
    public boolean bumpy = false;
    public boolean textured = false;

    public Cone(Vector3 point, Vector3 axis, double angle, double length, ColorRGB colour) {
        this.point = point;
        this.axis = axis;
        this.angle = angle;
        this.colour = colour;
        this.length = length;

        this.phong_kD = DEFAULT_CONE_KD;
        this.phong_kS = DEFAULT_CONE_KS;
        this.phong_alpha = DEFAULT_CONE_ALPHA;
        this.reflectivity = DEFAULT_CONE_REFLECTIVITY;

        this.angleStart = createAngleStartVector();
    }

    public Cone(Vector3 point, Vector3 axis, double angle, double length, ColorRGB colour, double kD, double kS, double alphaS, double reflectivity, String textureMapImg) {
        this.point = point;
        this.axis = axis;
        this.angle = angle;
        this.colour = colour;
        this.length = length;

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

        Vector3 CO = O.subtract(C);

        // angle is in degrees
        double deg_to_rad = (2*Math.PI)/360;
        double cosTheta = Math.cos(this.angle * deg_to_rad);

        double a = Math.pow(D.dot(V),2)-Math.pow(cosTheta,2);
        double b = 2 * (D.dot(V) * CO.dot(V)- D.dot(CO) * Math.pow(cosTheta,2));
        double c = Math.pow(CO.dot(V),2) - CO.dot(CO) * Math.pow(cosTheta,2);

        // check discriminant
        double disc = Math.pow(b,2) - 4 * a * c;

        if (disc < 0) {
            // disc < 0, so there was no intersection
            return new RaycastHit();
        }

        double intersect1 = (-b - Math.sqrt(disc)) / (2 * a);
        double intersect2 = (-b + Math.sqrt(disc)) / (2 * a);
        double intersectionS;

        // Check whether it is intersecting the shadow cone opposite to the defined cone, if so, no hit
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

        // check it is within the length
        double len = intersectionPoint.subtract(C).magnitude();
        if (len > length) {
            return new RaycastHit();
        }

        // get the normal vector
        Vector3 normal = this.getNormalAt(intersectionPoint);

        return new RaycastHit(this, intersectionS, intersectionPoint, normal);
    }

    private boolean checkShadowIntersection(double t, Ray ray) {
        Vector3 P = ray.evaluateAt(t);
        Vector3 C = this.point;
        Vector3 V = this.axis;
        // if this is the case (and angle <= 90 deg) then it is the shadow cone
        if (P.subtract(C).dot(V) <= 0) {
            return true;
        }
        return false;
    }

    // Get normal to the plane
    @Override
    public Vector3 getNormalAt(Vector3 I) {
        //https://stackoverflow.com/questions/66343772/cone-normal-vector
        Vector3 P = this.point;
        Vector3 PI = I.subtract(P);
        return P.cross(PI).cross(PI).normalised();
    }

    @Override
    public ColorRGB getColourAt (Vector3 position) {
        if (textured) {
            return getTextureColourAt(position);
        }
        return super.getColourAt(position);
    }

    public Tuple<Double> getRTheta (Vector3 position) {
        // across = r - |r.n|n
        Vector3 r = position.subtract(this.point);
        Vector3 n = this.axis;
        Vector3 across = r.subtract(n.scale(r.dot(n)));

        // the angle between this and angleStart can be calculated
        double cosTheta = across.normalised().dot(this.angleStart);

        // this will mean that the texture will be mapped on 2 sides of the cone the same as it does not distinguish between clockwise and anticlockwise
        // coordinate system is r,theta = x,y
        double mangnitude = r.magnitude();
        double theta = Math.acos(cosTheta);

        return new Tuple<>(mangnitude,theta);
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
        Tuple<Double> rTheta = this.getRTheta(position);
        double r = rTheta.a;
        double theta = rTheta.b;

        int x = (int)((double)textureMapWidth * (theta/Math.PI)); // theta can range from 0 to PI because of arccos
        // z should be textureMapWidth when r=length
        int z = (int)((double)textureMapHeight * (r/length));

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
