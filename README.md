# FieldCropAnalysis
Masters Project

Provides an interface for visible spectrum analysis of fields. Utilises the Neuroph machine learning library.

Field can be dragged around within window - as long as not completely in view.

Sliders in combination with Mask Pixels check box provide tools for selecting and masking colour regions.

Smoothing applies simple smoothing algorithm which helps to reduce 'salt and pepper' effect when masking pixels.

Classify with Neural Network, feeds the image through trained neural network which identifies and colour codes
regions in the field:

* Green - 'healthy foliage'
* Blue, 'Stressed areas'
* Red, 'Soil or very stressed'

Classification smoothing applies much more agressive smoothing algorithm on top of classification colours.

*Interesting to note that when manually exploring the image with sliders, 
the areas in shade of trees fall far within Cyan to Blue region.
However, when applying the classifier, it correctly identifies these areas as foliage - 
Highlighted in green.

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
