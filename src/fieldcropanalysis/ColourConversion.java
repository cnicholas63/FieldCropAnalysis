/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fieldcropanalysis;

import static java.awt.Color.HSBtoRGB;
import static java.awt.Color.RGBtoHSB;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class ColourConversion extends Component {

    BufferedImage trainingImage;
    BufferedImage outputImage;
    int[] hues; // Array for holding hue pixel counts
    int imageWidth, imageHeight;

    public ColourConversion() {
        float[] hsb;


        hues = new int[360]; // Enough space for 360 hues 0 - 360 degrees (360 & 0 occupying same space)

        // Load image
        trainingImage = loadImage();
        imageWidth = trainingImage.getWidth();
        imageHeight = trainingImage.getHeight();
        
        window();

//        for (int y = 0; y < trainingImage.getHeight(); y++) {
//            for (int x = 0; x < trainingImage.getWidth(); x++) {
//                int hue;
//                int col = trainingImage.getRGB(x, y); // Get the pixel colour
//                hsb = getHSB(col);
//
//                //System.err.println("hsb: " + hsb[0]);
//                hue = (int) hsb[0];
//                hue %= 360; // Make sure 360 == 0
//                if (hue < 0 || hue >= 360) {
//                    System.err.println("Hue out of range: " + hue);
//                    System.exit(0);
//                }
//                hues[hue]++;
//            }
//        }

        colourAudit();
        colourMapAnalysis();

//        System.err.println("Done");
//        for (int t = 0; t < 360; t++) {
//            System.out.println("Hue " + t + " = " + hues[t]);
//        }

        //colourise(50, 195);
    }

    float[] getHSB(int rgb) {
        int red; // Split out the composite RGB channels from the pixel
        int green;
        int blue;
        float[] hsb;

        red = rgb >> 16 & 255; // Split out the composite RGB channels from the pixel
        green = rgb >> 8 & 255;
        blue = rgb & 255;

        hsb = RGBtoHSB(red, green, blue, null);

        hsb[0] *= 360; // Bring up to range 0 - 360
        hsb[1] *= 100; // Bring up to range 0 - 100
        hsb[2] *= 100; // Bring up to range 0 - 100

        return hsb;
    }

    // Colourise specified hue range with colours from opposite side of colour wheel
    // 180 degrees
    void colourise(int startHue, int endHue) {
        float[] hsb;

        for (int y = 0; y < trainingImage.getHeight(); y++) {
            for (int x = 0; x < trainingImage.getWidth(); x++) {
                int hue;
                int saturation;
                int brightness;

                int col = trainingImage.getRGB(x, y); // Get the pixel colour
                hsb = getHSB(col);

                //System.err.println("hsb: " + hsb[0]);
                hue = (int) hsb[0];
                saturation = (int) hsb[1];
                brightness = (int) hsb[2];
                hue %= 360; // Make sure 360 == 0
              
                if (hue >= startHue && hue <= endHue) { // Pixels falls in hue range so colour it
                    if (saturation >= 25 && saturation <= 100) {
                        hue += 180; // Add 180 degree angle to hue on wheel
                        hue %= 360; // keep in range 0 - 359

                        hsb[0] = hue;

                        col = HSBtoRGB(hsb[0], hsb[1], hsb[2]);

                        trainingImage.setRGB(x, y, col);

                    }
                }
                

            }
        }
    }

    // Audits the colour profile of the image
    void colourAudit() {
        float[] hsb;        
        
        for (int y = 0; y < trainingImage.getHeight(); y++) {
            for (int x = 0; x < trainingImage.getWidth(); x++) {
                int hue;
                int saturation;
                int brightness;

                int col = trainingImage.getRGB(x, y); // Get the pixel colour
                hsb = getHSB(col);

                //System.err.println("hsb: " + hsb[0]);
                hue = (int) hsb[0];
                saturation = (int) hsb[1];
                brightness = (int) hsb[2];
                hue %= 360; // Make sure 360 == 0
                hues[hue]++; // Increment hue counter
                
            }
        }
    }
    
    // Display colour histogram
    void colourMapAnalysis() {
        int hue, col;
        int saturation;
        int brightness;

        int min, max; // Used when calculating normalisation of hues array
                
        // Normalise the hues array data - find lowest and highest value
        min = max = hues[0];
        
        for(int h : hues) { // Calculate minimum and maximum values in hues array       
            min = min > h ? h : min;
            max = max < h ? h : max;
        }
                
        for (int deg = 0; deg < 360; deg++) {
            hue = hues[deg]; // Get the hue useage count
            float hueNorm = (float)(hue - min) / (float)(max - min); // hueNorm in range 0 - 1
            
            int barHeight = (int)(hueNorm * 500f);
            
            for(int t = 0, y = (imageHeight - 1); t < barHeight; t++) { // Draw a histogram bar for this hue
                
                col = HSBtoRGB((float)deg/360, 1.0f, 1.0f);
                
                trainingImage.setRGB(deg % imageWidth, y - (t%imageHeight), col); // make sure x & y done go out of bounds
            }
        }
    }

/* OLD   */  
    void createImage() {
        double red; // Split out the composite RGB channels from the pixel
        double green;
        double blue;
        double[] rgb = new double[3];

        // Create training set from image
        for (int y = 0; y < trainingImage.getHeight(); y++) {
            for (int x = 0; x < trainingImage.getWidth(); x++) {
                int col = trainingImage.getRGB(x, y); // Get the pixel colour
                red = col >> 16 & 255; // Split out the composite RGB channels from the pixel
                green = col >> 8 & 255;
                blue = col & 255;

                // Normalize the colour channels between 0 - 1
                red /= 255d;
                green /= 255d;
                blue /= 255d;

                rgb[0] = red; // Array containing colour channels
                rgb[1] = green;
                rgb[2] = blue;

                red = rgb[0] * 255;
                green = rgb[1] * 255;
                blue = rgb[2] * 255;

                //col = 0; // zero out bits in col
                col = (int) red << 16;   // Set red channel
                col ^= (int) green << 8; // Or in green channel
                col ^= (int) blue; // Or in blue channel

                trainingImage.setRGB(x, y, col);
            }

            System.out.println("rgb: " + rgb[0] + ", " + rgb[1] + ", " + rgb[2]);
        }

    }


    private void window() {
        JFrame f = new JFrame("Load Image Sample");

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //ColourConversion colourConv = new ColourConversion(); // Instantiate new computer art

        f.add(this);

        f.pack();
        f.setVisible(true);
    }
    
    BufferedImage loadImage() {
        BufferedImage image;
        String fileName = "Field_Ormskirk_Sewn.png"; // "ColourWheelLarge.png"; Field_Ormskirk_Sewn.png";
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.out.println("File not loaded: " + fileName);
            image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        }

        //Raster raster = trainingImage.getData(new Rectangle(0, 0, trainingImage.getWidth() / 2, trainingImage.getHeight() / 2));
        return image;
    }

    public Dimension getPreferredSize() {
        if (trainingImage == null) {
            return new Dimension(100, 100);
        } else {
            return new Dimension(trainingImage.getWidth(null), trainingImage.getHeight(null));
        }
    }

//    public void paint(Graphics g) {
//        g.drawImage(trainingImage, 0, 0, null);
//    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Load Image Sample");

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        ColourConversion colourConv = new ColourConversion(); // Instantiate new computer art

        f.add(colourConv);

        f.pack();
        f.setVisible(true);

    }

}
