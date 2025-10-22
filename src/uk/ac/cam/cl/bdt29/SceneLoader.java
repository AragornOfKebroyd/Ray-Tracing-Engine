package uk.ac.cam.cl.bdt29;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.cam.cl.bdt29.data_structures.ColorRGB;
import uk.ac.cam.cl.bdt29.data_structures.Vector3;
import uk.ac.cam.cl.bdt29.elements.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class SceneLoader {
    // Loads our scene from an XML file

    private RaycastHit.Scene scene;

    public SceneLoader(String filename) {
        scene = new RaycastHit.Scene();

        Element document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filename))
                    .getDocumentElement();
        } catch (ParserConfigurationException e) {
            assert false;
        } catch (IOException e) {
            throw new RuntimeException("error reading file:\n" + e.getMessage());
        } catch (SAXException e) {
            throw new RuntimeException("error loading XML.");
        }

        if (document.getNodeName() != "scene")
            throw new RuntimeException("scene file does not contain a scene element");

        NodeList elements = document.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); ++i) {
            Element element = (Element) elements.item(i);
            switch (element.getNodeName()) {

                case "camera":
                    Vector3 origin = getPosition(element);
                    Vector3 rotation = getRotation(element);
                    scene.setCameraOrigin(origin);
                    scene.setCameraRotation(rotation);

                    break;

                case "sphere":
                    // get a sphere with fallback elements of null
                    Sphere sphere = new Sphere(
                            getPosition(element),
                            getDouble(element, "radius", 1),
                            getColour(element),
                            getDouble(element, "kD", Sphere.DEFAULT_SPHERE_KD),
                            getDouble(element, "kS", Sphere.DEFAULT_SPHERE_KS),
                            getDouble(element, "alphaS", Sphere.DEFAULT_SPHERE_ALPHA),
                            getDouble(element, "reflectivity", Sphere.DEFAULT_SPHERE_REFLECTIVITY),
                            getTransmittance(element),
                            getDouble(element, "refractive_index", Sphere.DEFAULT_SPHERE_REFRACTIVE_INDEX),
                            getString(element, "bump-map", null),
                            getString(element, "texture-map", null)
                    );
                    scene.addObject(sphere);
                    break;

                case "plane":
                    Plane plane = new Plane(
                            getPosition(element),
                            getNormal(element),
                            getColour(element),
                            getDouble(element, "kD", Plane.DEFAULT_PLANE_KD),
                            getDouble(element, "kS", Plane.DEFAULT_PLANE_KS),
                            getDouble(element, "alphaS", Plane.DEFAULT_PLANE_ALPHA),
                            getDouble(element, "reflectivity", Plane.DEFAULT_PLANE_REFLECTIVITY),
                            getString(element, "bump-map", null),
                            getString(element, "texture-map", null),
                            getDouble(element,"texture-scale", 10),
                            getDouble(element, "texture-rotation", 0),
                            getPlaneBoundingCentre(element, null),
                            getDouble(element, "bounding-radius", Double.POSITIVE_INFINITY),
                            getString(element, "bounding-operator","<")
                    );
                    scene.addObject(plane);
                    break;

                case "cone":
                    Cone cone = new Cone(
                            getPosition(element),
                            getAxis(element),
                            getDouble(element, "angle", Cone.DEFAULT_CONE_ANGLE),
                            getDouble(element, "length", Cone.DEFAULT_CONE_LENGTH),
                            getColour(element),
                            getDouble(element, "kD", Cone.DEFAULT_CONE_KD),
                            getDouble(element, "kS", Cone.DEFAULT_CONE_KS),
                            getDouble(element, "alphaS", Cone.DEFAULT_CONE_ALPHA),
                            getDouble(element, "reflectivity", Cone.DEFAULT_CONE_REFLECTIVITY),
                            getString(element, "texture-map", null)
                    );
                    scene.addObject(cone);
                    break;

                case "cylinder":
                    Cylinder cylinder = new Cylinder(
                            getPosition(element),
                            getAxis(element),
                            getDouble(element, "length", Cylinder.DEFAULT_CYLINDER_LENGTH),
                            getDouble(element, "radius", Cylinder.DEFAULT_CYLINDER_RADUS),
                            getColour(element),
                            getDouble(element, "kD", Cylinder.DEFAULT_CYLINDER_KD),
                            getDouble(element, "kS", Cylinder.DEFAULT_CYLINDER_KS),
                            getDouble(element, "alphaS", Cylinder.DEFAULT_CYLINDER_ALPHA),
                            getDouble(element, "reflectivity", Cylinder.DEFAULT_CYLINDER_REFLECTIVITY),
                            getString(element, "texture-map", null)
                    );
                    scene.addObject(cylinder);
                    break;

                case "point-light":
                    PointLight light = new PointLight(getPosition(element), getColour(element),
                            getDouble(element, "intensity", 100));
                    scene.addPointLight(light);
                    break;

                case "ambient-light":
                    scene.setAmbientLight(getColour(element).scale(getDouble(element, "intensity", 1)));
                    break;

                case "background-colour":
                    scene.setBackgroundColour(getColour(element));
                    break;

                default:
                    throw new RuntimeException("unknown object tag: " + element.getNodeName());
            }
        }
    }

    public RaycastHit.Scene getScene() {
        return scene;
    }

    private Vector3 getPosition(Element tag) {
        double x = getDouble(tag, "x", 0);
        double y = getDouble(tag, "y", 0);
        double z = getDouble(tag, "z", 0);
        return new Vector3(x, y, z);
    }

    private Vector3 getNormal(Element tag) {
        double x = getDouble(tag, "nx", 0);
        double y = getDouble(tag, "ny", 0);
        double z = getDouble(tag, "nz", 0);
        return new Vector3(x, y, z).normalised();
    }

    private Vector3 getAxis(Element tag) {
        double x = getDouble(tag, "ax", 0);
        double y = getDouble(tag, "ay", 0);
        double z = getDouble(tag, "az", 0);
        return new Vector3(x, y, z).normalised();
    }

    private Vector3 getRotation(Element tag) {
        double x = getDouble(tag, "rx", 0);
        double y = getDouble(tag, "ry", 0);
        double z = getDouble(tag, "rz", 0);
        return new Vector3(x % 360, y % 360, z % 360);
    }

    private Vector3 getPlaneBoundingCentre(Element tag, Vector3 fallback) {
        double x = getDouble(tag, "bx", 0);
        double y = getDouble(tag, "by", 0);
        double z = getDouble(tag, "bz", 0);
        if (x == 0 && y == 0 && z == 0) {
            try {
                // check that it was actually wanted
                Double.parseDouble(tag.getAttribute("bx"));
                Double.parseDouble(tag.getAttribute("by"));
                Double.parseDouble(tag.getAttribute("bz"));
                return new Vector3(x, y, z);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return new Vector3(x, y, z);
    }

    private ColorRGB getColour(Element tag) {
        String hexString = tag.getAttribute("colour");
        double red, green, blue;
        try {
            red = Integer.parseInt(hexString.substring(1, 3), 16) / 255.0;
            green = Integer.parseInt(hexString.substring(3, 5), 16) / 255.0;
            blue = Integer.parseInt(hexString.substring(5, 7), 16) / 255.0;
        } catch (Exception e) {
            return new ColorRGB(0);
        }

        return new ColorRGB(red, green, blue);
    }

    private ColorRGB getTransmittance(Element tag) {

        double tr = getDouble(tag, "tr", 0);
        double tg = getDouble(tag, "tg", 0);
        double tb = getDouble(tag, "tb", 0);

        return new ColorRGB(tr, tg, tb);
    }


    private double getDouble(Element tag, String attribute, double fallback) {
        try {
            return Double.parseDouble(tag.getAttribute(attribute));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String getString(Element tag, String attribute, String fallback){
        String fname = tag.getAttribute(attribute);
        if (fname.equals("")) {
            return fallback;
        }
        if (attribute.equals("bump-map")) {
            return "bump-maps/" + fname;
        }
        if (attribute.equals("texture-map")) {
            return "textures/" + fname;
        }
        return fname;
    }
}
