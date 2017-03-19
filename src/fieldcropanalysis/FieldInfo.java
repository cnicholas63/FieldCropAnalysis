/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fieldcropanalysis;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/**
 *
 * @author cnich
 */

public class FieldInfo {

    BufferedImage fieldImage;
//   BufferedImage outputImage;

    ArrayList<Point2D.Double> imageCorners;  // Array holding gps coordinates for the four image corners used to work out scalling and field boundary nodes
    ArrayList<Point2D.Double> fieldBoundary; // Array holding the gps coordinates of field boundary nodes
    Double scaleX; // Scale in X direction of image from coordinates to pixels
    Double scaleY; // Scale in Y direction of image from coordinates to pixels
    Double scaleFactorX = 0.0; // Scale X factor power of 10
    Double scaleFactorY = 0.0; // Scale Y factor power of 10
    double areaWidth, areaHeight; // Area width & height in GPS
    int imageWidth, imageHeight;  // Image width & height in pixels

    //String imageName = "Map Cricket Pitch Screenshot.png";
    String imageName;

    public FieldInfo(String fieldData) {
     
        System.out.println("Constructing Field");
        imageCorners = new ArrayList<>();  // Instantiate array list holding Point2D.Double coordinates for image corners
        fieldBoundary = new ArrayList<>(); // Instantiate array list holding Point2D.Double coordinates for boundary nodes

        // Read in field data: image name, Coordinates, etc.   
        readFieldData(fieldData);

        // Load the image
        fieldImage = loadFieldImage(imageName);
        imageWidth = fieldImage.getWidth();
        imageHeight = fieldImage.getHeight();

        calcScale(fieldImage.getWidth(), fieldImage.getHeight()); // Calculate the pixel scale
        
        //colourBoundary(Color.DARK_GRAY.getRGB());
        
        
        // Plot a registration point on the field at each field boundary GPS
        for (int t = 0; t < fieldBoundary.size(); t++) {
            plotPoint(fieldBoundary.get(t), Color.WHITE.getRGB());
        }

    }

    // Calculate the image scale
    private void calcScale(int imageWidth, int imageHeight) {
        areaWidth = Math.abs(imageCorners.get(0).getX() - imageCorners.get(1).getX()); // Difference between left and right of image
        areaHeight = Math.abs(imageCorners.get(0).getY() - imageCorners.get(2).getY()); // Difference between top and bottom of image

        System.out.println("Width = " + areaWidth + ", Height = " + areaHeight);
        System.out.println("Distance = " + imageCorners.get(0).distance(imageCorners.get(1)));
    }

    // Plot point at specified GPS location
    private void plotPoint(Point2D.Double point, int colour) {
        Double locationX, locationY; // Location of point mapped to pixel co-ordinates
        Double percentX, percentY; // Percentage into image of pixel location

        locationX = imageCorners.get(0).getX() - point.getX(); // get difference between pointX and originX (top left corner)
        locationY = imageCorners.get(0).getY() - point.getY(); // get difference between pointY and originY

        percentX = locationX / areaWidth;
        percentY = locationY / areaHeight;

        double xLoc = percentX * (double) imageWidth;
        double yLoc = percentY * (double) imageHeight;

        if (xLoc >= imageWidth) {
            xLoc = imageWidth - 1;
        } else if (xLoc < 0) {
            xLoc = 0;
        }

        if (yLoc >= imageHeight) {
            yLoc = imageHeight - 1;
        }
        if (yLoc < 0) {
            yLoc = 0;
        }

        fieldImage.setRGB((int) xLoc, (int) yLoc, colour); // Plot a point
    }

    // Plot a pixel at specified pixel location using Colour
    private void plotPixel(int xLoc, int yLoc, int colour) {
        if (xLoc < imageWidth && yLoc < imageHeight) {
            fieldImage.setRGB(xLoc, yLoc, colour);
        }
        //else System.out.println("Point = " + xLoc +", " + yLoc);
    }
    
    
    // Returns the GPS location from a pixel location
    public Point2D.Double getGPSFromPixel(int x, int y) {       
        double scalledX, scalledY;
        double left, right, top, bottom;
        Point2D.Double GPS;
        
        Point2D.Double topLeft, bottomRight;
        int pixelX = 0, pixelY = 0;   // image pixel coords

        topLeft = imageCorners.get(0);     // Get top left co-ordinate of image
        bottomRight = imageCorners.get(3); // Get bottom right co-ordinate of image
                
        top = Math.abs(imageCorners.get(0).y); // Top edge of image       
        left = Math.abs(topLeft.x);            // Left edge of image
        bottom = Math.abs(bottomRight.y);      // Bottom edge of image
        right = Math.abs(bottomRight.x);       // Right edge of image
        
        scalledX = left - (areaWidth / imageWidth * (double)x);  // Calculate Latitude
        scalledY = top - (areaHeight / imageHeight * (double)y); // Calculate Longitude
        
        GPS = new Point2D.Double(scalledX, scalledY);
        
        //System.out.println("Pixel GPS (x, y): " + scalledX + ", " + scalledY);
        
        return GPS;
        
    }

    // Colours the field boundary so that the field is easilly identifiable
    private void colourBoundary(int colour) {
        double left, right, top, bottom;
        Point2D.Double topLeft, bottomRight;
        int pixelX = 0, pixelY = 0;   // image pixel coords

        topLeft = imageCorners.get(0);     // Get top left co-ordinate of image
        bottomRight = imageCorners.get(3); // Get bottom right co-ordinate of image

        top = Math.abs(imageCorners.get(0).y); // Top edge of image       
        left = Math.abs(topLeft.x);            // Left edge of image
        bottom = Math.abs(bottomRight.y);      // Bottom edge of image
        right = Math.abs(bottomRight.x);       // Right edge of image

        System.out.println("AreaHeight: " + areaHeight + "\tStep: " + areaHeight / imageHeight);
        System.out.println("AreaWidth: " + areaWidth + "\tStep: " + areaWidth / imageWidth);

        for (double y = 0; y < areaHeight; y += areaHeight / imageHeight) {
            pixelX = 0; // Start in first column
            for (double x = 0; x < areaWidth; x += areaWidth / imageWidth) {
                if (!isPointInPolygon(new Point2D.Double(left - x, top - y), fieldBoundary)) {
                    plotPixel(pixelX, pixelY, colour);
                }

                pixelX++; // Increment pixel column counter
            }
            pixelY++; // Increment pixel row counter
        }
    }

    // Calculate if point is within field, true == yes, false == no
    public boolean isPixelInField(int x, int y) {
        double scalledX, scalledY;
        double left, right, top, bottom;
        
        Point2D.Double topLeft, bottomRight;
        int pixelX = 0, pixelY = 0;   // image pixel coords

        topLeft = imageCorners.get(0);     // Get top left co-ordinate of image
        bottomRight = imageCorners.get(3); // Get bottom right co-ordinate of image
                
        top = Math.abs(imageCorners.get(0).y); // Top edge of image       
        left = Math.abs(topLeft.x);            // Left edge of image
        bottom = Math.abs(bottomRight.y);      // Bottom edge of image
        right = Math.abs(bottomRight.x);       // Right edge of image
        
        scalledX = left - (areaWidth / imageWidth * (double)x);
        scalledY = top - (areaHeight / imageHeight * (double)y);
        
        //System.out.println("Scalled (x, y): " + scalledX + ", " + scalledY);
        
        return isPointInPolygon(new Point2D.Double(scalledX, scalledY), fieldBoundary);

    }
    

    // Returns the horizontal pixel to area ratio 
    public double getHorizontalPixelScale() {
        return areaWidth / imageWidth;
    }
    
    // Returns the vertical pixel to area ratio
    public double getVerticalPixelScale() {
        return areaHeight / imageHeight;
    }
    
    // Calculates if specified point falls on or within field boundary
    private boolean isPointInPolygon(Point2D.Double p, ArrayList<Point2D.Double> polygon) {
        double minX = polygon.get(0).x; // prime minimum and maximum with first point in polygon
        double maxX = polygon.get(0).x;
        double minY = polygon.get(0).y;
        double maxY = polygon.get(0).y;

        for (int t = 1; t < polygon.size(); t++) { // Find the boundary of the polygon
            Point2D.Double q = polygon.get(t);
            minX = Math.min(q.x, minX);
            maxX = Math.max(q.x, maxX);
            minY = Math.min(q.y, minY);
            maxY = Math.max(q.y, maxY);
        }

        // If the point is outside polygon boundary ignore it
        if (p.x < minX || p.x > maxX || p.y < minY || p.y > maxY) {
            return false;
        }

        /* Test if point (P) is within polygon using Crossing Number algorithm
        *  If P is within the polygon the crossing number will be odd
        *  If P is outside the polygon crossing number will be even
        *  This algorithm toggles boolean between false and true each time a boundary is crossed
        *  True = inside, false = outside
        *  based on code at http://stackoverflow.com/questions/217578/how-can-i-determine-whether-a-2d-point-is-within-a-polygon        
         */
        boolean inside = false;

        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            if ((polygon.get(i).y > p.y) != (polygon.get(j).y > p.y)
                    && p.x < (polygon.get(j).x - polygon.get(i).x) * (p.y - polygon.get(i).y) / (polygon.get(j).y - polygon.get(i).y) + polygon.get(i).x) {
                inside = !inside;
            }
        }

        return inside;
    }

    // Load the image
    private BufferedImage loadFieldImage(String imageName) {
        BufferedImage image;

        try {
            image = ImageIO.read(new File(imageName)); // Read the file from disk
        } catch (IOException e) {
            System.out.println("File not loaded: " + imageName); // If the file is not loaded supply 100x100 image
            image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        }

        return image;
    }

    // Return the field image 
    public BufferedImage getFieldImage() {
        return fieldImage;
    }

    // Read field information from CSV file
    private void readFieldData(String csvFile) {
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ","; // CSV delimeter
        String imageFile;
        Point2D.Double coordinate; // Parsed CSV latitude and longitude
        int index;

        try {
            br = new BufferedReader(new FileReader(csvFile)); // Buffered reader to file
            index = 0; // Array index

            // Read the image file name from the CSV - this should be the first line
            imageFile = br.readLine();
            imageName = imageFile; // Assign image name from CSV
            
            if (imageName.contains(",")) { // If there is a comma in the image name - this is not an image name
                 throw new IllegalArgumentException();
            }
            
            System.out.println("Image Name: " + imageName);

            // REad in the file one line at a time and split into CSV values Latitude and Longitude            
            while ((line = br.readLine()) != null) { // Read a single CSV line t
                // use comma as separator
                String[] coordCSV = line.split(cvsSplitBy); // Split the line into lat, long     

                // Latitude and Longitude are Y and X respectively, so ned to swap the order read from file, lat = coordCSV[1]
                coordinate = new Point2D.Double(Math.abs(Double.parseDouble(coordCSV[1])), Math.abs(Double.parseDouble(coordCSV[0])));

                if (index < 4) { // First 4 lines should be image corner co-ordinates
                    imageCorners.add(coordinate);
                } else { // Remainder of lines should be coundary co-ordinates
                    fieldBoundary.add(coordinate);
                }

                System.out.println("Coordinate read: " + coordCSV[0] + ", " + coordCSV[1]);
                index++; // Increment array index

            }
            System.out.println("Coordinate pairs read = " + index);

        } catch (FileNotFoundException e) { // Catch and report any errors
            System.err.println("Error File not found: " + csvFile);
            System.exit(0);
        } catch (IOException e) {
            System.err.println("IO error reading: " + csvFile);
            System.exit(0);
        } catch (IllegalArgumentException e) { // Field image name not found in file
            System.err.println("Illegal Argument Exception, first line of field data MUST be field image name: " + csvFile);
            System.exit(0);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.err.println("File buffer not read fully: " + csvFile);
                    System.exit(0);
                }
            }
        }
    }

}
