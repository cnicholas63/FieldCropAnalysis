/*
 * @author Chris Nicholas Student Number 22489355
 * Main window, sets up the main window and provides core logic
 * for the application
 * 
 */
package fieldcropanalysis;

import java.awt.Color;
import static java.awt.Color.HSBtoRGB;
import static java.awt.Color.RGBtoHSB;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.text.DecimalFormat;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class MainWindow extends javax.swing.JFrame {

    Slider startSlider; // Start colour slider
    Slider endSlider;   // End colour slider

    int mouseStartX, mouseStartY, mouseEndX, mouseEndY; // Mouse locations
    int imageOriginX, imageOriginY; // records the top left co-ordinates of image
    BufferedImage originalImage;    // Buffered image holding original image
    BufferedImage blurredImage;     // Buffered image holding blurred copy
    BufferedImage workImage;        // Buffered image holding image that is worked and displayed
    BufferedImage workImageState;   // Buffered image used to dave work image state then NN applied
    BufferedImage referenceImage; // Buffered image for main image (copy of original)
    int[] hues; // Array for holding hue analysis
    boolean maskSet; // Is the mask check box set flag
    boolean maskInvert; // Should the mask be inverted 
    boolean applyBlur; // Should the image have blur applied
    boolean applyNN;   // Should image be classified using NN
    boolean applyClassSmooth; // SHould the classification smoothing be applied
    JLabel pixelsInRange;
    Boolean onlyWithinBoundary;

    FieldInfo field; // Field information object
    FieldCropAnalysis fieldCropAnalysis; // Field Crop Analysis provides NN classification routines

    /**
     * Creates new form MainWindow
     */
    public MainWindow() {
        initComponents();

        hues = new int[360]; // Enough space for 360 hues 0 - 360 degrees (360 & 0 occupying same space)

        // Initialise GUI elements and status
        // Initialise sliders
        startSlider = new Slider(sldrStartColour, lblStartValue, pnlStartColour); // Instantiate startSlider
        endSlider = new Slider(sldrEndColour, lblEndValue, pnlEndColour); // Instantiate endSlider

        pixelsInRange = lblInRange; 
        displayColourPalette(); // Display HSB colour palette

        imageOriginX = 0; // Initialiase the image origin to top left (0, 0)
        imageOriginY = 0;

        maskSet = false;    // Mask checkbox not set
        maskInvert = false; // Invert Mask checkbox not set
        applyBlur = false;  // Blur not applied by default
        applyNN = false;     // Neural Network not applied
        applyClassSmooth = false; // Classification smoothing not applied
        onlyWithinBoundary = true; // Only sample pixels within boundary
        

        // Field information files - this should be done using file picker
        String rs = "Field_Rising_Sun_10.csv";
        String tl = "Field_Thornton_Lodge_14_Acre.csv";
        String os = "Field_Ormskirk_Sewn.csv";

        field = new FieldInfo(rs); // Instantiate new field loading image and GPS coordinates        

        originalImage = field.getFieldImage();   // Get the original field image
        blurredImage = copyImage(originalImage); // Make a copy of the original image for blurring
        workImage = copyImage(originalImage);    // Make a copy of the original image

        referenceImage = originalImage; // Reference referenceImage to work image

        blurImage(blurredImage, originalImage); // Apply blur to blurredImage
        //blurImage(blurredImage, blurredImage);

        colourAudit(onlyWithinBoundary); // Audit the HSB composition of the image within field boundary
        colourAnalysis(); 
        
        // Instantiate FieldCropAnalysis providing access to Neural Net functionality
        fieldCropAnalysis = new FieldCropAnalysis(); 

        displayImage(imageOriginX, imageOriginY); // Display the main image
    }

    /**
     * Audits the colour profile of the image
     *
     * @param Boolean withinFieldBoundary, if true only sample pixels inside
     * field boundary
     *
     */
    private void colourAudit(Boolean withinFieldBoundary) {
        float[] hsb;
        int col;
        Boolean samplePixel; // True = pixel should be sampled, False = not
        System.out.println("Colour Audit");

        for (int t = 0; t < hues.length; t++) { // RE-initialise hue counts to 0
            hues[t] = 0;
        }

        for (int y = 0; y < referenceImage.getHeight(); y++) {
            for (int x = 0; x < referenceImage.getWidth(); x++) {
                int hue;
                int saturation;
                int brightness;

                samplePixel = true; // Assume the pixel is to be sampled

                if (withinFieldBoundary) {           // If we only want to sample within field boundary
                    if (!field.isPixelInField(x, y)) // If the pixel falls outside the boundary
                    {
                        samplePixel = false;         // We don't want to sample it
                    }
                }

                if (samplePixel) { // If the pixel is to be sampled
                    col = referenceImage.getRGB(x, y); // Get the pixel colour
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
    }

    // Analyse the colours profile within selected range
    private void colourAnalysis() {
        int pixelTotal; // Used to claculate total sampled pixels
        float pixelSamples = 0.0f;
        float pixelPercent = 0.0f;
        int range = 0; // Sample range size
        int start, end, sampleLength; // Start, end and sampleLength of sample range 

        pixelTotal = 0;

        for (int t = 0; t < 360; t++) {
            pixelTotal += hues[t]; // Total pixels sammpled (this should be image width * height)     
        }

        System.out.println("Colour Analysis, PixelTotal = " + pixelTotal);

        start = startSlider.getValue();
        end = endSlider.getValue();

        if (start > end) {// Length of sample range
            sampleLength = 360 - start + end; // Going from start to end through start -> 360 -> end
        } else {
            sampleLength = end - start; // Sample between the sliders
        }
        System.out.println("Sample length: " + sampleLength);

        for (int t = 0; t < sampleLength; t++) {
            pixelSamples += hues[(start + t) % 360];
        }

        pixelPercent = pixelSamples / (float) pixelTotal * 100f;

        lblInRange.setText(String.format("%3.2f", pixelPercent));
    }

    // Tries to display the image at origin x, y
    // Allows for scrolling of image within panel - if appropriate
    private void displayImage(int x, int y) {
        int panelWidth, panelHeight; // Width and height of panel
        int imageWidth, imageHeight; // Width and height of image
        int width, height;

        panelWidth = jlblMainImage.getWidth();   // Store panel dimensions
        panelHeight = jlblMainImage.getHeight();

        imageWidth = workImage.getWidth(); // Store image dimensions
        imageHeight = workImage.getHeight();

        x = x < 0 ? 0 : x; // Check X not negative
        y = y < 0 ? 0 : y; // Check Y not negative        

        // Handle image width and X origin
        if (imageWidth > panelWidth) { // Allow to scroll on X but constrain image edges to panel edges
            width = panelWidth; // Enough image to fill th epanel

            if (x + panelWidth > imageWidth) { // Is there enough image to display horizontally?
                x = imageWidth - panelWidth;  // No shift left to make image fill panel horizontally          
            }
        } else { // Image smaller than panel so no scrolling!
            width = imageWidth; // Prevent going past right edge of image
            x = 0; // Origin to 0

        }

        // Handle image hieght and Y origin
        if (imageHeight > panelHeight) { // Allow scroll on Y but constrain image edges to panel edges
            height = panelHeight; // Enough image to fill the panel

            if (y + panelHeight > imageHeight) { // is there enough image to display vertically?
                y = imageHeight - panelHeight; // No shift down (up on screen) to make image fill panel vertically
            }

        } else { // image is vertically smaller than panel so no scrolling!
            height = imageHeight; // Prevent going past bottom of image
            y = 0; // Origin to 0
        }

        imageOriginX = x;
        imageOriginY = y;

        ImageIcon fieldImage = new ImageIcon(workImage.getSubimage(x, y, width, height));

        jlblMainImage.setIcon(fieldImage);

    }

    // Copy image to new buffered image
    private BufferedImage copyImage(BufferedImage orgImage) {
        BufferedImage copyImage;
        ColorModel cm = orgImage.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = orgImage.copyData(null);

        copyImage = new BufferedImage(cm, raster, isAlphaPremultiplied, null); // Create copy of original image

        return copyImage;
    }

    /**
     * Display the colour palette
     */
    private void displayColourPalette() {
        BufferedImage colourPalette = new BufferedImage(360, 20, TYPE_INT_RGB);

        for (int y = 0; y < colourPalette.getHeight(); y++) {
            for (int x = 0; x < 360; x++) {
                //colourPalette.setRGB(x, y, (Color.HSBtoRGB(1f/360f*(float)x, 0.75f, 0.75f).getRGB()).getRGB());
                colourPalette.setRGB(x, y, Color.getHSBColor(1f / 360f * (float) x, 0.75f, 0.75f).getRGB());
            }
        }
        jlblColourPalette.setIcon(new ImageIcon(colourPalette));
    }

    /**
     * Applies mask to the image, masking pixels outside selected colour range
     * @param boolean mask, true mask on, false mask off
     **/
    private void maskImage(boolean mask) {
        float[] hsb;
        int start, end; // Start and end slider values
        int maskColour = new Color(150, 150, 150).getRGB(); // Mask colour
        

        start = startSlider.getValue(); // Get start value
        end = endSlider.getValue(); // Get end value

        if (mask) { // Mask the pixels in the main image
            for (int y = 0; y < referenceImage.getHeight(); y++) {
                for (int x = 0; x < referenceImage.getWidth(); x++) {
                    int hue;
                    int saturation;
                    int brightness;

                    int col = referenceImage.getRGB(x, y); // Get the original pixel colour
                    hsb = getHSB(col); // Get HSB components from RGB

                    hue = (int) hsb[0];        // Get the hue
                    saturation = (int) hsb[1]; // Get the saturation
                    brightness = (int) hsb[2]; // Get the brightness
                    hue %= 360; // Make sure 360 == 0
                    //hues[hue]++; // Increment hue counter

                    if (!maskInvert) { // Normal mask
                        if (start < end) {
                            if (hue < start || hue > end) { // If pixel colour outside specified range mask it
                                col = maskColour; // Mask pixels with mid grey
                            }
                        }

                    } else // Invert mask
                    {
                        if (start < end) { // Normal look for values outside range
                            if (hue >= start && hue <= end) { // If pixel colour inside specified range mask it
                                col = maskColour; // Mask pixels with mid grey
                            }
                        } 
                    }

                    workImage.setRGB(x, y, col); // Set the pixel in work image
                }
            }
        } else { // Remove any mask from pixels - restore colours
            for (int y = 0; y < referenceImage.getHeight(); y++) {
                for (int x = 0; x < referenceImage.getWidth(); x++) {
                    int col = referenceImage.getRGB(x, y); // Get the original pixel colour

                    workImage.setRGB(x, y, col); // Restore pixels to original in work image
                }
            }
        }

        // Redraw image
        displayImage(imageOriginX, imageOriginY);
    }

    private float[] getHSB(int rgb) {
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

        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                int hue;
                int saturation;
                int brightness;

                int col = originalImage.getRGB(x, y); // Get the pixel colour
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

                        originalImage.setRGB(x, y, col);

                    }
                }
            }
        }
    }

    /**
     * Applies simple averaging blur to pixel based on immediate neighbours
     *
     * @param targetImage The image that will be blurred
     * @param originalImage The image sampled
     * @return
     */
    private void blurImage(BufferedImage targetImage, BufferedImage originalImage) {
        int width, height;

        width = targetImage.getWidth();
        height = targetImage.getHeight();
        int weight = 0; // Calculated weight of sampled pixels
        int average = 0; // Average of weigth;

        for (int y = 0; y < height; y++) {  // Scan through image sampling pixels and averaging
            for (int x = 0; x < width; x++) {
                int count = 0; // number of pixels sampled
                int startX, startY; // Start of sampling X & Y
                int endX, endY;     // End of sampling 
                int red, green, blue;          // Colour channels accumulate channel total from sampled pixels
                int sample; // Sampled pixel

                red = green = blue = 0;

                // Ensure sampling does not go out of bounds in X
                startX = x == 0 ? 0 : x - 1;
                endX = x < width - 1 ? x + 1 : x;

                // Ensure sampling does not go out of bounds in Y
                startY = y == 0 ? 0 : y - 1;
                endY = y < height - 1 ? y + 1 : y;

                // Sampling loops
                for (int sY = startY; sY <= endY; sY++) {
                    for (int sX = startX; sX <= endX; sX++) {

                        sample = originalImage.getRGB(sX, sY); // Sample the pixel from original image

                        // Seperate and accumulate channels
                        red += (sample >> 16) & 0x000000FF;
                        green += (sample >> 8) & 0x000000FF;
                        blue += (sample) & 0x000000FF;

                        count++; // track the number of pixels sampled
                    }
                }

                red /= count;   // Calculate channel averages
                green /= count;
                blue /= count;

                sample = new Color(red, green, blue).getRGB();

                //sample = (red << 16) + (green << 8) + blue; // Reconstruct sample using averaged values
                targetImage.setRGB(x, y, sample); // Set the pixel in target image
            }
        }

    }

    /**
     * ********************************
     * Event Handlers Called by NetBeans official event handlers. 
     * This allows the addition and removal of GUI components without 
     * worrying about losing event handler code. 
     * ********************************
     */
    // My handler for mouse moved over main image
    private void mainImageMouseMoved(java.awt.event.MouseEvent evt) {
        int x = evt.getX();
        int y = evt.getY();
        double xPixelScale, yPixelScale;
        Point2D.Double GPS;

        DecimalFormat numberFormat = new DecimalFormat("#.000000"); // Used for formatting double to set decimal places

        xPixelScale = field.getHorizontalPixelScale();
        yPixelScale = field.getVerticalPixelScale();

        x += imageOriginX; // Calculate position of mouse over image (taking any scrolling into account
        y += imageOriginY;

        lblCursorOutX.setText("" + x);
        lblCursorOutY.setText("" + y);

        GPS = field.getGPSFromPixel(x, y); // Get GPS coordinates

        if (field.isPixelInField(x, y)) { // Set text colour BLACK
            lblCursorOutLat.setForeground(Color.BLACK);
            lblCursorOutLong.setForeground(Color.BLACK);
            
        } else { // Set text colour RED
            lblCursorOutLat.setForeground(Color.RED);
            lblCursorOutLong.setForeground(Color.RED);
        }
        
        float[] hsb = {0.0f, 0.0f, 0.0f}; // Used for sampling HSB elements from image. Initialise to Red (0 == Red)
        
        // Colour pixel under pointer panel
        if (x >= 0 && x < referenceImage.getWidth() && y >= 0 && y < referenceImage.getHeight()) {
            hsb = getHSB(referenceImage.getRGB(x, y)); // Get the HSB from RGB
        }

        float hue = 1.0f / 360f * hsb[0]; // Normalise hue to range 0 - 1
        Color col = Color.getHSBColor(hue, 0.75f, 0.75f);
                pnlCursorColour.setBackground(col);

        // Output cursor Coordinates to six decimals        
        lblCursorOutLat.setText(numberFormat.format(-GPS.getX()));  
        lblCursorOutLong.setText(numberFormat.format(GPS.getY()));
    }

    // Mouse clicked over main image 
    private void mainImageMouseClicked(java.awt.event.MouseEvent evt) {
        mouseStartX = evt.getX(); // Get the mouse X location at click
        mouseStartY = evt.getY(); // Get the mouse Y location at click
        System.out.println("Mouse Clicked: " + mouseStartX + ", " + mouseStartY);// TODO add your handling code here: 
    }

    // Mouse dragged over main image
    private void mainImageMouseDragged(java.awt.event.MouseEvent evt) {
        // Mouse starting X and Y recorded in mousePressed
        int mouseX = evt.getX(); // Record current location of mouse
        int mouseY = evt.getY();

        int xDrag = mouseStartX - mouseX;
        int yDrag = mouseStartY - mouseY;
        displayImage(xDrag, yDrag);

    }

    // Mouse released event handler
    private void mainImageMouseReleased(java.awt.event.MouseEvent evt) {
        mouseEndX = evt.getX(); // Get the mouse X location at click
        mouseEndY = evt.getY(); // Get the mouse Y location at click
        System.out.println("Mouse released: " + mouseEndX + ", " + mouseEndY);// TODO add your handling code here:

    }

    // Mouse pressed event handler
    private void mainImageMousePressed(java.awt.event.MouseEvent evt) {
        mouseStartX = evt.getX(); // Get the mouse X location at click
        mouseStartY = evt.getY(); // Get the mouse Y location at click

        mouseStartX += imageOriginX;
        mouseStartY += imageOriginY;

        System.out.println("Mouse Pressed: " + mouseStartX + ", " + mouseStartY);// TODO add your handling code here:
    }

    // Blur selection box state changed
    private void checkBlurItemStateChanged(java.awt.event.ItemEvent evt) {
        applyBlur = !applyBlur;

        if (applyBlur) {
            referenceImage = blurredImage;
        } else {
            referenceImage = originalImage;
        }

        colourAudit(onlyWithinBoundary);
        colourAnalysis();
        maskImage(maskSet);

        displayImage(imageOriginX, imageOriginY);
        System.out.println("Blur = " + applyBlur);
    }

    // Checkbox Invert Mask state changeed
    private void checkInvertMaskItemStateChanged(java.awt.event.ItemEvent evt) {
        maskInvert = !maskInvert; // Toggle maskInvert flag

        if (maskSet) { // Only apply a mask if mask image selected (maskSet = true)
            maskImage(maskSet);
        }
    }

    // Checkbox Mask Pixels state changed
    private void checkMaskPixelsItemStateChanged(java.awt.event.ItemEvent evt) {
        System.out.println("Box clicked");
        System.out.println("State = " + evt.getStateChange());

        maskSet = !maskSet; // Toggle maskSet flag

        maskImage(maskSet); // Mask/demask pixels in image
        
        chkInvertMask.setEnabled(maskSet); // Only enable if mask is on
        
    }
    
    // Checkbox Apply Neural Network state changed
    private void checkApplyNNStateChanged(java.awt.event.ItemEvent evt) {
        System.out.println("Box clicked");
        System.out.println("State = " + evt.getStateChange());
        
        applyNN = !applyNN; 

        // Toggle enabled flag other form controls
        chkBlur.setEnabled(!applyNN); // Set to opposite of applyNN
        chkInvertMask.setEnabled(!applyNN);
        chkMaskPixels.setEnabled(!applyNN);
        
        chkClassSmooth.setEnabled(applyNN); // Only enabled if applyNN = true
        
        sldrStartColour.setEnabled(!applyNN);
        sldrEndColour.setEnabled(!applyNN);
        
        if(applyNN) { // This is a destructive process changing workImage data
            workImageState = copyImage(workImage); // Backup the work image
            
            workImage = fieldCropAnalysis.neuralNet(workImage, true); // true = Apply HSB Evaluation   
            
            // Redraw image
            displayImage(imageOriginX, imageOriginY);
        } else { // Restore original image and state
            workImage = copyImage(workImageState); // Restore work image
            // Redraw image
            displayImage(imageOriginX, imageOriginY);
           
            chkClassSmooth.setSelected(false); // Uncheck class smoothing check box
            applyClassSmooth = false;
            
        }
    }
    
    
    // Apply agressive smoothing based on classification of surrounding pixels - within 5 pixel readius
    private void checkApplyClassSmoothingStateChanged(java.awt.event.ItemEvent evt) {
        applyClassSmooth = !applyClassSmooth; // Toggle smoothing flag 
        
        if(applyClassSmooth) {
            workImage = fieldCropAnalysis.regionAware(workImage);           
            
            displayImage(imageOriginX, imageOriginY);
            System.out.println("Image displayed");
        }
    }
    
    

    // End Slider mouse released handler
    private void sliderEndColourMouseReleased(java.awt.event.MouseEvent evt) {
        if (maskSet) {
            maskImage(maskSet);
        }
    }

    // End Slider mouse dragged
    private void sliderEndColourMouseDragged(java.awt.event.MouseEvent evt) {
        endSlider.updateStatus(); // Update the status of the slider to reflect changes

        if (endSlider.getValue() < startSlider.getValue()) { // Check not lower than start
            startSlider.setValue(endSlider.getValue());

        }

        colourAnalysis(); // Analyse the colour profile for selected colour range
    }

    // Start Slider mouse released handler
    private void sliderStartColourMouseReleased(java.awt.event.MouseEvent evt) {
        if (maskSet) {
            maskImage(maskSet);
        }
    }

    // Slider start colour mouse dragged handler
    private void sliderStartColourMouseDragged(java.awt.event.MouseEvent evt) {
        startSlider.updateStatus(); // Update the status of the slider to reflect changes

        if (startSlider.getValue() > endSlider.getValue()) { // Check not higher than end
            endSlider.setValue(startSlider.getValue());

        }

        colourAnalysis(); // Analyse the colour profile for selected colour range
    }
    

    

    /**
     * This method is called from within the constructor to initialise the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpnlControls = new javax.swing.JPanel();
        sldrStartColour = new javax.swing.JSlider();
        lblStartColour = new javax.swing.JLabel();
        lblStartValue = new javax.swing.JLabel();
        pnlStartColour = new javax.swing.JPanel();
        sldrEndColour = new javax.swing.JSlider();
        lblEndColour = new javax.swing.JLabel();
        lblEndValue = new javax.swing.JLabel();
        pnlEndColour = new javax.swing.JPanel();
        jlblColourPalette = new javax.swing.JLabel();
        chkMaskPixels = new javax.swing.JCheckBox();
        chkInvertMask = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        lblInRange = new javax.swing.JLabel();
        chkBlur = new javax.swing.JCheckBox();
        lblCursorX = new javax.swing.JLabel();
        lblCursorOutX = new javax.swing.JLabel();
        lblCursorLong = new javax.swing.JLabel();
        lblCursorY = new javax.swing.JLabel();
        lblCursorOutY = new javax.swing.JLabel();
        lblCursorOutLong = new javax.swing.JLabel();
        lblCursorLat = new javax.swing.JLabel();
        lblCursorOutLat = new javax.swing.JLabel();
        pnlCursorColour = new javax.swing.JPanel();
        chkApplyNN = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        chkClassSmooth = new javax.swing.JCheckBox();
        jlblMainImage = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Field Crop Analysis 22489355");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jpnlControls.setForeground(new java.awt.Color(0, 255, 0));
        jpnlControls.setToolTipText("Represents percentage of pixels within Start Hue to End Hue range");
        jpnlControls.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jpnlControls.setFocusable(false);
        jpnlControls.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jpnlControls.setMaximumSize(new java.awt.Dimension(1067, 200));
        jpnlControls.setMinimumSize(new java.awt.Dimension(1067, 200));
        jpnlControls.setName(""); // NOI18N
        jpnlControls.setPreferredSize(new java.awt.Dimension(1067, 200));

        sldrStartColour.setMaximum(359);
        sldrStartColour.setToolTipText("");
        sldrStartColour.setPreferredSize(new java.awt.Dimension(360, 26));
        sldrStartColour.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                sldrStartColourMouseDragged(evt);
            }
        });
        sldrStartColour.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sldrStartColourMouseReleased(evt);
            }
        });

        lblStartColour.setText("Start Hue");

        lblStartValue.setText("Start");

        pnlStartColour.setBackground(new java.awt.Color(150, 150, 150));
        pnlStartColour.setFocusable(false);
        pnlStartColour.setPreferredSize(new java.awt.Dimension(30, 30));

        javax.swing.GroupLayout pnlStartColourLayout = new javax.swing.GroupLayout(pnlStartColour);
        pnlStartColour.setLayout(pnlStartColourLayout);
        pnlStartColourLayout.setHorizontalGroup(
            pnlStartColourLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        pnlStartColourLayout.setVerticalGroup(
            pnlStartColourLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );

        sldrEndColour.setMaximum(359);
        sldrEndColour.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                sldrEndColourMouseDragged(evt);
            }
        });
        sldrEndColour.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sldrEndColourMouseReleased(evt);
            }
        });

        lblEndColour.setText("End Hue");

        lblEndValue.setText("End");

        pnlEndColour.setBackground(new java.awt.Color(150, 150, 150));
        pnlEndColour.setFocusable(false);
        pnlEndColour.setPreferredSize(new java.awt.Dimension(30, 30));

        javax.swing.GroupLayout pnlEndColourLayout = new javax.swing.GroupLayout(pnlEndColour);
        pnlEndColour.setLayout(pnlEndColourLayout);
        pnlEndColourLayout.setHorizontalGroup(
            pnlEndColourLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        pnlEndColourLayout.setVerticalGroup(
            pnlEndColourLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );

        jlblColourPalette.setText("jLabel1");

        chkMaskPixels.setText("Mask pixels outside range");
        chkMaskPixels.setToolTipText("Hides pixels outside selected colour range behind grey mask");
        chkMaskPixels.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkMaskPixelsItemStateChanged(evt);
            }
        });

        chkInvertMask.setText("Invert mask (mask pixels in range)");
        chkInvertMask.setToolTipText("Inverts the mask effect, reveling what is under the mask");
        chkInvertMask.setEnabled(false);
        chkInvertMask.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkInvertMaskItemStateChanged(evt);
            }
        });
        chkInvertMask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkInvertMaskActionPerformed(evt);
            }
        });

        jLabel1.setText("% Pixels in range");

        lblInRange.setBackground(new java.awt.Color(200, 200, 200));
        lblInRange.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblInRange.setLabelFor(jLabel1);

        chkBlur.setText("Apply Smoothing");
        chkBlur.setToolTipText("Smooths the image, so helps clear a little pixellation");
        chkBlur.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkBlurItemStateChanged(evt);
            }
        });
        chkBlur.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkBlurActionPerformed(evt);
            }
        });

        lblCursorX.setText("Cursor X:");

        lblCursorOutX.setText("jLabel2");
        lblCursorOutX.setToolTipText("Pixel X location of mouse over image");

        lblCursorLong.setText("Cursor Long:");

        lblCursorY.setText("Cursor Y:");

        lblCursorOutY.setText("jLabel3");
        lblCursorOutY.setToolTipText("Pixel Y location of mouse over image");

        lblCursorOutLong.setText("jLabel2");
        lblCursorOutLong.setToolTipText("Longditude of mouse over image (Red indicates outside field boundary)");

        lblCursorLat.setText("Cursor Lat:");

        lblCursorOutLat.setText("jLabel2");
        lblCursorOutLat.setToolTipText("Latitude of mouse over image (Red indicates outside field boundary)");

        pnlCursorColour.setBackground(new java.awt.Color(150, 150, 150));
        pnlCursorColour.setFocusable(false);
        pnlCursorColour.setPreferredSize(new java.awt.Dimension(30, 30));

        javax.swing.GroupLayout pnlCursorColourLayout = new javax.swing.GroupLayout(pnlCursorColour);
        pnlCursorColour.setLayout(pnlCursorColourLayout);
        pnlCursorColourLayout.setHorizontalGroup(
            pnlCursorColourLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        pnlCursorColourLayout.setVerticalGroup(
            pnlCursorColourLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );

        chkApplyNN.setText("Classify with Neural Network:-");
        chkApplyNN.setToolTipText("Applies a neural network classifier algorithm");
        chkApplyNN.setName(""); // NOI18N
        chkApplyNN.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkApplyNNItemStateChanged(evt);
            }
        });
        chkApplyNN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkApplyNNActionPerformed(evt);
            }
        });

        jLabel2.setText("This will take a few seconds, please be patient.");

        chkClassSmooth.setText("Classification Smoothing");
        chkClassSmooth.setToolTipText("Applies strong smoothing to classified image");
        chkClassSmooth.setEnabled(false);
        chkClassSmooth.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkClassSmoothItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jpnlControlsLayout = new javax.swing.GroupLayout(jpnlControls);
        jpnlControls.setLayout(jpnlControlsLayout);
        jpnlControlsLayout.setHorizontalGroup(
            jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpnlControlsLayout.createSequentialGroup()
                .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpnlControlsLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpnlControlsLayout.createSequentialGroup()
                                .addComponent(lblEndColour, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(4, 4, 4)
                                .addComponent(sldrEndColour, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblEndValue, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jpnlControlsLayout.createSequentialGroup()
                                .addComponent(lblStartColour, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sldrStartColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblStartValue, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnlStartColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(pnlCursorColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(pnlEndColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jpnlControlsLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblCursorX, javax.swing.GroupLayout.DEFAULT_SIZE, 58, Short.MAX_VALUE)
                            .addComponent(lblCursorY, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblCursorOutX, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblCursorOutY, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(35, 35, 35)
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblCursorLong, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE)
                            .addComponent(lblCursorLat, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblCursorOutLat)
                            .addComponent(lblCursorOutLong, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jpnlControlsLayout.createSequentialGroup()
                        .addGap(72, 72, 72)
                        .addComponent(jlblColourPalette, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpnlControlsLayout.createSequentialGroup()
                        .addGap(55, 55, 55)
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkBlur)
                            .addComponent(chkMaskPixels)
                            .addComponent(chkInvertMask))
                        .addGap(12, 12, 12)
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkClassSmooth)
                            .addGroup(jpnlControlsLayout.createSequentialGroup()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(77, 77, 77)
                                .addComponent(lblInRange, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(chkApplyNN, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(184, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpnlControlsLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(98, 98, 98))))
        );
        jpnlControlsLayout.setVerticalGroup(
            jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpnlControlsLayout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpnlControlsLayout.createSequentialGroup()
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(sldrStartColour, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblStartColour, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(lblStartValue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(pnlStartColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCursorX)
                                .addComponent(lblCursorOutX)
                                .addComponent(lblCursorLong)
                                .addComponent(lblCursorOutLong))
                            .addGroup(jpnlControlsLayout.createSequentialGroup()
                                .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpnlControlsLayout.createSequentialGroup()
                                        .addComponent(jlblColourPalette, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(12, 12, 12))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpnlControlsLayout.createSequentialGroup()
                                        .addComponent(pnlCursorColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)))
                                .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(chkInvertMask)
                                        .addComponent(chkClassSmooth))
                                    .addComponent(sldrEndColour, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblEndColour, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(pnlEndColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblEndValue, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(36, 36, 36))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jpnlControlsLayout.createSequentialGroup()
                                .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(chkMaskPixels)
                                    .addComponent(chkApplyNN))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblCursorY)
                            .addComponent(lblCursorOutY)
                            .addComponent(lblCursorLat)
                            .addComponent(lblCursorOutLat))
                        .addGap(15, 15, 15))
                    .addGroup(jpnlControlsLayout.createSequentialGroup()
                        .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblInRange, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jpnlControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(chkBlur)
                                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGap(166, 166, 166))))
        );

        getContentPane().add(jpnlControls, java.awt.BorderLayout.SOUTH);

        jlblMainImage.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jlblMainImage.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jlblMainImage.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(150, 150, 150)));
        jlblMainImage.setMaximumSize(new java.awt.Dimension(1500, 600));
        jlblMainImage.setMinimumSize(new java.awt.Dimension(100, 100));
        jlblMainImage.setName("Chris"); // NOI18N
        jlblMainImage.setPreferredSize(new java.awt.Dimension(1200, 600));
        jlblMainImage.setRequestFocusEnabled(false);
        jlblMainImage.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jlblMainImageMouseDragged(evt);
            }
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jlblMainImageMouseMoved(evt);
            }
        });
        jlblMainImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jlblMainImageMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jlblMainImageMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jlblMainImageMouseReleased(evt);
            }
        });
        getContentPane().add(jlblMainImage, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Mouse moved over image event handler
    private void jlblMainImageMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlblMainImageMouseMoved
        mainImageMouseMoved(evt);
    }//GEN-LAST:event_jlblMainImageMouseMoved

    // Mouse clicked event handler
    private void jlblMainImageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlblMainImageMouseClicked
        mainImageMouseClicked(evt);
    }//GEN-LAST:event_jlblMainImageMouseClicked

    // Mouse being dragged over image
    private void jlblMainImageMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlblMainImageMouseDragged
        mainImageMouseDragged(evt);
    }//GEN-LAST:event_jlblMainImageMouseDragged

    // Mouse released event handler
    private void jlblMainImageMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlblMainImageMouseReleased
        mainImageMouseReleased(evt);

    }//GEN-LAST:event_jlblMainImageMouseReleased

    // Mouse pressed event handler
    private void jlblMainImageMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlblMainImageMousePressed
        mainImageMousePressed(evt);
    }//GEN-LAST:event_jlblMainImageMousePressed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        System.out.println("Window resized" + evt.getComponent().getWidth());
    }//GEN-LAST:event_formComponentResized

    private void chkBlurActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkBlurActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkBlurActionPerformed

    private void chkBlurItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkBlurItemStateChanged
        checkBlurItemStateChanged(evt);
    }//GEN-LAST:event_chkBlurItemStateChanged

    private void chkInvertMaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkInvertMaskActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkInvertMaskActionPerformed

    private void chkInvertMaskItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkInvertMaskItemStateChanged
        checkInvertMaskItemStateChanged(evt);
    }//GEN-LAST:event_chkInvertMaskItemStateChanged

    private void chkMaskPixelsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkMaskPixelsItemStateChanged
        checkMaskPixelsItemStateChanged(evt);
    }//GEN-LAST:event_chkMaskPixelsItemStateChanged

    // End Slider mouse released handler
    private void sldrEndColourMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sldrEndColourMouseReleased
        sliderEndColourMouseReleased(evt);
    }//GEN-LAST:event_sldrEndColourMouseReleased

    private void sldrEndColourMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sldrEndColourMouseDragged
        sliderEndColourMouseDragged(evt);
    }//GEN-LAST:event_sldrEndColourMouseDragged

    // Start Slider mouse released handler
    private void sldrStartColourMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sldrStartColourMouseReleased
        sliderStartColourMouseReleased(evt);
    }//GEN-LAST:event_sldrStartColourMouseReleased

    // Slider start colour mouse dragged handler
    private void sldrStartColourMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sldrStartColourMouseDragged
        sliderStartColourMouseDragged(evt);
    }//GEN-LAST:event_sldrStartColourMouseDragged

    private void chkApplyNNItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkApplyNNItemStateChanged
        checkApplyNNStateChanged(evt);
    }//GEN-LAST:event_chkApplyNNItemStateChanged

    private void chkApplyNNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkApplyNNActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkApplyNNActionPerformed

    private void chkClassSmoothItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkClassSmoothItemStateChanged
        checkApplyClassSmoothingStateChanged(evt);
    }//GEN-LAST:event_chkClassSmoothItemStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainWindow().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox chkApplyNN;
    private javax.swing.JCheckBox chkBlur;
    private javax.swing.JCheckBox chkClassSmooth;
    private javax.swing.JCheckBox chkInvertMask;
    private javax.swing.JCheckBox chkMaskPixels;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jlblColourPalette;
    private javax.swing.JLabel jlblMainImage;
    private javax.swing.JPanel jpnlControls;
    private javax.swing.JLabel lblCursorLat;
    private javax.swing.JLabel lblCursorLong;
    private javax.swing.JLabel lblCursorOutLat;
    private javax.swing.JLabel lblCursorOutLong;
    private javax.swing.JLabel lblCursorOutX;
    private javax.swing.JLabel lblCursorOutY;
    private javax.swing.JLabel lblCursorX;
    private javax.swing.JLabel lblCursorY;
    private javax.swing.JLabel lblEndColour;
    private javax.swing.JLabel lblEndValue;
    private javax.swing.JLabel lblInRange;
    private javax.swing.JLabel lblStartColour;
    private javax.swing.JLabel lblStartValue;
    private javax.swing.JPanel pnlCursorColour;
    private javax.swing.JPanel pnlEndColour;
    private javax.swing.JPanel pnlStartColour;
    private javax.swing.JSlider sldrEndColour;
    private javax.swing.JSlider sldrStartColour;
    // End of variables declaration//GEN-END:variables
}
