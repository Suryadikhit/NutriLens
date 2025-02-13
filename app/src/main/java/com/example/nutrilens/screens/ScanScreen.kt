package com.example.nutrilens.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.nutrilens.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("RememberReturnType")
@Composable
fun ScanScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var manualBarcode by remember { mutableStateOf("") }


    val currentCamera = remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }

    fun toggleTorch() {
        currentCamera.value?.let { camera ->
            if (camera.cameraInfo.hasFlashUnit()) {
                try {
                    camera.cameraControl.enableTorch(!torchEnabled)
                    torchEnabled = !torchEnabled
                } catch (e: Exception) {
                    Log.e("Torch", "Error toggling torch: ${e.message}")
                }
            } else {
                Toast.makeText(context, "Flashlight not available on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle barcode detection navigation
    LaunchedEffect(detectedBarcode) {
        detectedBarcode?.let { barcode ->
            if (barcode.isNotEmpty()) {
                navController.navigate("details/$barcode")
                detectedBarcode = null // Prevent duplicate navigation
            }
        }
    }


    // Handle camera errors
    LaunchedEffect(cameraError) {
        cameraError?.let {
            Log.e("ScanScreen", "Camera error: $it")
            Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission Request Launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    // Check and request permission
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            hasCameraPermission = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }



    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        floatingActionButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FloatingActionButton(
                    onClick = { toggleTorch() },
                    containerColor = Color.White,
                    modifier = Modifier.size(45.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.torch), contentDescription = "Toggle Torch")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = Color.White,
                    modifier = Modifier.size(45.dp)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Enter Barcode")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = { navController.navigate("SearchScreen") },
                    containerColor = Color.White,
                    modifier = Modifier.size(45.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.keyboard), contentDescription = "Search by Product Name")
                }
            }
        }
    )  { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasCameraPermission) {
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalyzer.setAnalyzer(analysisExecutor) { imageProxy ->
                    processBarcodeImage(imageProxy, context) { barcode ->
                        coroutineScope.launch { detectedBarcode = barcode }
                    }
                }

                LaunchedEffect(cameraProviderFuture) {
                    val cameraProvider = try {
                        cameraProviderFuture.get()
                    } catch (e: Exception) {
                        cameraError = e.message
                        return@LaunchedEffect
                    }

                    val preview = Preview.Builder().build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()

                    try {
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                        currentCamera.value = camera

                    } catch (e: Exception) {
                        cameraError = e.message
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { previewView }
                    )

                    // Overlay should be drawn separately
                    ScanOverlay()
                }

            } else {
                Text(
                    text = "Camera permission is required!",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }


            // Manual Barcode Entry Dialog
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Enter Barcode") },
                    text = {
                        OutlinedTextField(
                            value = manualBarcode,
                            onValueChange = { manualBarcode = it },
                            label = { Text("Barcode") },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDialog = false
                                if (manualBarcode.isNotEmpty()) {
                                    detectedBarcode = manualBarcode
                                }
                            }
                        ) {
                            Text("Submit")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel", color = Color.Red)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ScanOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val boxSize = size.width * 0.8f
        val offsetX = (size.width - boxSize) / 2
        val offsetY = (size.height - boxSize) / 2
        val cornerSize = 40.dp.toPx() // Length of corner lines

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // Dark overlay
            val overlayPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                alpha = 120
                style = android.graphics.Paint.Style.FILL
            }
            nativeCanvas.drawRect(0f, 0f, size.width, size.height, overlayPaint)

            // Transparent scan box
            val clearPaint = android.graphics.Paint().apply {
                xfermode =
                    android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
            }
            nativeCanvas.drawRect(
                RectF(offsetX, offsetY, offsetX + boxSize, offsetY + boxSize),
                clearPaint
            )

            // Corner lines paint
            val cornerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 5.dp.toPx()
                isAntiAlias = true
            }

            fun drawCorner(x: Float, y: Float, isTop: Boolean, isLeft: Boolean) {
                val horizontalStartX = if (isLeft) x else x - cornerSize
                val horizontalEndX = if (isLeft) x + cornerSize else x
                val verticalStartY = if (isTop) y else y - cornerSize
                val verticalEndY = if (isTop) y + cornerSize else y

                nativeCanvas.drawLine(horizontalStartX, y, horizontalEndX, y, cornerPaint)
                nativeCanvas.drawLine(x, verticalStartY, x, verticalEndY, cornerPaint)
            }

            // Draw each corner
            drawCorner(offsetX, offsetY, isTop = true, isLeft = true) // Top-left
            drawCorner(offsetX + boxSize, offsetY, isTop = true, isLeft = false) // Top-right
            drawCorner(offsetX, offsetY + boxSize, isTop = false, isLeft = true) // Bottom-left
            drawCorner(
                offsetX + boxSize,
                offsetY + boxSize,
                isTop = false,
                isLeft = false
            ) // Bottom-right
        }
    }
}


@OptIn(ExperimentalGetImage::class)
private fun processBarcodeImage(
    imageProxy: ImageProxy,
    context: Context,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image ?: return
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val scanner = BarcodeScanning.getClient()

    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            if (barcodes.isNotEmpty()) {
                val barcodeValue = barcodes.first().rawValue
                if (barcodeValue != null) {
                    vibrateDevice(context) // Trigger vibration
                    onBarcodeDetected(barcodeValue)
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Barcode", "Error: ${e.message}")
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

// Function to trigger vibration
private fun vibrateDevice(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(100)
    }
}