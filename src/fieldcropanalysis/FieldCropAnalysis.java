/*
 * Author: Chris Nicholas
 * FieldCropAnalysis provides methods for analysing a crop
 */

package fieldcropanalysis;
// Neuroph Dependencies
//import org.neuroph.contrib.imgrec.image.Color;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;
import org.neuroph.nnet.learning.BackPropagation;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.image.ColorModel;

/**
 *
 * @author cnich
 */
public class FieldCropAnalysis {
    
    // Constructor
    public FieldCropAnalysis() {
        
    }
    
    public BufferedImage neuralNet(BufferedImage image, boolean hsb) {
        NeuralNetwork network = NeuralNetwork.load("NNFieldClassifierHSB.net");
        int rgb; // Represents RGB from scanned pixel
        float[] pixelProperties = {0.0f, 0.0f, 0.0f}; // Holds pixel properties RGB or HSB
        
        int[] classColours = { // Define classification colours
            new java.awt.Color(255, 0, 0).getRGB(),
            new java.awt.Color(0, 255, 0).getRGB(),
            new java.awt.Color(0, 0, 255).getRGB()
        };
        
        // Scan through image file 
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                rgb = image.getRGB(x, y); // Get pixel colour

                Color color = new Color(rgb);
                
                // Split pixel colour into composit values;
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();
                
                // Normalise data between 0 and 1, we know that RGB colours are in the range 0 - 255
                // so we can just divide the value by 255 - no need for min/max business
                if(!hsb) { // RGB analysis
                    pixelProperties[0] = (float) red / 255.0f;
                    pixelProperties[1] = (float) green / 255.0f;
                    pixelProperties[2] = (float) blue / 255.0f;
                } else { // HSB analysis
                    pixelProperties = Color.RGBtoHSB(red, green, blue, null);
                }
                
                double[] networkInputs = {pixelProperties[0], pixelProperties[1], pixelProperties[2]}; 

                network.setInput(networkInputs); // Sets inputs
                
                network.calculate(); // Calculate network
                double[] networkOutputs = network.getOutput();

                int classification = 0; // Initialise classification to 0

                if(networkOutputs[0] > networkOutputs[1] && networkOutputs[0] > networkOutputs[2])
                   classification = 0;
                else if(networkOutputs[1] > networkOutputs[0] && networkOutputs[1] > networkOutputs[2])
                    classification = 1;
                else               
                    classification = 2;
               
                image.setRGB(x, y, classColours[classification]); // Apply artificial colour to pixel according to its classification
            }
        }

        return image;
    }
    
    BufferedImage regionAware(BufferedImage orgImage) {
        int rgb; // Represents RGB from scanned pixel
        float[] pixelProperties = {0.0f, 0.0f, 0.0f}; // Holds pixel properties RGB or HSB
        
        BufferedImage newField;
        int imageType = orgImage.getType();
  
        int width = orgImage.getWidth();
        int height = orgImage.getHeight();
        
        int[] classColours = { // Define classification colours
            new java.awt.Color(255, 0, 0).getRGB(),
            new java.awt.Color(0, 255, 0).getRGB(),
            new java.awt.Color(0, 0, 255).getRGB()
        };
        
        newField = new BufferedImage(width, height, imageType); // Cretae new image
        
        // orgImage = neuralNet(orgImage, true); // USe HSB network


        // Scan through image file 
        for (int y = 0; y < height; y++) {            
            for (int x = 0; x < width; x++) {
                int[] bin = new int[3]; // Three bins for counting classifications
                
                for(int innerY = y - 5; innerY < y + 5; innerY++) {
                    
                    // Keep innerY in bounds
                    if(innerY < 0)
                        innerY = 0;
                    else if(innerY >= height) 
                        break; 
                    
                    // find the most popular classification in the region
                    for(int innerX = x - 5; innerX < x + 5; innerX++) {
                        // Keep innerX in bounds
                        if(innerX < 0) 
                            innerX = 0;
                        else if(innerX >= width)
                            break;
                        
                        rgb = orgImage.getRGB(innerX, innerY); // Get pixel colour
                        Color color = new Color(rgb);
                
                        // Split pixel colour into composit values;
                        int red = color.getRed();
                        int green = color.getGreen();
                        int blue = color.getBlue();
                        
                        // Total bins
                        if(red == 255) // pixel classified as soil
                            bin[0]++;
                        else if(green == 255)
                            bin[1]++;
                        else
                            bin[2]++;                        
                    }
                }
                
                // Find the most popular bin and apply it to the current pixel
                int classification;
                
                if(bin[0] > bin[1] && bin[0] > bin[2] )
                    classification = 0; 
                else if(bin[1] > bin[0] && bin[1] > bin[2])
                    classification = 1;
                else 
                    classification = 2;
                
                newField.setRGB(x, y, classColours[classification]); // Apply artificial colour to pixel according to its classification
            }
        }
        
        return newField;
    }
    
}
