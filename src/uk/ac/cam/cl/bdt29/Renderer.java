package uk.ac.cam.cl.bdt29;

import uk.ac.cam.cl.bdt29.data_structures.ColorRGB;
import uk.ac.cam.cl.bdt29.data_structures.SquareMatrix3;
import uk.ac.cam.cl.bdt29.data_structures.Vector3;
import uk.ac.cam.cl.bdt29.elements.Plane;
import uk.ac.cam.cl.bdt29.elements.PointLight;
import uk.ac.cam.cl.bdt29.elements.SceneObject;

import java.awt.image.BufferedImage;
import java.util.List;

public class Renderer {

    // The width and height of the image in pixels
    private int width, height;

    // Bias factor for reflected and shadow rays
    private final double EPSILON = 0.0001;

    // The number of times a ray can bounce for reflection
    private int bounces;

    // Background colour of the image
    private ColorRGB backgroundColor;

    // Distributed shadow tracing
    private final int SHADOW_RAY_COUNT = 1; // 20

    private final double LIGHT_SIZE = 0.00000001; // 0.1

    // Depth of Field
    private final int DOF_RAY_COUNT = 12; // 20
    private final double DOF_FOCAL_PLANE = 3.85;
    private final double DOF_AMOUNT = 0.045; // 0.05

    public Renderer(int width, int height, int bounces, ColorRGB background) {
        this.width = width;
        this.height = height;
        this.bounces = bounces;
        this.backgroundColor = background;
    }

    /*
     * Trace the ray through the supplied scene, returning the colour to be rendered.
     * The bouncesLeft parameter is for rendering reflective surfaces.
     */

    // TODO: refactor

    protected ColorRGB trace(RaycastHit.Scene scene, Ray ray, int bouncesLeft) {
        // Find closest intersection of ray in the scene
        RaycastHit closestHit = scene.findClosestIntersection(ray);

        // If no object has been hit, return a background colour
        SceneObject object = closestHit.getObjectHit();
        if (object == null){
            return backgroundColor;
        }

        // Otherwise calculate colour at intersection and return
        // Get properties of surface at intersection - location, surface normal
        Vector3 P = closestHit.getLocation();
        Vector3 N = closestHit.getNormal();
        Vector3 O = ray.getOrigin();


        // Get refracted component
        double currentRefractiveIndex = ray.getRefractiveIndex();
        double newRefractiveIndex = object.getRefractiveIndex();

        // Illuminate the surface
        ColorRGB directIllumination = this.illuminate(scene, object, P, N, O, currentRefractiveIndex);

        // Get reflectivity
        // will be changed later if there is refraction
        double FresnelFactorForReflectivity = object.getReflectivity();


        ColorRGB refractedColour;
        // base cases
        if (!object.isTransmissive()) {
            refractedColour = new ColorRGB(0); // it does not matter what this is as it will be scaled by transmissance later so will go to 0
        } else if (bouncesLeft == 0) {
            // with 10 bounces, we dont get any of this
            refractedColour = new ColorRGB(0); // this means that when it no longer has any bounces, it is treated as being void.
        } // actual refraction
        else {
            // use Snells law to refract the ray
            // if the current refractive index is equal to the objects refractive index, this means we are leaving the material and so going out into air, refractive index = 1
            if (currentRefractiveIndex == newRefractiveIndex) {
                newRefractiveIndex = 1;
            }
            double refractive_index_ratio = currentRefractiveIndex / newRefractiveIndex;

            // https://en.wikipedia.org/wiki/Snell%27s_law#Vector_form

            // incoming vector
            Vector3 I = P.subtract(O).normalised();

            // I.N = cos(theta_I)
            double cosThetaI = -I.dot(N);
            if (cosThetaI < 0) {
                cosThetaI = -cosThetaI;
                System.out.println("Warning: Normal facing the wrong way");
            }

            // nI sin theta_I = nR sin theta_R
            // n_ratio^2 sin^2 theta_I = sin^2 theta_R
            // sin theta_R = sqrt(n_ratio^2(1-cos^2 theta_I))

            // Math.max to ensure no floating point errors cause a negative root
            double sinThetaR = Math.sqrt(Math.max(0,Math.pow(refractive_index_ratio, 2)*(1-Math.pow(cosThetaI,2))));
            double cosThetaR = Math.sqrt(Math.max(0, 1 - Math.pow(sinThetaR, 2)));

            // if sinThetaR^2 > 1 then Total internal reflection
            if (Math.pow(sinThetaR,2) > 1) {
//					System.out.println("TIR");
                // all of the light is reflected
                // get the reflected ray
                // PO reflected in N gives the direction of the new ray
                Vector3 direction = O.subtract(P).reflectIn(N).normalised();
                Vector3 origin = P.add(N.scale(EPSILON));
                Ray reflectedRay = new Ray(origin,direction, ray.getRefractiveIndex());

                ColorRGB transmittance = object.getTransmittance();
                ColorRGB transmittanceComplement = new ColorRGB(1).subtract(transmittance);



                ColorRGB reflectedIllumination = trace(scene, reflectedRay, bouncesLeft-1);

                ColorRGB reflectedComponent = reflectedIllumination.scale(transmittance);
                ColorRGB directComponent = directIllumination.scale(transmittanceComplement);
                return directComponent.add(reflectedComponent);
            } else {
                // use the equation given on the wiki page
                Vector3 v_refract = I.scale(refractive_index_ratio).add(N.scale(refractive_index_ratio*cosThetaI-cosThetaR));

                Ray refractedRay = new Ray(P.add(N.scale(-EPSILON)), v_refract.normalised(), newRefractiveIndex);

                // frensel refraction, schlick's approximation
                FresnelFactorForReflectivity = object.getReflectivity() + (1 - object.getReflectivity()) * Math.pow(1 - cosThetaI,5);

                refractedColour = trace(scene, refractedRay, bouncesLeft-1);
            }
        }


        if (bouncesLeft == 0 || FresnelFactorForReflectivity == 0) {
            // Base case
            ColorRGB transmittance = object.getTransmittance();
            ColorRGB transmittanceComplement = new ColorRGB(1).subtract(transmittance);

            // as bounces are 0, we assume reflectivity of 0
            ColorRGB refractedComponent = refractedColour.scale(transmittance);
            ColorRGB directComponent = directIllumination.scale(transmittanceComplement);

            return directComponent.add(refractedComponent);
        } else {
            // Recursive case

            // get the reflected ray
            // PO reflected in N gives the direction of the new ray
            Vector3 direction = O.subtract(P).reflectIn(N).normalised();
            Vector3 origin = P.add(direction.scale(EPSILON));
            Ray reflectedRay = new Ray(origin,direction, ray.getRefractiveIndex());

            ColorRGB reflectedIllumination = trace(scene, reflectedRay, bouncesLeft-1);

            // Scale refracted, direct and reflective illumination to conserve light
            // reflected: reflectedComponent
            // refracted+direct = 1-reflectedComponent
            // refrecated = transmittance(1-reflectedComponent)

            // get transmittance
            ColorRGB transmittance = object.getTransmittance();
            ColorRGB transmittanceComplement = new ColorRGB(1).subtract(transmittance);

            // get components
            ColorRGB reflectedComponent = reflectedIllumination.scale(FresnelFactorForReflectivity);
            ColorRGB refractedComponent = refractedColour.scale(transmittance).scale(1.0-FresnelFactorForReflectivity);
            ColorRGB directComponent = directIllumination.scale(transmittanceComplement).scale(1.0-FresnelFactorForReflectivity);

            return directComponent.add(reflectedComponent).add(refractedComponent);
        }
    }



    /*
     * Illuminate a surface on and object in the scene at a given position P and surface normal N,
     * relative to ray originating at O
     */


    private ColorRGB illuminate(RaycastHit.Scene scene, SceneObject object, Vector3 P, Vector3 N, Vector3 O, double refractiveIndex) {

        ColorRGB colourToReturn = new ColorRGB(0);

        ColorRGB I_a = scene.getAmbientLighting(); // Ambient illumination intensity

        ColorRGB C_diff = object.getColourAt(P); // Diffuse colour defined by the object

        // Get Phong reflection model coefficients
        double k_d = object.getPhong_kD();
        double k_s = object.getPhong_kS();
        double alpha = object.getPhong_alpha();

        // AMBIENT LIGHT TERM
        ColorRGB ambientIntensity = C_diff.scale(I_a);
        colourToReturn = colourToReturn.add(ambientIntensity);

        // Loop over each point light source
        List<PointLight> pointLights = scene.getPointLights();
        for (int i = 0; i < pointLights.size(); i++) {
            PointLight light = pointLights.get(i); // Select point light

            // get light properites
            Vector3 LightPos = light.getPosition();
            double distanceToLight = LightPos.subtract(P).magnitude();
            ColorRGB C_spec = light.getColour();
            ColorRGB I = light.getIlluminationAt(distanceToLight);
            Vector3 L = LightPos.subtract(P).normalised();

            // Loop to do SHADOW_RAY_COUNT casts
            ColorRGB total_blocked = new ColorRGB(0);
//			int occluded=0;
            for (int j=0; j<SHADOW_RAY_COUNT; j++) {
                Vector3 RandomLightPos = light.getPosition().add(Vector3.randomInsideUnitSphere().scale(LIGHT_SIZE));
                double distanceToRandomLight = RandomLightPos.subtract(P).magnitude();

                // Calculate L = from P to light source; LightPos - P
                Vector3 LRand = RandomLightPos.subtract(P).normalised();

                // Check if there is a shadow, i.e an object in the way, the direction of the ray is oppostie to L.
                Ray shadowRay = new Ray(P.add(N.scale(EPSILON)),LRand, refractiveIndex);
                RaycastHit Hit = scene.findClosestIntersection(shadowRay);
                if (Hit.getDistance() <= distanceToRandomLight) {
//					occluded++;
                    ColorRGB transmitted = scene.findCompoundTransmittance(shadowRay, distanceToRandomLight);
                    ColorRGB blocked = new ColorRGB(1).subtract(transmitted);

                    total_blocked = total_blocked.add(blocked);
                }
            }
            ColorRGB average_blocked = total_blocked.scale(1/(double)SHADOW_RAY_COUNT);
            ColorRGB average_passThrough = new ColorRGB(1).subtract(average_blocked);

//			double scalar = ((double) (SHADOW_RAY_COUNT - occluded)) / ((double)SHADOW_RAY_COUNT);

            // Calculate Specular Reflection for this light source
            // Normal: N

            // Calculate V from P to O = O - P
            Vector3 V = O.subtract(P).normalised();
            // Reflect L in N to find R
            Vector3 R = L.reflectIn(N);

            // Calculate the specular reflection
            double dotprodRV = R.dot(V);
            // if the dot product is less than 0 then cos(theta) < 0 and so theta > pi / 2, this means that the ray is hitting from behind.
            if (dotprodRV > 0) {
                ColorRGB specularReflectionIntensity = C_spec.scale(I).scale(k_s).scale(Math.pow(dotprodRV,alpha));
                colourToReturn = colourToReturn.add(specularReflectionIntensity.scale(average_passThrough));
//				colourToReturn = colourToReturn.add(specularReflectionIntensity.scale(scalar));

            }

            // Calculate Diffuse Reflection for this light source
            double dotprodLN = L.dot(N);
            if (dotprodLN > 0) {

                ColorRGB diffuseReflectionIntensity = C_diff.scale(I).scale(k_d).scale(dotprodLN);
                colourToReturn = colourToReturn.add(diffuseReflectionIntensity.scale(average_passThrough));
//				colourToReturn = colourToReturn.add(diffuseReflectionIntensity.scale(scalar));

            }
        }
        return colourToReturn;
    }



    // Render image from scene, with camera at origin
    public BufferedImage render(RaycastHit.Scene scene) {

        // Set up image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Set up camera
        Camera camera = new Camera(width, height, scene.getCameraOrigin(), scene.getCameraRotation());

        // rotate the forcal plane so that is is in the direction of the camera
        SquareMatrix3 rotationMatrix = camera.getRotationMatrix();
        Vector3 DOFPlanePoint = camera.origin.add(rotationMatrix.leftMultiplyVector(new Vector3(0,0,DOF_FOCAL_PLANE)));
        Vector3 DOFPlaneDirection = rotationMatrix.leftMultiplyVector(new Vector3(0,0,-1)).normalised();
        Plane FocalPlane = new Plane(DOFPlanePoint, DOFPlaneDirection, new ColorRGB(0));

        // Loop over all pixels
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                // point that it would have intersected the focal plane
                Ray ray = camera.castRay(x, y); // Cast ray through pixel
                Vector3 pointAtFocalLength = FocalPlane.intersectionWith(ray).getLocation();

                ColorRGB SumlinearRGB = new ColorRGB(0);
                // cast DOF_RAY_COUNT random rays through our aperature
                for (int i=0;i<DOF_RAY_COUNT;i++) {
                    double offsetx = (Math.random()-0.5) * DOF_AMOUNT;
                    double offsety = (Math.random()-0.5) * DOF_AMOUNT;

                    Vector3 rayOrigin = camera.origin.add(new Vector3(offsetx, offsety, 0));
                    Vector3 rayDirection = pointAtFocalLength.subtract(rayOrigin).normalised();
                    Ray apertureRay = new Ray(rayOrigin, rayDirection, ray.getRefractiveIndex());

                    SumlinearRGB = SumlinearRGB.add(trace(scene, apertureRay, bounces));

                }
                ColorRGB linearRGB = SumlinearRGB.scale(1/(double)DOF_RAY_COUNT);
                ColorRGB gammaRGB = tonemap( linearRGB );
                image.setRGB(x, y, gammaRGB.toRGB()); // Set image colour to traced colour
            }
            // Display progress every 10 lines
            System.out.println(String.format("%.2f", 100 * y / (float) (height - 1)) + "% completed");
            if( y % 10 == 9 | y==(height-1) )
                System.out.println(String.format("%.2f", 100 * y / (float) (height - 1)) + "% completed");
        }
        return image;
    }


    // Combined tone mapping and display encoding
    public ColorRGB tonemap(ColorRGB linearRGB ) {
        double invGamma = 1. / 2.2;
        double a = 2;  // controls brightness
        double b = 1.3; // controls contrast

        // Sigmoidal tone mapping
        ColorRGB powRGB = linearRGB.power(b);
        ColorRGB displayRGB = powRGB.scale(powRGB.add(Math.pow(0.5 / a, b)).inv());

        // Display encoding - gamma
        ColorRGB gammaRGB = displayRGB.power(invGamma);

        return gammaRGB;
    }
}

