package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import android.graphics.Bitmap


// Manager of all image processing functionalities
// Interact with image processing page controller to display operations

class ImageProcService(private val context: Context) {

    // TODO: correct perspective, return four points of the file
    suspend fun correctPerspective(image: Bitmap) {
    }
    
    // TODO: Color Enhancement
    suspend fun enhanceColors(image: Bitmap) {
    }
    
    // TODO: Black & White High Contrast
    suspend fun applyHighContrast(image: Bitmap) {
    }
    
    // TODO: B&W Threshold Filter
    suspend fun applyThresholdFilter(image: Bitmap, threshold: Int) {
    }

    // TODO: Auto-enhance image, perspective correction + B&W threshold filter
    suspend fun autoProcessing(image: Bitmap) {
    }

    // TODO: todo
} 