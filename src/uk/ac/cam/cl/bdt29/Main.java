package uk.ac.cam.cl.bdt29;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    // Default input and output files

    public static final String DEFAULT_INPUT = "cone.xml";
    public static final String DEFAULT_OUTPUT = "test.png";

    public static final int DEFAULT_BOUNCES = 4; // 5

    // Height and width of the output image
    private static final int DEFAULT_WIDTH_PX = 1920; //800
    private static final int DEFAULT_HEIGHT_PX = 1080; //600

    public static void usageError() { // Usa+ge information
        System.err.println("USAGE: <tick2> [--input INPUT] [--output OUTPUT] [--bounces BOUNCES] [--resolution WIDTHxHEIGHT]");
        System.exit(-1);
    }

    public static void main(String[] args) throws IOException {
        // We should have an even number of arguments - each option and its value


        if (args.length % 2 != 0) {
            usageError();
        }

        // Parse the input and output filenames from the arguments
        String inputSceneFile = "scenes/" + DEFAULT_INPUT;
        String output = "renders/" + DEFAULT_OUTPUT;
        int width = DEFAULT_WIDTH_PX, height = DEFAULT_HEIGHT_PX;

        int bounces = DEFAULT_BOUNCES;
        for (int i = 0; i < args.length; i += 2) {
            switch (args[i]) {
                case "-i":
                case "--input":
                    inputSceneFile = args[i + 1];
                    break;
                case "-o":
                case "--output":
                    output = args[i + 1];
                    break;
                case "-b":
                case "--bounces":
                    bounces = Integer.parseInt(args[i + 1]);
                    break;
                case "-r":
                case "--resolution":
                    Pattern res_pat = Pattern.compile("(\\d+)x(\\d+)");
                    Matcher m = res_pat.matcher(args[i+1]);
                    if( m.find() ) {
                        width = Integer.parseInt(m.group(1));
                        height = Integer.parseInt(m.group(2));
                        if( width <= 0 || height <= 0 || width > 4096 || height >= 4096 ) {
                            System.err.println("unsupported resolution: " + args[i + 1]);
                            usageError();
                        }
                    } else {
                        System.err.println("unsupported resolution: " + args[i + 1]);
                        usageError();
                    }
                    break;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    usageError();
            }
        }

        // Create the scene from the XML file
        System.out.printf( "Loading scene '%s'\n", inputSceneFile );
        RaycastHit.Scene scene = new SceneLoader(inputSceneFile).getScene();



        // Create the image and colour the pixels
        BufferedImage image = new Renderer(width, height, bounces, scene.getBackgroundColour()).render(scene);

        // Save the image to disk
        File save = new File(output);
        ImageIO.write(image, "png", save);
    }
}
