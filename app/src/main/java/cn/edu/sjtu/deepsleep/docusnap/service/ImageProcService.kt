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
import android.graphics.Color
import kotlin.math.sqrt

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
    
//    // TODO: Black & White High Contrast
//    suspend fun applyHighContrast(image: Bitmap): Bitmap {
//        return image
//    }
    /**
     * Applies a high-contrast filter to a bitmap image using histogram equalization.
     * This is particularly effective for enhancing text clarity in document images.
     *
     * @param image The input Bitmap to process.
     * @return A new Bitmap with enhanced contrast.
     */
    suspend fun applyHighContrast(image: Bitmap): Bitmap {
        val width = image.width
        val height = image.height
        val newBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        newBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calculate histogram for the grayscale image
        val histogram = IntArray(256) { 0 }
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            // Use standard luminance calculation for grayscale
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            histogram[gray]++
        }

        // Calculate cumulative distribution function (CDF)
        val cdf = IntArray(256) { 0 }
        cdf[0] = histogram[0]
        for (i in 1..255) {
            cdf[i] = cdf[i - 1] + histogram[i]
        }

        // Find the minimum non-zero CDF value
        var minCdf = 0
        for (value in cdf) {
            if (value > 0) {
                minCdf = value
                break
            }
        }

        // Create the lookup table (LUT) for histogram equalization
        val lut = FloatArray(256)
        val totalPixels = (width * height).toFloat()
        for (i in 0..255) {
            lut[i] = ((cdf[i] - minCdf) / (totalPixels - minCdf)) * 255.0f
        }

        // Apply the lookup table to the pixels
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            val newGray = lut[gray].toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(newGray, newGray, newGray)
        }

        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return newBitmap
    }
    /**
     * Applies an adaptive thresholding filter to a bitmap image using Sauvola's method.
     * This is highly effective for binarizing document images with varying illumination.
     *
     * @param image The input Bitmap to process (should be grayscaled first for best results).
     * @param levels Not directly used in this adaptive implementation, but kept for signature consistency.
     * A window size parameter is used instead internally.
     * @return A new binary (black and white) Bitmap.
     */
    suspend fun applyThresholdFilter(image: Bitmap, levels: Int): Bitmap {
        val width = image.width
        val height = image.height
        val newBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        newBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val grayPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            grayPixels[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        // Parameters for Sauvola's algorithm
        val windowSize = 15 // Must be an odd number
        val k = 0.2
        val R = 128.0 // Max standard deviation

        val halfWindow = windowSize / 2
        val integralImage = IntArray(width * height)
        val integralImageSq = IntArray(width * height)

        // Calculate integral images for fast mean and standard deviation calculation
        for (y in 0 until height) {
            var sum = 0
            var sumSq = 0
            for (x in 0 until width) {
                val index = y * width + x
                sum += grayPixels[index]
                sumSq += grayPixels[index] * grayPixels[index]
                if (y == 0) {
                    integralImage[index] = sum
                    integralImageSq[index] = sumSq
                } else {
                    integralImage[index] = sum + integralImage[(y - 1) * width + x]
                    integralImageSq[index] = sumSq + integralImageSq[(y - 1) * width + x]
                }
            }
        }

        // Apply Sauvola's thresholding
        for (y in 0 until height) {
            for (x in 0 until width) {
                val x1 = (x - halfWindow).coerceIn(0, width - 1)
                val y1 = (y - halfWindow).coerceIn(0, height - 1)
                val x2 = (x + halfWindow).coerceIn(0, width - 1)
                val y2 = (y + halfWindow).coerceIn(0, height - 1)

                val count = (x2 - x1 + 1) * (y2 - y1 + 1)

                val sum = integralImage[y2 * width + x2] -
                        integralImage[y1 * width + x2] -
                        integralImage[y2 * width + x1] +
                        integralImage[y1 * width + x1]

                val sumSq = integralImageSq[y2 * width + x2] -
                        integralImageSq[y1 * width + x2] -
                        integralImageSq[y2 * width + x1] +
                        integralImageSq[y1 * width + x1]

                val mean = sum.toDouble() / count
                val stdDev = sqrt((sumSq.toDouble() / count) - (mean * mean))

                val threshold = mean * (1 + k * ((stdDev / R) - 1))

                val index = y * width + x
                pixels[index] = if (grayPixels[index] < threshold) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
            }
        }

        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return newBitmap
    }

//    /**
//     * Applies a B&W binarization filter to a bitmap.
//     * This is the core logic for your task.
//     * @param image The input bitmap to process.
//     * @param threshold The value (0-255) to use for binarization.
//     * @return A new bitmap with the filter applied.
//     */
//    /**
//     * Reduces the number of grayscale levels in a bitmap (Posterization).
//     * This can be used for binarization (levels=2) or posterization (levels=8, etc.).
//     * @param image The input bitmap.
//     * @param levels The desired number of color levels (e.g., 2 for binarization, 8 for 8-level).
//     * @return A new bitmap with the filter applied.
//     */
//    suspend fun applyThresholdFilter(image: Bitmap, levels: Int): Bitmap {
//        // Ensure levels is at least 2 to avoid division by zero.
//        if (levels < 2) return image
//
//        return withContext(Dispatchers.Default) {
//            val width = image.width
//            val height = image.height
//            val pixels = IntArray(width * height)
//            image.getPixels(pixels, 0, width, 0, 0, width, height)
//
//            // The size of each color segment.
//            val step = 256 / levels
//
//            for (i in pixels.indices) {
//                val pixel = pixels[i]
//                val r = (pixel shr 16) and 0xFF
//                val g = (pixel shr 8) and 0xFF
//                val b = pixel and 0xFF
//                val gray = (r * 0.3 + g * 0.59 + b * 0.11).toInt()
//
//                // This formula maps the gray value to one of the 'levels'.
//                val newGray = (gray / step) * step
//
//                pixels[i] = 0xFF000000.toInt() or (newGray shl 16) or (newGray shl 8) or newGray
//            }
//
//            val outputBitmap = Bitmap.createBitmap(width, height, image.config ?: Bitmap.Config.ARGB_8888)
//            outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
//            outputBitmap
//        }
//    }
    // High-Contrast Grayscale
//    suspend fun applyThresholdFilter(bitmap: Bitmap, levels: Int): Bitmap {
//        val width = bitmap.width
//        val height = bitmap.height
//        val pixels = IntArray(width * height)
//        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
//
//        // Define the contrast factor. Higher value means higher contrast. 1.0 is no change.
//        // 2.0 is a good starting point for documents.
//        val contrastFactor = 2.0
//
//        for (i in pixels.indices) {
//            val pixel = pixels[i]
//            // Calculate grayscale value (0-255).
//            val r = (pixel shr 16) and 0xFF
//            val g = (pixel shr 8) and 0xFF
//            val b = pixel and 0xFF
//            var gray = (r * 0.3 + g * 0.59 + b * 0.11).toInt()
//
//            // Apply contrast enhancement formula.
//            // This formula pushes gray values towards black or white.
//            val intercept = 128.0
//            gray = (intercept + contrastFactor * (gray - intercept)).toInt()
//
//            // Clamp the value to the 0-255 range to avoid color overflow.
//            if (gray < 0) gray = 0
//            if (gray > 255) gray = 255
//
//            // Set the new pixel color.
//            pixels[i] = 0xFF000000.toInt() or (gray shl 16) or (gray shl 8) or gray
//        }
//
//        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
//        return bitmap
//    }


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