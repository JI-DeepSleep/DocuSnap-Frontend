package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraCaptureScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    source: String = "document"
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required.")
        }
        return
    }

    var zoomRatio by remember { mutableStateOf(1f) }
    var exposureCompensation by remember { mutableStateOf(0f) }
    var isCapturing by remember { mutableStateOf(false) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<androidx.camera.core.CameraInfo?>(null) }
    val outputDirectory = remember { getOutputDirectory(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Flashlight control function
    // TODO: use a real device to check flashlight toggle functionality
    fun toggleFlashlight() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Find the back camera
            val cameraId = cameraManager.cameraIdList.find { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_BACK
            }
            
            if (cameraId != null) {
                if (isFlashlightOn) {
                    cameraManager.setTorchMode(cameraId, false)
                    isFlashlightOn = false
                } else {
                    cameraManager.setTorchMode(cameraId, true)
                    isFlashlightOn = true
                }
            }
        } catch (e: Exception) {
            Log.e("CameraCapture", "Error toggling flashlight", e)
            Toast.makeText(context, "Flashlight not available", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val preview = Preview.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        cameraControl = camera.cameraControl
                        cameraInfo = camera.cameraInfo
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                    } catch (exc: Exception) {
                        Log.e("CameraCapture", "Camera binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar
        TopAppBar(
            title = { Text("Camera") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Flash toggle can be added here if needed
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Camera controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Flashlight toggle button
                IconButton(
                    onClick = { toggleFlashlight() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (isFlashlightOn) "Turn off flashlight" else "Turn on flashlight",
                        tint = if (isFlashlightOn) Color.Yellow else Color.White
                    )
                }

                // Capture button
                Button(
                    onClick = {
                        isCapturing = true
                        // TODO: connect to docusnap database
                        val photoFile = File(
                            outputDirectory,
                            "IMG_${System.currentTimeMillis()}.jpg"
                        )
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        imageCapture.takePicture(
                            outputOptions,
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    isCapturing = false
                                    // Navigate to image processing with the photo URI on the main thread
                                    scope.launch {
                                        onNavigate("image_processing?photoUri=${Uri.fromFile(photoFile)}&source=$source")
                                    }
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    isCapturing = false
                                    Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    enabled = !isCapturing
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.Black
                        )
                    } else {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = "Capture",
                            modifier = Modifier.size(32.dp),
                            tint = Color.Black
                        )
                    }
                }

                // Gallery button
                IconButton(
                    onClick = { onNavigate("local_media?source=$source") },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

fun getOutputDirectory(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, context.packageName).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
} 