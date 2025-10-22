package uk.ac.cam.cl.bdt29.elements;

import uk.ac.cam.cl.bdt29.Ray;
import uk.ac.cam.cl.bdt29.RaycastHit;
import uk.ac.cam.cl.bdt29.data_structures.ColorRGB;
import uk.ac.cam.cl.bdt29.data_structures.SquareMatrix3;
import uk.ac.cam.cl.bdt29.data_structures.Vector3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Plane extends SceneObject {

    // Plane constants
    public static final double DEFAULT_PLANE_KD = 0.8;
    public static final double DEFAULT_PLANE_KS = 1.2;
    public static final double DEFAULT_PLANE_ALPHA = 10;
    public static final double DEFAULT_PLANE_REFLECTIVITY = 0.3;

    // A point in the plane
    protected Vector3 point;

    // The normal of the plane
    protected Vector3 normal;

    // bumpy and textured booleans
    public boolean bumpy = false;
    public boolean textured = false;

    private final Vector3 boundingSphereCentre;
    private final double boundingSphereRadius;
    private final String boundingOperator;

    public Plane(Vector3 point, Vector3 normal, ColorRGB colour) {
        this.point = point;
        this.normal = normal;
        this.colour = colour;

        this.phong_kD = DEFAULT_PLANE_KD;
        this.phong_kS = DEFAULT_PLANE_KS;
        this.phong_alpha = DEFAULT_PLANE_ALPHA;
        this.reflectivity = DEFAULT_PLANE_REFLECTIVITY;
        this.boundingSphereCentre = null;
        this.boundingSphereRadius = Double.POSITIVE_INFINITY;
        this.boundingOperator = "<";
    }

    public Plane(Vector3 point, Vector3 normal, ColorRGB colour, double kD, double kS, double alphaS, double reflectivity, String bumpMapImg, String textureMapImg, double textureScale, double textureRotation, Vector3 boundingCentre, double boudingRadius, String boundingOperator) {
        this.point = point;
        this.normal = normal;
        this.colour = colour;

        this.phong_kD = kD;
        this.phong_kS = kS;
        this.phong_alpha = alphaS;
        this.reflectivity = reflectivity;

        // if it has a bump map load the bump_map and set bumpy to be true, same for texturemap
        if (bumpMapImg != null) {
            loadBumpMap(bumpMapImg);
            this.getMatrix();
            this.getPxPz();
            this.bumpy = true;
        }
        if (textureMapImg != null) {
            loadTextureMap(textureMapImg);
            this.textureRot = textureRotation;
            this.getMatrix();
            this.getPxPz();
            this.textureScale = textureScale;
            this.textured = true;
        }

        this.boundingSphereCentre = boundingCentre;
        this.boundingSphereRadius = boudingRadius;
        if (boundingOperator.equals("<") || boundingOperator.equals(">") ) {
            this.boundingOperator = boundingOperator;
        } else {
            System.out.println("bounding operator not > or <, defualting to <");
            this.boundingOperator = "<";
        }

    }

    // Intersect this plane with a ray
    @Override
    public RaycastHit intersectionWith(Ray ray) {
        // Get ray parameters
        Vector3 O = ray.getOrigin();
        Vector3 D = ray.getDirection();

        // Get plane parameters
        Vector3 Q = this.point;
        Vector3 N = this.normal;

        double dotprodND = N.dot(D);
        double dotprodNO = N.dot(O);
        // P.N = d - plane
        // P = O + sD - line
        // (O + sD).N=d => O.N +s(D.N)=d => s = (d-O.N)/N.D
        double d = N.dot(Q);
        double intersectionS = (d-dotprodNO)/dotprodND;

        if (intersectionS < 0) {
            return new RaycastHit();
        }

        // get the point of intersection
        Vector3 intersectionPoint = O.add(D.scale(intersectionS));

        // if there is a bounding sphere then check if this is out of this bound
        if (this.boundingOperator.equals(">")) {
            // render if it is >, so return if it is <
            if (boundingSphereCentre != null && intersectionPoint.subtract(boundingSphereCentre).magnitude() < boundingSphereRadius) {
                return new RaycastHit();
            }
        } else {
            if (boundingSphereCentre != null && intersectionPoint.subtract(boundingSphereCentre).magnitude() > boundingSphereRadius) {
                return new RaycastHit();
            }
        }


        // get the normal to be used for shading
        Vector3 shadingNormal = this.getNormalAt(intersectionPoint);

        // as the ray direction vector is normalised, the ray distance is intersectionS
        return new RaycastHit(this, intersectionS, intersectionPoint, shadingNormal);
    }

    // Get normal to the plane
    @Override
    public Vector3 getNormalAt(Vector3 position) {
        if (this.bumpy) {
            return this.getBumpyNormalAt(position);
        }
        return normal; // normal is the same everywhere on the plane
    }

    @Override
    public ColorRGB getColourAt (Vector3 position) {
        if (textured) {
            return getTextureColourAt(position);
        }
        return super.getColourAt(position);
    }

    // load bump map and bump map methods and texture map too
    private static final float BUMP_FACTOR = 5f;
    private static final double bumpScale = 3;
    private float[][] heightMap;
    private int bumpMapHeight;
    private int bumpMapWidth;

    // Matrix properites
    private SquareMatrix3 transformation;
    private SquareMatrix3 inverse;
    private Vector3 axis;
    private Vector3 Px;
    private Vector3 Pz;

    private void loadBumpMap (String bumpMapImg) {
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

    private void getMatrix() {
        // matrix to rotate the texture on the xz plane, so a rotation around the y axis
        double deg_to_rad = (2*Math.PI)/360;
        double cosPhi = Math.cos(this.textureRot * deg_to_rad);
        double sinPhi = Math.sin(this.textureRot * deg_to_rad);
        double[][] matInitRotate = {
                {cosPhi,0,sinPhi},
                {0,1,0},
                {-sinPhi,0,cosPhi}
        };
        SquareMatrix3 rotate = new SquareMatrix3(matInitRotate);

        // Matrix properites
        Vector3 j = new Vector3(0, 1, 0);
        Vector3 Raxis = this.normal.cross(j);
        if (Raxis.isZero()) {
            // no need to rotate
            this.transformation = rotate;
            this.inverse = this.transformation.getInverse();
            return;
        }
        // otherwise
        this.axis = Raxis.normalised();

        double cosTheta = this.normal.normalised().dot(j);
        double sinTheta = Math.sqrt(1 - Math.pow(cosTheta, 2));

        double[][] matInitMap = {
                {
                        cosTheta + Math.pow(axis.x, 2) * (1 - cosTheta),
                        -axis.z * sinTheta,
                        axis.x * axis.z * (1 - cosTheta)
                },{
                axis.z * sinTheta,
                cosTheta,
                axis.x * sinTheta
        },{
                axis.x * axis.z * (1 - cosTheta),
                axis.x * sinTheta,
                cosTheta + Math.pow(axis.z, 2) * (1 - cosTheta)
        }
        };

        SquareMatrix3 mapToXY = new SquareMatrix3(matInitMap);

        // should be Rotate * Map * vector as we want to map first
        this.transformation = rotate.multiply(mapToXY);
        this.inverse = transformation.getInverse();
    }

    private void getPxPz() {
        Vector3 i = new Vector3(1,0,0);
        Vector3 k = new Vector3(0,0,1);

        Px = inverse.leftMultiplyVector(i);
        Pz = inverse.leftMultiplyVector(k);
    }

    public double getHeightAt(int x, int z) {
        x = x % bumpMapWidth;
        z = z % bumpMapHeight;
        if (x < 0) {
            x += bumpMapWidth;
        }
        if (z < 0) {
            z += bumpMapHeight;
        }
        return heightMap[z][x]; // gets the row first then the column, so y,x indexing
    }

    public Vector3 getBumpyNormalAt(Vector3 position) {
        Vector3 mappedPoint = mapVectortoXY(position);

        // if 10 is the scale of the thingy then 10 should correspond to 1
        double xVal = mappedPoint.x;
        double zVal = mappedPoint.z;

        // Scale corresponding to scale
        int x = (int)((double)bumpMapWidth * (xVal / bumpScale));
        int z = (int)((double)bumpMapHeight * (zVal / bumpScale));

        // this way round makes the bumps go the correct way, not sure why its different
        double Bu = getHeightAt(x,z) - getHeightAt(x+1,z);
        double Bv = getHeightAt(x,z) - getHeightAt(x,z+1);

        Vector3 newNormal = normal.add(Px.scale(Bu).add(Pz.scale(Bv))).normalised();

        return newNormal;
    }

    private ColorRGB[][] textureMap;
    private int textureMapHeight;
    private int textureMapWidth;
    private double textureScale;
    private double textureRot;

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

    public Vector3 mapVectortoXY (Vector3 position) {
        // translate the plane to the origin by doing a translation of -a where a is a point on the plane
        Vector3 translatedPoint = position.subtract(this.point);
        // Rotate using the rotation matrix rotating through an angle of newPoint . j
        // Using an axis orthogonal to newPoint and j

        Vector3 rotatedPoint = transformation.leftMultiplyVector(translatedPoint);
        return rotatedPoint;
    }

    public ColorRGB getTextureColourAt(Vector3 position) {
        Vector3 mappedPoint = mapVectortoXY(position);

        // if 10 is the scale of the thingy then 10 should correspond to 1
        double xVal = mappedPoint.x;
        double zVal = mappedPoint.z;

        // Scale corresponding to scale

        int x = (int)((double)textureMapWidth * (xVal / textureScale));
        int z = (int)((double)textureMapWidth * (zVal / textureScale));

        x = x % textureMapWidth;
        z = z % textureMapHeight;
        if (x < 0) {
            x += textureMapWidth;
        }
        if (z < 0) {
            z += textureMapHeight;
        }

        // make the texture the correct orientation.
        z = textureMapHeight - z - 1;

        // get colour at those coordinates and return it
        return textureMap[z][x];
    }
}

