# FieldCropAnalysis

Part of my Master’s project. The aim was to develop a protoype which provides a gentle introduction into the analysis of remote sensed images. 

Today, remote sensing often involves the analysis of images taken by satellite - usually outside of the visible spectrum. The acquisition and analysis of such images can be an expensive, time consuming and a technical process. This prototype, allows the investigation of a crop using the visible spectrum and images acquired via drone for example.

Provide an image with a set of GPS field boundary coordinates and the user can explore the field within the visible spectrum and with the ability to identify areas using gps coordinates. 

The user can highlight areas of interest, masked regions and has full control over the visible spectral range.

The system employs a neural network model which when applied provides an automated analysis of the image categorising regions into one of three zones:

* Green - 'healthy foliage'
* Blue, 'Stressed areas'
* Red, 'Soil or very stressed'

The field can be dragged around within window - as long as not completely in view.

Sliders in combination with Mask Pixels check box provide tools for selecting and masking colour regions.

Smoothing applies simple smoothing algorithm which helps to reduce 'salt and pepper' effect when masking pixels.

Classification smoothing applies much more agressive smoothing algorithm on top of classification colours.

*Interesting to note that when manually exploring the image with sliders, the areas in deep shade of trees fall far within Cyan to Blue region. However, when applying the classifier, these areas are correctly identified as foliage - Highlighted in green.

Status line shows the mouse pixel coordinate and longitude/latitude over field.

# Screen shots:
Field no selection:

<img width="400" src="https://github.com/cnicholas63/FieldCropAnalysis/blob/master/ScreenShots/FieldNoSelection.png" alt="Field no selection">

Field with masked out selection:

<img width="400" src="https://github.com/cnicholas63/FieldCropAnalysis/blob/master/ScreenShots/FieldWithSelection.png" alt="Field no selection">

Field with classifier applied:

<img width="400" src="https://github.com/cnicholas63/FieldCropAnalysis/blob/master/ScreenShots/FieldClassifierApplied.png" alt="Field no selection">

Field with smoothed classifier applied:

<img width="400" src="https://github.com/cnicholas63/FieldCropAnalysis/blob/master/ScreenShots/FieldClassifierSmoothed.png" alt="Field no selection">
