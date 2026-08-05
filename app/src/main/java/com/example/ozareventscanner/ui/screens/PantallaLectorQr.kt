package com.example.ozareventscanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.AppViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

// Color oscuro específico para overlays del escáner (debe ser oscuro para contrastar con la cámara)
private val QrFondo      = Color(0xFF0F172A)
private val QrAccent     = ColorPrimary
private val QrVerde      = ColorAccent
private val QrSecundario = ColorTextSecondary

/**
 * Pantalla 4 — Lector QR con CameraX + ML Kit.
 * Pide permiso de cámara en tiempo de ejecución.
 * Muestra el resultado del último QR escaneado.
 */
@Composable
fun PantallaLectorQr(
    viewModel: AppViewModel,
    onVolver: () -> Unit
) {
    val context        = LocalContext.current
    val resultadoQr   by viewModel.resultadoQr.collectAsState()
    var tienePermiso   by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> tienePermiso = concedido }

    LaunchedEffect(Unit) {
        if (!tienePermiso) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QrFondo)
    ) {
        if (tienePermiso) {
            // ── Vista de cámara ───────────────────────────────────────────────
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onQrDetectado = { codigo ->
                    viewModel.registrarResultadoQr(codigo)
                }
            )

            // ── Overlay oscuro superior e inferior ────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {
                // Zona superior opaca
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f)
                        .background(QrFondo.copy(alpha = 0.7f))
                )
                // Zona del marco (transparente)
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f))
                // Zona inferior opaca
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2.5f)
                        .background(QrFondo.copy(alpha = 0.85f))
                )
            }

            // ── Marco de escaneo animado ──────────────────────────────────────
            MarcoEscaneo(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
            )

        } else {
            // ── Sin permiso ───────────────────────────────────────────────────
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.padding(32.dp)
                ) {
                    Text(
                        text       = "📷",
                        fontSize   = 56.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text       = "Permiso de cámara requerido",
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        fontSize   = 18.sp
                    )
                    Text(
                        text     = "Necesitamos acceso a la cámara para escanear códigos QR",
                        color    = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        colors  = ButtonDefaults.buttonColors(containerColor = QrAccent)
                    ) {
                        Text("Conceder Permiso", color = Color.White)
                    }
                }
            }
        }

        // ── Botón volver (siempre visible) ────────────────────────────────────
        IconButton(
            onClick  = onVolver,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 44.dp)
                .background(QrFondo.copy(alpha = 0.6f), RoundedCornerShape(50))
        ) {
            Icon(
                imageVector        = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint               = Color.White
            )
        }

        // ── Texto guía ────────────────────────────────────────────────────────
        if (tienePermiso) {
            Text(
                text     = "Apunta al código QR del participante",
                color    = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp)
                    .background(QrFondo.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // ── Panel de resultado QR ─────────────────────────────────────────────
        if (resultadoQr != null) {
            ResultadoQrPanel(
                codigo    = resultadoQr ?: "",
                onDismiss = { viewModel.limpiarResultadoQr() },
                modifier  = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Marco de escaneo animado
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MarcoEscaneo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "marco")
    val alphaLinea by infiniteTransition.animateFloat(
        initialValue   = 0.2f,
        targetValue    = 1f,
        animationSpec  = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label          = "linea_scan"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Marco con esquinas redondeadas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, QrAccent.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
        )

        // Línea de escaneo horizontal
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(2.dp)
                .alpha(alphaLinea)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, QrAccent, QrAccent, Color.Transparent)
                    )
                )
        )

        // Esquinas decorativas
        val cornerSize = 24.dp
        val cornerWidth = 3.dp

        // Esquina superior-izquierda
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            Box(modifier = Modifier.size(cornerSize, cornerWidth).background(QrAccent))
            Box(modifier = Modifier.size(cornerWidth, cornerSize).background(QrAccent))
        }
        // Esquina superior-derecha
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            Box(modifier = Modifier.size(cornerSize, cornerWidth).align(Alignment.TopEnd).background(QrAccent))
            Box(modifier = Modifier.size(cornerWidth, cornerSize).align(Alignment.TopEnd).background(QrAccent))
        }
        // Esquina inferior-izquierda
        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            Box(modifier = Modifier.size(cornerSize, cornerWidth).align(Alignment.BottomStart).background(QrAccent))
            Box(modifier = Modifier.size(cornerWidth, cornerSize).align(Alignment.BottomStart).background(QrAccent))
        }
        // Esquina inferior-derecha
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            Box(modifier = Modifier.size(cornerSize, cornerWidth).align(Alignment.BottomEnd).background(QrAccent))
            Box(modifier = Modifier.size(cornerWidth, cornerSize).align(Alignment.BottomEnd).background(QrAccent))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel resultado QR (Estilo Claro/Superficie Blanca)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ResultadoQrPanel(codigo: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = ColorSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.CheckCircle,
                contentDescription = null,
                tint               = QrVerde,
                modifier           = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Código detectado",
                    fontWeight = FontWeight.Bold,
                    color      = ColorTextPrimary,
                    fontSize   = 14.sp
                )
                Text(
                    text     = codigo,
                    color    = QrSecundario,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = onDismiss) {
                Text("OK", color = QrAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraX Preview + Analizador ML Kit
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onQrDetectado: (String) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val scanner        = remember { BarcodeScanning.getClient() }

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    @androidx.camera.core.ExperimentalGetImage
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull { it.valueType == Barcode.TYPE_TEXT || it.rawValue != null }
                                    ?.rawValue
                                    ?.let { onQrDetectado(it) }
                             }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
