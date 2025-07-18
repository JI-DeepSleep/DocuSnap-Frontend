package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


// Manager of all image processing functionalities
// Interact with image processing page controller to display operations

class ImageProcService(private val context: Context) {

    // TODO: correct perspective, return four points of the file
    suspend fun correctPerspective(image: Bitmap): Bitmap {
        return image
    }
    
    // TODO: Color Enhancement
    suspend fun enhanceColors(image: Bitmap): Bitmap {
        return image
    }
    
    // TODO: Black & White High Contrast
    suspend fun applyHighContrast(image: Bitmap): Bitmap {
        return image
    }

    /**
     * Applies a B&W binarization filter to a bitmap.
     * This is the core logic for your task.
     * @param image The input bitmap to process.
     * @param threshold The value (0-255) to use for binarization.
     * @return A new bitmap with the filter applied.
     */
    /**
     * Reduces the number of grayscale levels in a bitmap (Posterization).
     * This can be used for binarization (levels=2) or posterization (levels=8, etc.).
     * @param image The input bitmap.
     * @param levels The desired number of color levels (e.g., 2 for binarization, 8 for 8-level).
     * @return A new bitmap with the filter applied.
     */
    suspend fun applyThresholdFilter(image: Bitmap, levels: Int): Bitmap {
        // Ensure levels is at least 2 to avoid division by zero.
        if (levels < 2) return image

        return withContext(Dispatchers.Default) {
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            image.getPixels(pixels, 0, width, 0, 0, width, height)

            // The size of each color segment.
            val step = 256 / levels

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val gray = (r * 0.3 + g * 0.59 + b * 0.11).toInt()

                // This formula maps the gray value to one of the 'levels'.
                val newGray = (gray / step) * step

                pixels[i] = 0xFF000000.toInt() or (newGray shl 16) or (newGray shl 8) or newGray
            }

            val outputBitmap = Bitmap.createBitmap(width, height, image.config ?: Bitmap.Config.ARGB_8888)
            outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            outputBitmap
        }
    }
    // High-Contrast Grayscale
    suspend fun applyContrastGrayScale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Define the contrast factor. Higher value means higher contrast. 1.0 is no change.
        // 2.0 is a good starting point for documents.
        val contrastFactor = 2.0

        for (i in pixels.indices) {
            val pixel = pixels[i]
            // Calculate grayscale value (0-255).
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            var gray = (r * 0.3 + g * 0.59 + b * 0.11).toInt()

            // Apply contrast enhancement formula.
            // This formula pushes gray values towards black or white.
            val intercept = 128.0
            gray = (intercept + contrastFactor * (gray - intercept)).toInt()

            // Clamp the value to the 0-255 range to avoid color overflow.
            if (gray < 0) gray = 0
            if (gray > 255) gray = 255

            // Set the new pixel color.
            pixels[i] = 0xFF000000.toInt() or (gray shl 16) or (gray shl 8) or gray
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }


    // TODO: Auto-enhance image, perspective correction + B&W threshold filter
    suspend fun autoProcessing(image: Bitmap): Bitmap {
        return image
    }

    // TODO: todo
    private fun uriToBitmap(uriString: String?): Bitmap? {
        if (uriString == null) return null
        return try {
            val imageUri = Uri.parse(uriString)
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            val listener = ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
                val targetSize = 1080
                val width = info.size.width
                val height = info.size.height
                if (width > targetSize || height > targetSize) {
                    val scale = if (width > height) targetSize.toFloat() / width else targetSize.toFloat() / height
                    decoder.setTargetSize((width * scale).toInt(), (height * scale).toInt())
                }
            }
            ImageDecoder.decodeBitmap(source, listener).copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "processed_image_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            stream.close()
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 