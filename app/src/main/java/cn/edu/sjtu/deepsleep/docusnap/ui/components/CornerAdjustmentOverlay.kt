package cn.edu.sjtu.deepsleep.docusnap.ui.components

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun CornerAdjustmentOverlay(
    bitmap: Bitmap,
    corners: Array<PointF>,
    onCornerMoved: (Int, PointF) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    
    // Local state for immediate visual updates
    var displayCorners by remember(corners) { 
        mutableStateOf(corners.map { PointF(it.x, it.y) }.toTypedArray()) 
    }
    
    // Sync display corners with prop changes
    LaunchedEffect(corners) {
        displayCorners = corners.map { PointF(it.x, it.y) }.toTypedArray()
    }
    
    // Calculate scaling factor to fit bitmap in canvas
    val scale = remember(bitmap, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            minOf(
                canvasSize.width.toFloat() / bitmap.width,
                canvasSize.height.toFloat() / bitmap.height
            )
        } else 1f
    }
    
    // Calculate offset to center the scaled bitmap
    val offset = remember(bitmap, canvasSize, scale) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale
            Offset(
                x = (canvasSize.width - scaledWidth) / 2f,
                y = (canvasSize.height - scaledHeight) / 2f
            )
        } else Offset.Zero
    }
    
    // Convert image coordinates to canvas coordinates
    fun imageToCanvas(point: PointF): Offset {
        return Offset(
            x = offset.x + point.x * scale,
            y = offset.y + point.y * scale
        )
    }
    
    // Convert canvas coordinates to image coordinates
    fun canvasToImage(canvasOffset: Offset): PointF {
        return PointF(
            (canvasOffset.x - offset.x) / scale,
            (canvasOffset.y - offset.y) / scale
        )
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Main canvas for image and corner visualization
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    canvasSize = coordinates.size
                }
                .pointerInput(Unit) {
                    var draggedCornerIndex = -1
                    
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            Log.d("CornerAdjustment", "Touch detected at: ${startOffset.x}, ${startOffset.y}")
                            Log.d("CornerAdjustment", "Scale: $scale, Offset: ${offset.x}, ${offset.y}")
                            
                            // Find the closest corner to the touch point using display corners
                            var closestCornerIndex = -1
                            var minDistance = Float.MAX_VALUE
                            
                            displayCorners.forEachIndexed { index, corner ->
                                val cornerCanvasX = offset.x + corner.x * scale
                                val cornerCanvasY = offset.y + corner.y * scale
                                val distance = sqrt(
                                    (startOffset.x - cornerCanvasX) * (startOffset.x - cornerCanvasX) +
                                    (startOffset.y - cornerCanvasY) * (startOffset.y - cornerCanvasY)
                                )
                                Log.d("CornerAdjustment", "Corner $index: image(${corner.x}, ${corner.y}) -> canvas($cornerCanvasX, $cornerCanvasY), distance: $distance")
                                
                                if (distance < minDistance && distance < 100f) { // Larger touch radius
                                    minDistance = distance
                                    closestCornerIndex = index
                                }
                            }
                            
                            draggedCornerIndex = closestCornerIndex
                            Log.d("CornerAdjustment", "Selected corner: $draggedCornerIndex (min distance: $minDistance)")
                        },
                        onDrag = { change, _ ->
                            if (draggedCornerIndex != -1) {
                                Log.d("CornerAdjustment", "Dragging corner $draggedCornerIndex to: ${change.position.x}, ${change.position.y}")
                                
                                // Convert canvas position to image coordinates
                                val imageX = (change.position.x - offset.x) / scale
                                val imageY = (change.position.y - offset.y) / scale
                                
                                // Clamp to image bounds
                                val clampedX = imageX.coerceIn(0f, bitmap.width.toFloat())
                                val clampedY = imageY.coerceIn(0f, bitmap.height.toFloat())
                                
                                Log.d("CornerAdjustment", "New image coordinates: ($clampedX, $clampedY)")
                                
                                // Update display corners immediately for smooth visual feedback
                                // Create new array to trigger recomposition
                                displayCorners = displayCorners.mapIndexed { index, corner ->
                                    if (index == draggedCornerIndex) {
                                        PointF(clampedX, clampedY)
                                    } else {
                                        PointF(corner.x, corner.y)
                                    }
                                }.toTypedArray()
                                
                                // Also update the ViewModel
                                onCornerMoved(draggedCornerIndex, PointF(clampedX, clampedY))
                                
                                // Consume the change to prevent scrolling
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            Log.d("CornerAdjustment", "Drag ended for corner: $draggedCornerIndex")
                            draggedCornerIndex = -1
                        }
                    )
                }
        ) {
            // Draw the bitmap
            if (canvasSize.width > 0 && canvasSize.height > 0) {
                drawImage(
                    image = bitmap.asImageBitmap(),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bitmap.width, bitmap.height),
                    dstOffset = IntOffset(offset.x.toInt(), offset.y.toInt()),
                    dstSize = IntSize(
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt()
                    )
                )
                
                // Draw quadrilateral overlay using display corners
                drawQuadrilateral(displayCorners, scale, offset)
                
                // Draw corner handles using display corners
                drawCornerHandles(displayCorners, scale, offset)
            }
        }
    }
}

private fun DrawScope.drawQuadrilateral(
    corners: Array<PointF>,
    scale: Float,
    offset: Offset
) {
    if (corners.size != 4) return
    
    val path = Path()
    val firstCorner = Offset(
        x = offset.x + corners[0].x * scale,
        y = offset.y + corners[0].y * scale
    )
    path.moveTo(firstCorner.x, firstCorner.y)
    
    for (i in 1 until 4) {
        val corner = Offset(
            x = offset.x + corners[i].x * scale,
            y = offset.y + corners[i].y * scale
        )
        path.lineTo(corner.x, corner.y)
    }
    path.close()
    
    // Draw filled quadrilateral with very light overlay
    drawPath(
        path = path,
        color = Color.Blue.copy(alpha = 0.1f)
    )
    
    // Draw quadrilateral border with dashed effect
    drawPath(
        path = path,
        color = Color.Blue.copy(alpha = 0.8f),
        style = Stroke(
            width = 4.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    )
}

private fun DrawScope.drawCornerHandles(
    corners: Array<PointF>,
    scale: Float,
    offset: Offset
) {
    val handleRadius = 20.dp.toPx() // Increased size for better touch
    val labels = arrayOf("TL", "TR", "BR", "BL")
    
    corners.forEachIndexed { index, corner ->
        val center = Offset(
            x = offset.x + corner.x * scale,
            y = offset.y + corner.y * scale
        )
        
        // Draw outer circle (white background with shadow effect)
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = handleRadius + 4.dp.toPx(),
            center = center.copy(x = center.x + 2.dp.toPx(), y = center.y + 2.dp.toPx())
        )
        
        drawCircle(
            color = Color.White,
            radius = handleRadius + 2.dp.toPx(),
            center = center
        )
        
        // Draw inner circle (colored handle)
        drawCircle(
            color = Color.Blue,
            radius = handleRadius,
            center = center
        )
        
        // Draw inner highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = handleRadius * 0.6f,
            center = center
        )
    }
}
