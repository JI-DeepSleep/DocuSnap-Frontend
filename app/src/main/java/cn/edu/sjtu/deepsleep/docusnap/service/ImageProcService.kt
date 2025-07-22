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
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.*
import kotlin.math.*
// Manager of all image processing functionalities
// Interact with image processing page controller to display operations

class ImageProcService(private val context: Context) {

    private fun applyCannyEdgeDetection(pixels: IntArray, width: Int, height: Int, 
                                   lowThreshold: Int = 50, highThreshold: Int = 150): IntArray {
    
        // Step 1: Apply Sobel to get gradient magnitude and direction
        val (gradientMagnitude, gradientDirection) = calculateGradientMagnitudeAndDirection(pixels, width, height)
        
        // Step 2: Non-maximum suppression
        val suppressedEdges = nonMaximumSuppression(gradientMagnitude, gradientDirection, width, height)
        
        // Step 3: Double thresholding
        val thresholdedEdges = doubleThresholding(suppressedEdges, width, height, lowThreshold, highThreshold)
        
        // Step 4: Edge tracking by hysteresis
        return edgeTrackingByHysteresis(thresholdedEdges, width, height)
    }

    /**
    * Calculate gradient magnitude and direction using Sobel operators
    */
    private fun calculateGradientMagnitudeAndDirection(pixels: IntArray, width: Int, height: Int): Pair<IntArray, FloatArray> {
        val sobelX = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        val sobelY = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )
        
        val magnitude = IntArray(width * height)
        val direction = FloatArray(width * height)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gx = 0
                var gy = 0
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixelIndex = (y + ky) * width + (x + kx)
                        val pixelValue = pixels[pixelIndex]
                        gx += pixelValue * sobelX[ky + 1][kx + 1]
                        gy += pixelValue * sobelY[ky + 1][kx + 1]
                    }
                }
                
                val index = y * width + x
                magnitude[index] = sqrt((gx * gx + gy * gy).toDouble()).toInt().coerceIn(0, 255)
                direction[index] = atan2(gy.toDouble(), gx.toDouble()).toFloat()
            }
        }
        
        return Pair(magnitude, direction)
    }

    /**
    * Non-maximum suppression - thin edges to single pixel width
    */
    private fun nonMaximumSuppression(magnitude: IntArray, direction: FloatArray, width: Int, height: Int): IntArray {
        val result = IntArray(width * height)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                val angle = direction[index]
                val mag = magnitude[index]
                
                // Convert angle to 0-180 degrees and determine direction
                var adjustedAngle = angle * 180.0 / PI
                if (adjustedAngle < 0) adjustedAngle += 180.0
                
                var neighbor1 = 0
                var neighbor2 = 0
                
                // Determine neighbors based on gradient direction
                when {
                    (adjustedAngle >= 0 && adjustedAngle < 22.5) || (adjustedAngle >= 157.5 && adjustedAngle <= 180) -> {
                        // Horizontal edge (0 degrees)
                        neighbor1 = magnitude[y * width + (x - 1)]
                        neighbor2 = magnitude[y * width + (x + 1)]
                    }
                    adjustedAngle >= 22.5 && adjustedAngle < 67.5 -> {
                        // Diagonal edge (45 degrees)
                        neighbor1 = magnitude[(y - 1) * width + (x + 1)]
                        neighbor2 = magnitude[(y + 1) * width + (x - 1)]
                    }
                    adjustedAngle >= 67.5 && adjustedAngle < 112.5 -> {
                        // Vertical edge (90 degrees)
                        neighbor1 = magnitude[(y - 1) * width + x]
                        neighbor2 = magnitude[(y + 1) * width + x]
                    }
                    adjustedAngle >= 112.5 && adjustedAngle < 157.5 -> {
                        // Diagonal edge (135 degrees)
                        neighbor1 = magnitude[(y - 1) * width + (x - 1)]
                        neighbor2 = magnitude[(y + 1) * width + (x + 1)]
                    }
                }
                
                // Suppress if current pixel is not a local maximum
                result[index] = if (mag >= neighbor1 && mag >= neighbor2) mag else 0
            }
        }
        
        return result
    }

    /**
    * Double thresholding - classify pixels as strong, weak, or non-edge
    */
    private fun doubleThresholding(magnitude: IntArray, width: Int, height: Int, 
                                lowThreshold: Int, highThreshold: Int): IntArray {
        val result = IntArray(width * height)
        
        for (i in magnitude.indices) {
            val mag = magnitude[i]
            result[i] = when {
                mag >= highThreshold -> 255  // Strong edge
                mag >= lowThreshold -> 128   // Weak edge
                else -> 0                    // Non-edge
            }
        }
        
        return result
    }

    /**
    * Edge tracking by hysteresis - connect weak edges to strong edges
    */
    private fun edgeTrackingByHysteresis(thresholded: IntArray, width: Int, height: Int): IntArray {
        val result = thresholded.copyOf()
        val visited = BooleanArray(width * height)
        
        // Find all strong edges and trace connected weak edges
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                if (result[index] == 255 && !visited[index]) {
                    // Strong edge found, trace connected weak edges
                    traceEdge(result, visited, width, height, x, y)
                }
            }
        }
        
        // Remove remaining weak edges (not connected to strong edges)
        for (i in result.indices) {
            if (result[i] == 128) {
                result[i] = 0
            }
        }
        
        return result
    }

    /**
    * Recursively trace connected weak edges from a strong edge
    */
    private fun traceEdge(edges: IntArray, visited: BooleanArray, width: Int, height: Int, x: Int, y: Int) {
        val stack = mutableListOf<Point>()
        stack.add(Point(x, y))
        
        while (stack.isNotEmpty()) {
            val point = stack.removeAt(stack.size - 1)
            val px = point.x
            val py = point.y
            
            if (px < 0 || px >= width || py < 0 || py >= height) continue
            
            val index = py * width + px
            if (visited[index]) continue
            
            visited[index] = true
            
            // If this is a weak edge, promote it to strong edge
            if (edges[index] == 128) {
                edges[index] = 255
            }
            
            // Check 8-connected neighbors
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    
                    val nx = px + dx
                    val ny = py + dy
                    
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        val neighborIndex = ny * width + nx
                        if (!visited[neighborIndex] && (edges[neighborIndex] == 128 || edges[neighborIndex] == 255)) {
                            stack.add(Point(nx, ny))
                        }
                    }
                }
            }
        }
    }

    /**
    * Apply morphological dilation to thicken edges and fill small gaps
    */
    private fun applyDilation(binary: IntArray, width: Int, height: Int, iterations: Int = 1): IntArray {
        var result = binary.copyOf()
        
        // 3x3 structuring element (cross shape)
        val structElement = arrayOf(
            intArrayOf(0, 1, 0),
            intArrayOf(1, 1, 1),
            intArrayOf(0, 1, 0)
        )
        
        repeat(iterations) {
            val temp = IntArray(width * height)
            
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    var maxValue = 0
                    
                    // Apply structuring element
                    for (sy in -1..1) {
                        for (sx in -1..1) {
                            if (structElement[sy + 1][sx + 1] == 1) {
                                val pixelIndex = (y + sy) * width + (x + sx)
                                maxValue = maxOf(maxValue, result[pixelIndex])
                            }
                        }
                    }
                    
                    temp[y * width + x] = maxValue
                }
            }
            
            // Copy borders
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (y == 0 || y == height - 1 || x == 0 || x == width - 1) {
                        temp[y * width + x] = result[y * width + x]
                    }
                }
            }
            
            result = temp
        }
        
        return result
    }

    /**
    * Apply morphological erosion to thin edges and remove noise
    */
    private fun applyErosion(binary: IntArray, width: Int, height: Int, iterations: Int = 1): IntArray {
        var result = binary.copyOf()
        
        // 3x3 structuring element (cross shape)
        val structElement = arrayOf(
            intArrayOf(0, 1, 0),
            intArrayOf(1, 1, 1),
            intArrayOf(0, 1, 0)
        )
        
        repeat(iterations) {
            val temp = IntArray(width * height)
            
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    var minValue = 255
                    
                    // Apply structuring element
                    for (sy in -1..1) {
                        for (sx in -1..1) {
                            if (structElement[sy + 1][sx + 1] == 1) {
                                val pixelIndex = (y + sy) * width + (x + sx)
                                minValue = minOf(minValue, result[pixelIndex])
                            }
                        }
                    }
                    
                    temp[y * width + x] = minValue
                }
            }
            
            // Copy borders
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (y == 0 || y == height - 1 || x == 0 || x == width - 1) {
                        temp[y * width + x] = result[y * width + x]
                    }
                }
            }
            
            result = temp
        }
        
        return result
    }

    /**
    * Calculate contour area using cross product method
    * Points are sorted by angle to center before calculation
    */
    private fun calculateContourArea(contour: List<Point>): Double {
        if (contour.size < 3) return 0.0
        
        // Calculate center point
        val centerX = contour.sumOf { it.x }.toDouble() / contour.size
        val centerY = contour.sumOf { it.y }.toDouble() / contour.size
        
        // Sort points by angle to center
        val sortedContour = contour.sortedBy { point ->
            atan2((point.y - centerY), (point.x - centerX))
        }
        
        // Calculate area using cross product (shoelace formula)
        var area = 0.0
        val n = sortedContour.size
        
        for (i in 0 until n) {
            val j = (i + 1) % n
            val xi = sortedContour[i].x.toDouble()
            val yi = sortedContour[i].y.toDouble()
            val xj = sortedContour[j].x.toDouble()
            val yj = sortedContour[j].y.toDouble()
            
            area += xi * yj - xj * yi
        }
        
        return abs(area) / 2.0
    }

    /**
    * Sort contour points in clockwise order around the centroid
    * This ensures clean line drawing between consecutive points
    */
    private fun sortContourPoints(contour: List<Point>): List<Point> {
        if (contour.size < 3) return contour
        
        // Calculate centroid
        val centerX = contour.sumOf { it.x }.toDouble() / contour.size
        val centerY = contour.sumOf { it.y }.toDouble() / contour.size
        
        // Sort points by angle from centroid (clockwise order)
        return contour.sortedBy { point ->
            atan2((point.y - centerY), (point.x - centerX))
        }
    }

    /**
    * Update findDocumentCorners to use the new corner detection method
    */
    suspend fun findDocumentCorners(image: Bitmap): Array<PointF>? {
        return withContext(Dispatchers.Default) {
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            image.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // Convert to grayscale
            val grayPixels = IntArray(width * height)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                grayPixels[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
            
            // Apply stronger Gaussian blur to reduce noise
            val blurred = applyGaussianBlur(grayPixels, width, height)
            
            // Apply Canny edge detection
            val edges = applyCannyEdgeDetection(blurred, width, height, lowThreshold = 50, highThreshold = 100)
            
            // Apply dilation to thicken edges and connect nearby edge pixels
            val dilatedEdges = applyDilation(edges, width, height, iterations = 2)
            
            // Apply erosion to clean up and thin the edges
            val cleanedEdges = applyErosion(dilatedEdges, width, height, iterations = 1)
            
            // Find contours and get the largest one by area
            val contours = findContoursFromBinary(cleanedEdges, width, height)
            val largestContour = contours.maxByOrNull { calculateContourArea(it) }
            
            return@withContext if (largestContour != null && largestContour.size >= 20) {
                findCornersFromContour(largestContour)
            } else {
                null
            }
        }
    }

    /**
    * Applies Gaussian blur to reduce noise before edge detection
    */
    private fun applyGaussianBlur(pixels: IntArray, width: Int, height: Int): IntArray {
        // Use a 5x5 Gaussian kernel with sigma ≈ 1.4 for stronger blur
        val kernel = arrayOf(
            intArrayOf(1, 4, 7, 4, 1),
            intArrayOf(4, 16, 26, 16, 4),
            intArrayOf(7, 26, 41, 26, 7),
            intArrayOf(4, 16, 26, 16, 4),
            intArrayOf(1, 4, 7, 4, 1)
        )
        val kernelSum = 273 // Sum of all kernel values
        val kernelRadius = 2 // 5x5 kernel has radius 2
        
        val result = IntArray(width * height)
        
        // Apply the blur with proper boundary handling
        for (y in kernelRadius until height - kernelRadius) {
            for (x in kernelRadius until width - kernelRadius) {
                var sum = 0
                
                for (ky in -kernelRadius..kernelRadius) {
                    for (kx in -kernelRadius..kernelRadius) {
                        val pixelIndex = (y + ky) * width + (x + kx)
                        val kernelValue = kernel[ky + kernelRadius][kx + kernelRadius]
                        sum += pixels[pixelIndex] * kernelValue
                    }
                }
                
                result[y * width + x] = sum / kernelSum
            }
        }
        
        // Handle borders by copying edge pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (y < kernelRadius || y >= height - kernelRadius || 
                    x < kernelRadius || x >= width - kernelRadius) {
                    result[y * width + x] = pixels[y * width + x]
                }
            }
        }
        
        return result
    }

    suspend fun debugCannyEdgeDetection(image: Bitmap, lowThreshold: Int = 50, highThreshold: Int = 100): Bitmap {
        return withContext(Dispatchers.Default) {
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            image.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // Convert to grayscale
            val grayPixels = IntArray(width * height)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                grayPixels[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
            
            // Apply Gaussian blur
            val blurred = applyGaussianBlur(grayPixels, width, height)
            
            // Apply Canny edge detection
            val edges = applyCannyEdgeDetection(blurred, width, height, lowThreshold, highThreshold)
            
            // Apply dilation to thicken edges and connect nearby edge pixels
            val dilatedEdges = applyDilation(edges, width, height, iterations = 1)
            
            // Apply erosion to clean up and thin the edges  
            val finalEdges = applyErosion(dilatedEdges, width, height, iterations = 1)
            
            // Convert final result back to bitmap for visualization
            val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val resultPixels = IntArray(width * height)
            
            for (i in finalEdges.indices) {
                val edgeValue = finalEdges[i]
                resultPixels[i] = Color.rgb(edgeValue, edgeValue, edgeValue)
            }
            
            resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
            
            // Add debug info
            val canvas = Canvas(resultBitmap)
            val debugPaint = Paint().apply {
                color = Color.RED
                textSize = 30f
                isAntiAlias = true
            }
            
            canvas.drawText("Canny + Opening (Erode→Dilate)", 20f, 50f, debugPaint)
            canvas.drawText("Low: $lowThreshold, High: $highThreshold", 20f, 90f, debugPaint)
            canvas.drawText("Morphology: Erode(1) + Dilate(1)", 20f, 130f, debugPaint)
            
            resultBitmap
        }
    }

    /**
    * Find corners using distance-based method instead of polygon approximation
    */
    private fun findCornersFromContour(contour: List<Point>): Array<PointF>? {
        if (contour.size < 10) return null
        
        // Step 1: Calculate center point
        val centerX = contour.sumOf { it.x }.toFloat() / contour.size
        val centerY = contour.sumOf { it.y }.toFloat() / contour.size
        val center = PointF(centerX, centerY)
        
        // Step 2: Find the farthest point from center (first corner)
        var maxDistance = 0.0
        var corner1: Point? = null
        
        for (point in contour) {
            val distance = sqrt(((point.x - centerX) * (point.x - centerX) + (point.y - centerY) * (point.y - centerY)).toDouble())
            if (distance > maxDistance) {
                maxDistance = distance
                corner1 = point
            }
        }
        
        if (corner1 == null) return null
        
        // Step 3: Find the farthest point from first corner (second corner)
        maxDistance = 0.0
        var corner2: Point? = null
        
        for (point in contour) {
            val distance = sqrt(((point.x - corner1.x) * (point.x - corner1.x) + (point.y - corner1.y) * (point.y - corner1.y)).toDouble())
            if (distance > maxDistance) {
                maxDistance = distance
                corner2 = point
            }
        }
        
        if (corner2 == null) return null
        
        // Step 4: Find the two points with maximum positive and negative distance to the line formed by corner1 and corner2
        var maxPositiveDistance = 0.0
        var maxNegativeDistance = 0.0
        var corner3: Point? = null
        var corner4: Point? = null
        
        for (point in contour) {
            // Skip if this is corner1 or corner2
            if (point == corner1 || point == corner2) continue
            
            // Calculate signed distance to line
            val signedDistance = pointToLineSignedDistance(point, corner1, corner2)
            
            if (signedDistance > maxPositiveDistance) {
                maxPositiveDistance = signedDistance
                corner3 = point
            } else if (signedDistance < maxNegativeDistance) {
                maxNegativeDistance = signedDistance
                corner4 = point
            }
        }
        
        if (corner3 == null || corner4 == null) return null
        
        // Convert to PointF array
        val corners = arrayOf(
            PointF(corner1.x.toFloat(), corner1.y.toFloat()),
            PointF(corner2.x.toFloat(), corner2.y.toFloat()),
            PointF(corner3.x.toFloat(), corner3.y.toFloat()),
            PointF(corner4.x.toFloat(), corner4.y.toFloat())
        )
        
        // Order the corners properly (TL, TR, BR, BL)
        return orderCorners(corners.toList())
    }

    /**
    * Calculate signed distance from point to line (positive on one side, negative on the other)
    */
    private fun pointToLineSignedDistance(point: Point, lineStart: Point, lineEnd: Point): Double {
        val A = (lineEnd.y - lineStart.y).toDouble()
        val B = (lineStart.x - lineEnd.x).toDouble()
        val C = (lineEnd.x * lineStart.y - lineStart.x * lineEnd.y).toDouble()
        
        return (A * point.x + B * point.y + C) / sqrt(A * A + B * B)
    }

    /**
     * Debug function: shows all contours found from Canny edge detection
     * This helps visualize what contours are actually being detected
     */
    suspend fun debugContoursOnly(image: Bitmap, lowThreshold: Int = 50, highThreshold: Int = 100): Bitmap {
        return withContext(Dispatchers.Default) {
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            image.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // Convert to grayscale
            val grayPixels = IntArray(width * height)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                grayPixels[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
            
            // Apply Gaussian blur to reduce noise
            val blurred = applyGaussianBlur(grayPixels, width, height)
            
            // Apply Canny edge detection
            val edges = applyCannyEdgeDetection(blurred, width, height, lowThreshold, highThreshold)

            // Apply dilation to thicken edges and connect nearby edge pixels
            val dilatedEdges = applyDilation(edges, width, height, iterations = 1)
            
            // Apply erosion to clean up and thin the edges
            val cleanedEdges = applyErosion(dilatedEdges, width, height, iterations = 1)
            
            // Find contours and sort by area (largest first)
            val contours = findContoursFromBinary(cleanedEdges, width, height)
                .sortedByDescending { calculateContourArea(it) }
            
            // Start with original image for better visibility
            val resultBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBitmap)
            
            // Draw all contours in different colors
            val colors = arrayOf(
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, 
                Color.CYAN, Color.MAGENTA, Color.WHITE, Color.LTGRAY
            )
            
            for ((index, contour) in contours.withIndex().take(8)) { // Show first 8 contours
                if (contour.size < 5) continue // Skip very small contours
                
                val contourPaint = Paint().apply {
                    color = colors[index % colors.size]
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                
                // Draw contour as connected lines
                for (i in 0 until contour.size - 1) {
                    val start = contour[i]
                    val end = contour[i + 1]
                    canvas.drawLine(start.x.toFloat(), start.y.toFloat(), 
                                   end.x.toFloat(), end.y.toFloat(), contourPaint)
                }
                
                // Connect last point to first to close the contour
                if (contour.size > 2) {
                    val last = contour.last()
                    val first = contour.first()
                    canvas.drawLine(last.x.toFloat(), last.y.toFloat(),
                                   first.x.toFloat(), first.y.toFloat(), contourPaint)
                }
                
                // Draw contour number at the center
                if (contour.isNotEmpty()) {
                    val centerX = contour.sumOf { it.x }.toFloat() / contour.size
                    val centerY = contour.sumOf { it.y }.toFloat() / contour.size
                    
                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 24f
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    
                    val backgroundPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.FILL
                    }
                    
                    val text = "${index + 1}"
                    val textBounds = Rect()
                    textPaint.getTextBounds(text, 0, text.length, textBounds)
                    
                    val bgRect = RectF(
                        centerX - textBounds.width() / 2 - 4f,
                        centerY - textBounds.height() / 2 - 4f,
                        centerX + textBounds.width() / 2 + 4f,
                        centerY + textBounds.height() / 2 + 4f
                    )
                    canvas.drawRect(bgRect, backgroundPaint)
                    canvas.drawText(text, centerX - textBounds.width() / 2, centerY + textBounds.height() / 2, textPaint)
                }
            }
            
            // Add debug info
            val debugPaint = Paint().apply {
                color = Color.BLACK
                textSize = 25f
                isAntiAlias = true
            }
            
            val debugBackgroundPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }
            
            val infoHeight = 150f + (contours.take(8).size * 25f)
            // canvas.drawRect(10f, 10f, 400f, infoHeight, debugBackgroundPaint)
            canvas.drawText("Contour Detection Debug (Area-based)", 20f, 35f, debugPaint)
            canvas.drawText("Total contours: ${contours.size}", 20f, 65f, debugPaint)
            canvas.drawText("Sorted by area (largest first)", 20f, 95f, debugPaint)
            canvas.drawText("Canny + Opening (Erode→Dilate)", 20f, 125f, debugPaint)
            
            // Show contour details with area information
            for ((index, contour) in contours.withIndex().take(8)) {
                val color = colors[index % colors.size]
                val colorName = when (color) {
                    Color.RED -> "Red"
                    Color.GREEN -> "Green"
                    Color.BLUE -> "Blue"
                    Color.YELLOW -> "Yellow"
                    Color.CYAN -> "Cyan"
                    Color.MAGENTA -> "Magenta"
                    Color.WHITE -> "White"
                    else -> "Orange"
                }
                val area = calculateContourArea(contour)
                val info = "${index + 1}. $colorName: ${contour.size}pts, ${area.toInt()}px²"
                canvas.drawText(info, 20f, 155f + index * 25f, debugPaint)
            }
            
            resultBitmap
        }
    }

    /**
    * Updated debugContourDetection to use the new corner detection method
    */
    suspend fun debugContourDetection(image: Bitmap, lowThreshold: Int = 50, highThreshold: Int = 100): Bitmap {
        return withContext(Dispatchers.Default) {
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            image.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // Convert to grayscale
            val grayPixels = IntArray(width * height)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                grayPixels[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
            
            // Apply Gaussian blur to reduce noise
            val blurred = applyGaussianBlur(grayPixels, width, height)
            
            // Apply Canny edge detection
            val edges = applyCannyEdgeDetection(blurred, width, height, lowThreshold, highThreshold)
            
        // Apply dilation to thicken edges and connect nearby edge pixels
        val dilatedEdges = applyDilation(edges, width, height, iterations = 1)
        
        // Apply erosion to clean up and thin the edges
        val cleanedEdges = applyErosion(dilatedEdges, width, height, iterations = 1)            // Find contours and get the largest one by area
            val contours = findContoursFromBinary(cleanedEdges, width, height)
            val largestContour = contours.maxByOrNull { calculateContourArea(it) }
            
            // Start with original image for better visibility
            val resultBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBitmap)
            
            // Get the largest contour only
//            val largestContour = contours.maxByOrNull { it.size }
            
            if (largestContour != null && largestContour.size >= 10) {
                // Draw the largest contour in bright red
                val contourPaint = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    isAntiAlias = true
                }
                
                // Draw contour as connected lines
                for (i in 0 until largestContour.size - 1) {
                    val start = largestContour[i]
                    val end = largestContour[i + 1]
                    canvas.drawLine(start.x.toFloat(), start.y.toFloat(), 
                                end.x.toFloat(), end.y.toFloat(), contourPaint)
                }
                
                // Connect last point to first to close the contour
                if (largestContour.size > 2) {
                    val last = largestContour.last()
                    val first = largestContour.first()
                    canvas.drawLine(last.x.toFloat(), last.y.toFloat(),
                                first.x.toFloat(), first.y.toFloat(), contourPaint)
                }
                
                // Draw center point for visualization
                val centerX = largestContour.sumOf { it.x }.toFloat() / largestContour.size
                val centerY = largestContour.sumOf { it.y }.toFloat() / largestContour.size
                
                val centerPaint = Paint().apply {
                    color = Color.YELLOW
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(centerX, centerY, 8f, centerPaint)
                
                // Try to find corners using the new distance-based method
                val detectedCorners = findCornersFromContour(largestContour)
                
                if (detectedCorners != null && detectedCorners.size == 4) {
                    // Draw the quadrilateral formed by corners
                    val quadPaint = Paint().apply {
                        color = Color.GREEN
                        style = Paint.Style.STROKE
                        strokeWidth = 6f
                        isAntiAlias = true
                    }
                    
                    // Draw the quadrilateral
                    for (i in 0 until detectedCorners.size) {
                        val start = detectedCorners[i]
                        val end = detectedCorners[(i + 1) % detectedCorners.size]
                        canvas.drawLine(start.x, start.y, end.x, end.y, quadPaint)
                    }
                    
                    // Draw corner points with labels
                    val cornerPaint = Paint().apply {
                        color = Color.BLUE
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    
                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 30f
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    
                    val backgroundPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.FILL
                    }
                    
                    val labels = arrayOf("TL", "TR", "BR", "BL") // Top-Left, Top-Right, Bottom-Right, Bottom-Left
                    
                    for (i in detectedCorners.indices) {
                        val corner = detectedCorners[i]
                        val label = labels[i]
                        
                        // Draw corner circle
                        canvas.drawCircle(corner.x, corner.y, 12f, cornerPaint)
                        
                        // Draw label with background
                        val textBounds = Rect()
                        textPaint.getTextBounds(label, 0, label.length, textBounds)
                        
                        val textX = corner.x + 20f
                        val textY = corner.y - 10f
                        
                        val backgroundRect = RectF(
                            textX - 5f,
                            textY - textBounds.height() - 5f,
                            textX + textBounds.width() + 5f,
                            textY + 5f
                        )
                        canvas.drawRect(backgroundRect, backgroundPaint)
                        canvas.drawText(label, textX, textY, textPaint)
                    }
                    
                    // Calculate area
                    val area = calculatePolygonArea(detectedCorners.toList())
                    val minArea = width * height * 0.05
                    
                    // Add detailed debug info
                    val debugPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 25f
                        isAntiAlias = true
                    }
                    
                    val debugBackgroundPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.FILL
                    }
                    
                    // canvas.drawRect(10f, 10f, 450f, 280f, debugBackgroundPaint)
                    val contourArea = calculateContourArea(largestContour)
                    canvas.drawText("Area-Based Corner Detection", 20f, 35f, debugPaint)
                    canvas.drawText("Contour points: ${largestContour.size}", 20f, 65f, debugPaint)
                    canvas.drawText("Contour area: ${contourArea.toInt()}px²", 20f, 95f, debugPaint)
                    canvas.drawText("Corners found: YES (4)", 20f, 125f, debugPaint)
                    canvas.drawText("Area: ${area.toInt()} (min: ${minArea.toInt()})", 20f, 155f, debugPaint)
                    canvas.drawText("Area check: ${if (area > minArea) "PASS" else "FAIL"}", 20f, 185f, debugPaint)
                    
                    // Show corner coordinates
                    for (i in detectedCorners.indices) {
                        val corner = detectedCorners[i]
                        val coordText = "${labels[i]}: (${corner.x.toInt()}, ${corner.y.toInt()})"
                        canvas.drawText(coordText, 20f, 215f + (i * 25f), debugPaint)
                    }
                    
                } else {
                    // No corners found
                    val debugPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 25f
                        isAntiAlias = true
                    }
                    
                    val backgroundPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.FILL
                    }
                    
                    canvas.drawRect(10f, 10f, 450f, 140f, backgroundPaint)
                    val contourArea = calculateContourArea(largestContour)
                    canvas.drawText("Area-Based Corner Detection", 20f, 35f, debugPaint)
                    canvas.drawText("Contour points: ${largestContour.size}", 20f, 65f, debugPaint)
                    canvas.drawText("Contour area: ${contourArea.toInt()}px²", 20f, 95f, debugPaint)
                    canvas.drawText("Corners found: NO", 20f, 125f, debugPaint)
                }
                
            } else {
                // No contours found
                val debugPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 25f
                    isAntiAlias = true
                }
                
                val backgroundPaint = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.FILL
                }
                
                canvas.drawRect(10f, 10f, 350f, 100f, backgroundPaint)
                canvas.drawText("NO CONTOURS FOUND", 20f, 35f, debugPaint)
                canvas.drawText("Total contours: ${contours.size}", 20f, 65f, debugPaint)
            }
            
            resultBitmap
        }
    }

    /**
    * Simplified contour finding for binary Canny edge output
    * Since Canny gives us clean binary edges, we just need to trace connected components
    */
    private fun findContoursFromBinary(binary: IntArray, width: Int, height: Int): List<List<Point>> {
        val visited = BooleanArray(width * height)
        val contours = mutableListOf<List<Point>>()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (binary[index] == 255 && !visited[index]) {
                    val contour = mutableListOf<Point>()
                    traceContour(binary, visited, width, height, x, y, contour)
                    if (contour.size > 10) { // Filter very small contours
                        // Sort contour points in proper order for clean line drawing
                        val sortedContour = sortContourPoints(contour)
                        contours.add(sortedContour)
                    }
                }
            }
        }
        
        return contours // Return contours without sorting, let calling function decide
    }

    /**
    * Trace a single contour using 8-connectivity
    */
    private fun traceContour(binary: IntArray, visited: BooleanArray, width: Int, height: Int, 
                            startX: Int, startY: Int, contour: MutableList<Point>) {
        val stack = mutableListOf<Point>()
        stack.add(Point(startX, startY))
        
        while (stack.isNotEmpty()) {
            val point = stack.removeAt(stack.size - 1)
            val x = point.x
            val y = point.y
            
            if (x < 0 || x >= width || y < 0 || y >= height) continue
            
            val index = y * width + x
            if (visited[index] || binary[index] != 255) continue
            
            visited[index] = true
            contour.add(Point(x, y))
            
            // Add 8-connected neighbors
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx != 0 || dy != 0) {
                        stack.add(Point(x + dx, y + dy))
                    }
                }
            }
        }
    }

    /**
    * Updated findLargestQuadrilateral to work with Canny output
    */
    private fun findLargestQuadrilateral(edges: IntArray, width: Int, height: Int): Array<PointF>? {
        // Canny output is already binary, no need for additional thresholding
        val contours = findContoursFromBinary(edges, width, height)
        
        // Find the largest contour that can be approximated as a quadrilateral
        var largestArea = 0.0
        var bestQuad: Array<PointF>? = null
        val minArea = width * height * 0.05 // Minimum 5% of image area
        
        for (contour in contours) {
            if (contour.size < 20) continue // Need sufficient points for good approximation
            
            // Try different epsilon values for polygon approximation
            val epsilons = arrayOf(0.01, 0.02, 0.03, 0.04, 0.05)
            
            for (epsilon in epsilons) {
                val approx = approximatePolygon(contour, epsilon)
                
                if (approx.size == 4) {
                    val area = calculatePolygonArea(approx)
                    if (area > largestArea && area > minArea) {
                        // Additional check: ensure it's roughly rectangular
                        if (isRoughlyRectangular(approx)) {
                            largestArea = area
                            bestQuad = orderCorners(approx)
                            break // Found good quad with this epsilon, move to next contour
                        }
                    }
                }
            }
        }
        
        return bestQuad
    }

    /**
    * Check if a quadrilateral is roughly rectangular (not too skewed)
    */
    private fun isRoughlyRectangular(corners: List<PointF>): Boolean {
        if (corners.size != 4) return false
        
        // Calculate angles between consecutive sides
        val angles = mutableListOf<Double>()
        
        for (i in 0 until 4) {
            val p1 = corners[i]
            val p2 = corners[(i + 1) % 4]
            val p3 = corners[(i + 2) % 4]
            
            val v1x = p1.x - p2.x
            val v1y = p1.y - p2.y
            val v2x = p3.x - p2.x
            val v2y = p3.y - p2.y
            
            val dot = v1x * v2x + v1y * v2y
            val mag1 = sqrt(v1x * v1x + v1y * v1y)
            val mag2 = sqrt(v2x * v2x + v2y * v2y)
            
            if (mag1 > 0 && mag2 > 0) {
                val cosAngle = dot / (mag1 * mag2)
                val angle = acos(cosAngle.coerceIn((-1.0).toFloat(), 1.0F)) * 180.0 / PI
                angles.add(angle)
            }
        }
        
        // Check if angles are roughly 90 degrees (allow 60-120 degree range)
        return angles.all { it in 60.0..120.0 }
    }

    /**
    * Douglas-Peucker algorithm for polygon approximation
    */
    private fun approximatePolygon(contour: List<Point>, epsilon: Double): List<PointF> {
        val perimeter = calculatePerimeter(contour)
        val actualEpsilon = epsilon * perimeter
        return douglasPeucker(contour.map { PointF(it.x.toFloat(), it.y.toFloat()) }, actualEpsilon)
    }

    private fun douglasPeucker(points: List<PointF>, epsilon: Double): List<PointF> {
        if (points.size <= 2) return points
        
        var maxDistance = 0.0
        var index = 0
        
        val start = points.first()
        val end = points.last()
        
        for (i in 1 until points.size - 1) {
            val distance = pointToLineDistance(points[i], start, end)
            if (distance > maxDistance) {
                maxDistance = distance.toDouble()
                index = i
            }
        }
        
        return if (maxDistance > epsilon) {
            val rec1 = douglasPeucker(points.subList(0, index + 1), epsilon)
            val rec2 = douglasPeucker(points.subList(index, points.size), epsilon)
            rec1.dropLast(1) + rec2
        } else {
            listOf(start, end)
        }
    }

    /**
    * Calculate distance from point to line
    */
    private fun pointToLineDistance(point: PointF, lineStart: PointF, lineEnd: PointF): Float {
        val A = lineEnd.y - lineStart.y
        val B = lineStart.x - lineEnd.x
        val C = lineEnd.x * lineStart.y - lineStart.x * lineEnd.y
        
        return abs(A * point.x + B * point.y + C) / sqrt(A * A + B * B)
    }

    /**
    * Calculate polygon area
    */
    private fun calculatePolygonArea(points: List<PointF>): Double {
        var area = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].x * points[j].y
            area -= points[j].x * points[i].y
        }
        return abs(area) / 2.0
    }

    /**
    * Calculate perimeter of contour
    */
    private fun calculatePerimeter(contour: List<Point>): Double {
        var perimeter = 0.0
        for (i in 0 until contour.size - 1) {
            val dx = contour[i + 1].x - contour[i].x
            val dy = contour[i + 1].y - contour[i].y
            perimeter += sqrt((dx * dx + dy * dy).toDouble())
        }
        return perimeter
    }

    /**
    * Orders corners as: top-left, top-right, bottom-right, bottom-left
    */
    private fun orderCorners(corners: List<PointF>): Array<PointF> {
        val ordered = Array(4) { PointF(0f, 0f) }
        
        // Calculate center point
        val centerX = corners.sumOf { it.x.toDouble() }.toFloat() / 4
        val centerY = corners.sumOf { it.y.toDouble() }.toFloat() / 4
        
        for (corner in corners) {
            if (corner.x < centerX && corner.y < centerY) {
                ordered[0] = corner // top-left
            } else if (corner.x > centerX && corner.y < centerY) {
                ordered[1] = corner // top-right
            } else if (corner.x > centerX && corner.y > centerY) {
                ordered[2] = corner // bottom-right
            } else {
                ordered[3] = corner // bottom-left
            }
        }
        
        return ordered
    }

    private fun performPerspectiveCorrection(image: Bitmap, corners: Array<PointF>): Bitmap {
        // Calculate the dimensions of the output document
        val topLeft = corners[0]
        val topRight = corners[1] 
        val bottomRight = corners[2]
        val bottomLeft = corners[3]
        
        // Calculate width and height of the corrected document
        val topWidth = sqrt(((topRight.x - topLeft.x) * (topRight.x - topLeft.x) + 
                            (topRight.y - topLeft.y) * (topRight.y - topLeft.y)).toDouble()).toFloat()
        val bottomWidth = sqrt(((bottomRight.x - bottomLeft.x) * (bottomRight.x - bottomLeft.x) + 
                            (bottomRight.y - bottomLeft.y) * (bottomRight.y - bottomLeft.y)).toDouble()).toFloat()
        val leftHeight = sqrt(((bottomLeft.x - topLeft.x) * (bottomLeft.x - topLeft.x) + 
                            (bottomLeft.y - topLeft.y) * (bottomLeft.y - topLeft.y)).toDouble()).toFloat()
        val rightHeight = sqrt(((bottomRight.x - topRight.x) * (bottomRight.x - topRight.x) + 
                            (bottomRight.y - topRight.y) * (bottomRight.y - topRight.y)).toDouble()).toFloat()
        
        // Use the maximum dimensions to preserve all content
        val outputWidth = maxOf(topWidth, bottomWidth).toInt()
        val outputHeight = maxOf(leftHeight, rightHeight).toInt()
        
        // Define destination rectangle (perfect rectangle)
        val dst = floatArrayOf(
            0f, 0f,                           // top-left
            outputWidth.toFloat(), 0f,        // top-right
            outputWidth.toFloat(), outputHeight.toFloat(), // bottom-right
            0f, outputHeight.toFloat()        // bottom-left
        )
        
        // Source quadrilateral (detected corners)
        val src = floatArrayOf(
            topLeft.x, topLeft.y,             // top-left
            topRight.x, topRight.y,           // top-right
            bottomRight.x, bottomRight.y,     // bottom-right
            bottomLeft.x, bottomLeft.y        // bottom-left
        )
        
        // Create transformation matrix
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        
        // Create the corrected bitmap
        val correctedBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(correctedBitmap)
        
        // Fill with white background
        canvas.drawColor(Color.WHITE)
        
        // Apply the perspective transformation
        canvas.drawBitmap(image, matrix, Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        })
        
        return correctedBitmap
    }

    /**
    * Public method for applying perspective correction with user-adjusted corners
    */
    suspend fun performPerspectiveCorrectionWithCorners(image: Bitmap, corners: Array<PointF>): Bitmap {
        return withContext(Dispatchers.Default) {
            performPerspectiveCorrection(image, corners)
        }
    }

    // Alternative implementation using manual perspective transformation for more control
    private fun performPerspectiveCorrectionManual(image: Bitmap, corners: Array<PointF>): Bitmap {
        val topLeft = corners[0]
        val topRight = corners[1] 
        val bottomRight = corners[2]
        val bottomLeft = corners[3]
        
        // Calculate output dimensions
        val topWidth = sqrt(((topRight.x - topLeft.x) * (topRight.x - topLeft.x) + 
                            (topRight.y - topLeft.y) * (topRight.y - topLeft.y)).toDouble()).toFloat()
        val bottomWidth = sqrt(((bottomRight.x - bottomLeft.x) * (bottomRight.x - bottomLeft.x) + 
                            (bottomRight.y - bottomLeft.y) * (bottomRight.y - bottomLeft.y)).toDouble()).toFloat()
        val leftHeight = sqrt(((bottomLeft.x - topLeft.x) * (bottomLeft.x - topLeft.x) + 
                            (bottomLeft.y - topLeft.y) * (bottomLeft.y - topLeft.y)).toDouble()).toFloat()
        val rightHeight = sqrt(((bottomRight.x - topRight.x) * (bottomRight.x - topRight.x) + 
                            (bottomRight.y - topRight.y) * (bottomRight.y - topRight.y)).toDouble()).toFloat()
        
        val outputWidth = maxOf(topWidth, bottomWidth).toInt()
        val outputHeight = maxOf(leftHeight, rightHeight).toInt()
        
        // Create output bitmap
        val correctedBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(outputWidth * outputHeight)
        
        // Get source image pixels
        val sourcePixels = IntArray(image.width * image.height)
        image.getPixels(sourcePixels, 0, image.width, 0, 0, image.width, image.height)
        
        // Calculate perspective transformation coefficients
        val srcPoints = arrayOf(topLeft, topRight, bottomRight, bottomLeft)
        val dstPoints = arrayOf(
            PointF(0f, 0f),
            PointF(outputWidth.toFloat(), 0f),
            PointF(outputWidth.toFloat(), outputHeight.toFloat()),
            PointF(0f, outputHeight.toFloat())
        )
        
        // For each pixel in the output image, find corresponding pixel in source
        for (y in 0 until outputHeight) {
            for (x in 0 until outputWidth) {
                // Apply inverse perspective transformation
                val sourcePoint = inverseTransform(
                    PointF(x.toFloat(), y.toFloat()),
                    srcPoints, dstPoints
                )
                
                if (sourcePoint != null && 
                    sourcePoint.x >= 0 && sourcePoint.x < image.width &&
                    sourcePoint.y >= 0 && sourcePoint.y < image.height) {
                    
                    // Bilinear interpolation for better quality
                    val color = bilinearInterpolation(sourcePixels, image.width, image.height, sourcePoint.x, sourcePoint.y)
                    pixels[y * outputWidth + x] = color
                } else {
                    // Fill with white if outside source bounds
                    pixels[y * outputWidth + x] = Color.WHITE
                }
            }
        }
        
        correctedBitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
        return correctedBitmap
    }

    /**
    * Bilinear interpolation for smoother image transformation
    */
    private fun bilinearInterpolation(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
        val x1 = x.toInt()
        val y1 = y.toInt()
        val x2 = (x1 + 1).coerceAtMost(width - 1)
        val y2 = (y1 + 1).coerceAtMost(height - 1)
        
        val fx = x - x1
        val fy = y - y1
        
        val p1 = pixels[y1 * width + x1]
        val p2 = pixels[y1 * width + x2]
        val p3 = pixels[y2 * width + x1]
        val p4 = pixels[y2 * width + x2]
        
        val r1 = Color.red(p1)
        val g1 = Color.green(p1)
        val b1 = Color.blue(p1)
        
        val r2 = Color.red(p2)
        val g2 = Color.green(p2)
        val b2 = Color.blue(p2)
        
        val r3 = Color.red(p3)
        val g3 = Color.green(p3)
        val b3 = Color.blue(p3)
        
        val r4 = Color.red(p4)
        val g4 = Color.green(p4)
        val b4 = Color.blue(p4)
        
        val r = ((r1 * (1 - fx) + r2 * fx) * (1 - fy) + (r3 * (1 - fx) + r4 * fx) * fy).toInt()
        val g = ((g1 * (1 - fx) + g2 * fx) * (1 - fy) + (g3 * (1 - fx) + g4 * fx) * fy).toInt()
        val b = ((b1 * (1 - fx) + b2 * fx) * (1 - fy) + (b3 * (1 - fx) + b4 * fx) * fy).toInt()
        
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    /**
    * Simple inverse transformation (for demonstration - you might want to implement proper perspective inverse)
    */
    private fun inverseTransform(dstPoint: PointF, srcCorners: Array<PointF>, dstCorners: Array<PointF>): PointF? {
        // This is a simplified bilinear inverse transformation
        // For more accuracy, you'd implement proper perspective inverse transformation
        
        val u = dstPoint.x / dstCorners[1].x  // normalized x (0-1)
        val v = dstPoint.y / dstCorners[2].y  // normalized y (0-1)
        
        // Bilinear interpolation in source quadrilateral
        val top = PointF(
            srcCorners[0].x + u * (srcCorners[1].x - srcCorners[0].x),
            srcCorners[0].y + u * (srcCorners[1].y - srcCorners[0].y)
        )
        
        val bottom = PointF(
            srcCorners[3].x + u * (srcCorners[2].x - srcCorners[3].x),
            srcCorners[3].y + u * (srcCorners[2].y - srcCorners[3].y)
        )
        
        return PointF(
            top.x + v * (bottom.x - top.x),
            top.y + v * (bottom.y - top.y)
        )
    }

    // Update the correctPerspective function to use corner detection
    suspend fun correctPerspective(image: Bitmap): Bitmap {

        // For debugging all contours found:
//         return debugContoursOnly(image, 30, 60)
        
        // For debugging corner detection on largest contour:
//         return debugContourDetection(image, 50, 100)
        
        // For debugging Canny edge detection:
//         return debugCannyEdgeDetection(image, 50, 100)

        val corners = findDocumentCorners(image)
        
        return if (corners != null) {
            // Use detected corners
            performPerspectiveCorrection(image, corners)
        } else {
            // Fallback: return original image if no corners detected
            image
        }
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