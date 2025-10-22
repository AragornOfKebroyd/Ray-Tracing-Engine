package uk.ac.cam.cl.bdt29;

import uk.ac.cam.cl.bdt29.data_structures.ColorRGB;
import uk.ac.cam.cl.bdt29.data_structures.Vector3;
import uk.ac.cam.cl.bdt29.elements.PointLight;
import uk.ac.cam.cl.bdt29.elements.SceneObject;

import java.util.LinkedList;
import java.util.List;

public class RaycastHit {

    // The distance the ray travelled before hitting an object
    private double distance;

    // The object that was hit by the ray
    private SceneObject objectHit;

    // The location that the ray hit the object
    private Vector3 location;

    // The normal of the object at the location hit by the ray
    private Vector3 normal;

    public RaycastHit() {
        this.distance = Double.POSITIVE_INFINITY;
    }

    public RaycastHit(SceneObject objectHit, double distance, Vector3 location, Vector3 normal) {
        this.distance = distance;
        this.objectHit = objectHit;
        this.location = location;
        this.normal = normal;
    }

    public SceneObject getObjectHit() {
        return objectHit;
    }

    public Vector3 getLocation() {
        return location;
    }

    public Vector3 getNormal() {
        return normal;
    }

    public double getDistance() {
        return distance;
    }

    public static class Scene {

        // A list of 3D objects to be rendered
        private List<SceneObject> objects;

        // A list of point light sources
        private List<PointLight> pointLights;

        // The color of the ambient light in the scene
        private ColorRGB ambientLight;
        private ColorRGB backgroundColour;

        private Vector3 cameraOrigin = new Vector3(0);
        private Vector3 cameraRotation = new Vector3(0);

        public Scene() {
            objects = new LinkedList<SceneObject>();
            pointLights = new LinkedList<PointLight>();
            ambientLight = new ColorRGB(1);
            backgroundColour = new ColorRGB(0.0001);
        }

        public void addObject(SceneObject object) {
            objects.add(object);
        }

        // Find the closest intersection of given ray with an object in the scene
        public RaycastHit findClosestIntersection(Ray ray) {
            RaycastHit closestHit = new RaycastHit(); // initially no intersection

            // Loop over objects and find closest intersection
            for (SceneObject object : objects) {
                RaycastHit trialHit = object.intersectionWith(ray);
                if (trialHit.getDistance() < closestHit.getDistance()) {
                    closestHit = trialHit;
                }
            }
            return closestHit;
        }

        public ColorRGB findCompoundTransmittance(Ray ray, double distanceToLight) {
            ColorRGB transmittance = new ColorRGB(1);
            for (SceneObject object : objects) {
                RaycastHit trialHit = object.intersectionWith(ray);
                if (trialHit.getDistance() > 0 && trialHit.getDistance() < distanceToLight) {
                    ColorRGB scaleTransmittance = trialHit.getObjectHit().getTransmittance();
                    transmittance = transmittance.scale(scaleTransmittance);
                }
            }
            return transmittance;
        }

        public ColorRGB getAmbientLighting() {
            return ambientLight;
        }

        public void setAmbientLight(ColorRGB ambientLight) {
            this.ambientLight = ambientLight;
        }

        public void setBackgroundColour(ColorRGB backgroundColour) {
            this.backgroundColour = backgroundColour;
        }

        public ColorRGB getBackgroundColour() {
            return this.backgroundColour;
        }

        public PointLight getPointLight() {
            return pointLights.get(0);
        }

        public List<PointLight> getPointLights() {
            return pointLights;
        }

        public void addPointLight(PointLight pointLight) {
            pointLights.add(pointLight);
        }

        public Vector3 getCameraOrigin() {
            return this.cameraOrigin;
        }

        public void setCameraOrigin(Vector3 origin) {
            this.cameraOrigin = origin;
        }

        public Vector3 getCameraRotation() {
            return this.cameraRotation;
        }

        public void setCameraRotation(Vector3 rotation) {
            this.cameraRotation = rotation;
        }


    }
}

