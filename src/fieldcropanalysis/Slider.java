/*
 * @author C Nicholas
 */
package fieldcropanalysis;

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;


    /**
     * Slider class links the elements that make up the slider components:
     * slider, value label and colour panel
     *
     */
    class Slider {
        protected JSlider sliderValue;
        protected JLabel valueLabel;
        protected JPanel colourPanel;

        // Constructor
        public Slider(JSlider slider, JLabel value, JPanel colour) {
            this.sliderValue = slider;
            this.valueLabel = value;
            this.colourPanel = colour;

            updateStatus();
        }

        // Update slider status by interrogating slider value using default saturation and brightness
        public void updateStatus() {
            Color colour;
            float hue, sat, bri;

            hue = 1.0f / 360.0f * (float) sliderValue.getValue(); // Normalise hue 0 - 360 -> 0.0 - 1.0
            sat = 1.0f / 100.0f * 0.75f; // Normalise brightness 0 - 100 -> 0.0 - 1.0
            bri = 1.0f / 100.0f * 0.75f; // Normalise brightness 0 - 100 -> 0.0 - 1.0

            colour = new Color(Color.HSBtoRGB(hue, sat, bri));
            colourPanel.setBackground(colour);

            // Display slider value
            valueLabel.setText(String.valueOf(sliderValue.getValue()));

            setColourPanel(sliderValue.getValue(), 75, 75); // Use default saturation and brightness: 75%          
        }
        
        // Returns the reading from the slider
        public int getValue() {
            return sliderValue.getValue();
        }    
        
        // Set the slider value
        public void setValue(int val) {
            sliderValue.setValue(val); // Update the slider to val
            
            updateStatus(); // Update the slider status
        }
        

        // Sets the colour panel
        private void setColourPanel(int h, int s, int b) {
            Color colour;
            float hue, sat, bri;

            hue = 1.0f / 360.0f * (float) h; // Normalise hue 0 - 360 -> 0.0 - 1.0
            sat = 1.0f / 100.0f * (float) s; // Normalise brightness 0 - 100 -> 0.0 - 1.0
            bri = 1.0f / 100.0f * (float) b; // Normalise brightness 0 - 100 -> 0.0 - 1.0

            colour = new Color(Color.HSBtoRGB(hue, sat, bri));
            colourPanel.setBackground(colour);
        }
    }

