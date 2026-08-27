package com.example.ozareventscanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ozareventscanner.ui.components.ModalBottomSheetResultadoScan
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.EventoViewModel
import com.example.ozareventscanner.viewmodel.ScanUiState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

// ─────────────────────────────────────────────────────────────────────────────
// Paleta del lector QR
// ─────────────────────────────────────────────────────────────────────────────
private val QrFondo    = Color(0xFF0F172A)
private val QrAccent   = ColorPrimary
private val QrVerde    = ColorSuccess
private val QrAmarillo = ColorWarning
private val QrRojo     = ColorError

/**
 * Pantalla de Escaneo QR con CameraX y ML Kit.
 *
 * Al detectar un código QR, consulta Room de forma offline-first e invoca
 * el [ModalBottomSheetResultadoScan] para mostrar el resultado y validar
 * la identidad del participante.
 */
@Composable
fun PantallaLectorQr(
    viewModel: EventoViewModel,
    onVolver: () -> Unit
) {
    val context        = LocalContext.current
    val scanState      by viewModel.scanState.collectAsState()
    val scanModalData  by viewModel.scanModalData.collectAsState()

    var tienePermiso by remember {
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
            // ── Cámara en Vivo ────────────────────────────────────────────────
            CameraPreviewWithValidation(
                modifier = Modifier.fillMaxSize(),
                estaEscaneandoActivo = scanState is ScanUiState.Idle && scanModalData == null,
                onQrDetectado = { codigo ->
                    viewModel.validarYMarcarParticipante(codigo)
                }
            )

            // ── Overlays oscurecedores fuera del área de escaneo ──────────────
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f)
                        .background(QrFondo.copy(alpha = 0.72f))
                )
                Box(modifier = Modifier.fillMaxWidth().weight(3f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2.5f)
                        .background(QrFondo.copy(alpha = 0.85f))
                )
            }

            // ── Marco Animado ─────────────────────────────────────────────────
            MarcoEscaneo(
                scanState = scanState,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
            )

            // ── Spinner Central durante procesamiento ─────────────────────────
            AnimatedVisibility(
                visible = scanState is ScanUiState.Procesando,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = QrAccent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

        } else {
            // ── Permiso Denegado ──────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permiso de cámara requerido",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Se requiere acceso a la cámara para escanear los códigos QR de los participantes.",
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = QrAccent)
                    ) {
                        Text("Conceder Permiso", color = Color.White)
                    }
                }
            }
        }

        // ── Botón Volver ──────────────────────────────────────────────────────
        IconButton(
            onClick = onVolver,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 48.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }

        // ── Etiqueta Guía ─────────────────────────────────────────────────────
        if (tienePermiso && scanState is ScanUiState.Idle && scanModalData == null) {
            Text(
                text = "Apunta al código QR del participante",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // ── ModalBottomSheet de Validación de Identidad y Resultado ──────────
        scanModalData?.let { data ->
            ModalBottomSheetResultadoScan(
                data = data,
                onDismiss = {
                    viewModel.cerrarModalScan()
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Marco de Escaneo Reactivo
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MarcoEscaneo(
    scanState: ScanUiState,
    modifier: Modifier = Modifier
) {
    val colorMarco = when (scanState) {
        is ScanUiState.Valido      -> QrVerde
        is ScanUiState.YaEscaneado,
        is ScanUiState.FormatoInvalido -> QrAmarillo
        is ScanUiState.NoEncontrado,
        is ScanUiState.ErrorScan   -> QrRojo
        else                       -> QrAccent
    }

    val infiniteTransition = rememberInfiniteTransition(label = "marco")
    val alphaLinea by infiniteTransition.animateFloat(
        initialValue  = 0.2f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "linea_scan"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, colorMarco.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
        )

        if (scanState is ScanUiState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(2.dp)
                    .alpha(alphaLinea)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, colorMarco, colorMarco, Color.Transparent)
                        )
                    )
            )
        }

        // Esquinas decorativas
        val cornerSize  = 24.dp
        val cornerWidth = 3.dp

        Box(modifier = Modifier.align(Alignment.TopStart)) {
            Box(modifier = Modifier.size(cornerSize, cornerWidth).background(colorMarco))
            Box(modifier = Modifier.size(cornerWidth, cornerSize).background(colorMarco))
        }
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            Box(modifier = Modifier.size(cornerSize, cornerWidth).align(Alignment.TopEnd).background(colorMarco))
            Box(modifier = Modifier.size(cornerWidth, cornerSize).align(Alignment.TopEnd).background(colorMarco))
        }
        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            Box(modifier = Modifier.size(cornerSize, cornerWidth).align(Alignment.BottomStart).background(colorMarco))
            Box(modifier = Modifier.size(cornerWidth, cornerSize).align(Alignment.BottomStart).background(colorMarco))
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            Box(modifier = Modifier.size(cornerSize, cornerWidth).align(Alignment.BottomEnd).background(colorMarco))
            Box(modifier = Modifier.size(cornerWidth, cornerSize).align(Alignment.BottomEnd).background(colorMarco))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraX Preview + ML Kit
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CameraPreviewWithValidation(
    modifier: Modifier = Modifier,
    estaEscaneandoActivo: Boolean,
    onQrDetectado: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val scanner        = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView          = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    if (!estaEscaneandoActivo) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes
                                    .firstOrNull { it.rawValue != null }
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
