package com.sch.sekolah_mobile_app.ui.screens.jadwal

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.sch.sekolah_mobile_app.ui.theme.PrimaryTeal
import com.sch.sekolah_mobile_app.ui.theme.PrimaryTealContainer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalGetImage::class)
@Composable
actual fun PlatformCameraScanner(
    onQrDetected: (String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = (context as? LifecycleOwner) ?: androidx.compose.ui.platform.LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Auto-request permission on first composition if not already granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Debounce state to avoid multiple rapid scans of the same QR token
    var lastScannedTime by remember { mutableStateOf(0L) }
    var lastScannedCode by remember { mutableStateOf("") }
    val isScanningPaused = remember { AtomicBoolean(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    // Configure ML Kit specifically for QR Code
                    val mlKitOptions = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(mlKitOptions)

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setTargetResolution(Size(1280, 720))
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (isScanningPaused.get()) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                var detectedToken: String? = null

                                // Engine 1: Pure in-memory ZXing (Instant, offline, 0 download dependency)
                                try {
                                    detectedToken = decodeZxing(imageProxy)
                                } catch (_: Exception) {}

                                if (!detectedToken.isNullOrBlank()) {
                                    val now = System.currentTimeMillis()
                                    if (detectedToken != lastScannedCode || now - lastScannedTime > 2500L) {
                                        lastScannedCode = detectedToken
                                        lastScannedTime = now
                                        val tokenToSend = detectedToken
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            onQrDetected(tokenToSend)
                                        }
                                    }
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                // Engine 2: Google ML Kit Barcode Scanner
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue
                                                if (!rawValue.isNullOrBlank()) {
                                                    val now = System.currentTimeMillis()
                                                    if (rawValue != lastScannedCode || now - lastScannedTime > 2500L) {
                                                        lastScannedCode = rawValue
                                                        lastScannedTime = now
                                                        ContextCompat.getMainExecutor(ctx).execute {
                                                            onQrDetected(rawValue)
                                                        }
                                                    }
                                                    break
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )

                            // Tap-to-focus on preview view
                            previewView.setOnTouchListener { view, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    try {
                                        val factory = previewView.meteringPointFactory
                                        val point = factory.createPoint(event.x, event.y)
                                        val action = FocusMeteringAction.Builder(point).build()
                                        camera.cameraControl.startFocusAndMetering(action)
                                    } catch (_: Exception) {}
                                    view.performClick()
                                }
                                true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        } else {
            // Permission Request State UI
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryTealContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Izin Kamera Diperlukan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Aplikasi membutuhkan izin akses kamera perangkat untuk memindai kode QR presensi KBM dari Guru secara langsung.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Izinkan Akses Kamera",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private val zxingReader = MultiFormatReader().apply {
    val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )
    setHints(hints)
}

/**
 * High performance local QR code decoder using ZXing with Y-plane byte rotation.
 * 100% offline, requires no Google Play Services models.
 */
private fun decodeZxing(imageProxy: ImageProxy): String? {
    val plane = imageProxy.planes.getOrNull(0) ?: return null
    val buffer = plane.buffer
    val width = imageProxy.width
    val height = imageProxy.height
    val rowStride = plane.rowStride
    val rotation = imageProxy.imageInfo.rotationDegrees

    val yBytes = ByteArray(width * height)
    if (rowStride == width) {
        val bytesToRead = minOf(buffer.remaining(), width * height)
        buffer.get(yBytes, 0, bytesToRead)
    } else {
        val rowBuffer = ByteArray(rowStride)
        for (i in 0 until height) {
            val bytesToRead = minOf(rowStride, buffer.remaining())
            buffer.get(rowBuffer, 0, bytesToRead)
            System.arraycopy(rowBuffer, 0, yBytes, i * width, width)
        }
    }

    val rotatedBytes: ByteArray
    val finalWidth: Int
    val finalHeight: Int

    when (rotation) {
        90 -> {
            rotatedBytes = rotateY90(yBytes, width, height)
            finalWidth = height
            finalHeight = width
        }
        180 -> {
            rotatedBytes = rotateY180(yBytes, width, height)
            finalWidth = width
            finalHeight = height
        }
        270 -> {
            rotatedBytes = rotateY270(yBytes, width, height)
            finalWidth = height
            finalHeight = width
        }
        else -> {
            rotatedBytes = yBytes
            finalWidth = width
            finalHeight = height
        }
    }

    val source = PlanarYUVLuminanceSource(
        rotatedBytes,
        finalWidth,
        finalHeight,
        0, 0,
        finalWidth,
        finalHeight,
        false
    )

    // Pass 1: HybridBinarizer (Standard fast binarizer)
    try {
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val result = zxingReader.decodeWithState(bitmap)
        if (!result.text.isNullOrBlank()) {
            return result.text
        }
    } catch (_: Exception) {
    } finally {
        zxingReader.reset()
    }

    // Pass 2: GlobalHistogramBinarizer (Excellent for phone screen glare / low contrast)
    try {
        val bitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
        val result = zxingReader.decodeWithState(bitmap)
        if (!result.text.isNullOrBlank()) {
            return result.text
        }
    } catch (_: Exception) {
    } finally {
        zxingReader.reset()
    }

    return null
}

private fun rotateY90(src: ByteArray, width: Int, height: Int): ByteArray {
    val dest = ByteArray(width * height)
    var i = 0
    for (x in 0 until width) {
        for (y in height - 1 downTo 0) {
            dest[i++] = src[y * width + x]
        }
    }
    return dest
}

private fun rotateY180(src: ByteArray, width: Int, height: Int): ByteArray {
    val dest = ByteArray(width * height)
    val len = width * height
    for (i in 0 until len) {
        dest[len - 1 - i] = src[i]
    }
    return dest
}

private fun rotateY270(src: ByteArray, width: Int, height: Int): ByteArray {
    val dest = ByteArray(width * height)
    var i = 0
    for (x in width - 1 downTo 0) {
        for (y in 0 until height) {
            dest[i++] = src[y * width + x]
        }
    }
    return dest
}
