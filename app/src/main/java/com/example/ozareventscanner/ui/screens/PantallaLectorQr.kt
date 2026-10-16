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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ozareventscanner.ui.components.ModalBottomSheetResultadoScan
import com.example.ozareventscanner.ui.components.ZendovIcons
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.EventoViewModel
import com.example.ozareventscanner.viewmodel.ScanUiState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Pantalla de Escaneo Inmersivo — Zendov Scanner.
 * Implementación 1:1 de Figma Make con retícula animada, esquinas acentuadas,
 * indicador LIVE y validación en tiempo real.
 */
@Composable
fun PantallaLectorQr(
    viewModel: EventoViewModel,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsState()
    val scanModalData by viewModel.scanModalData.collectAsState()

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
            .background(GrayDark2)
    ) {
        if (tienePermiso) {
            // ── 1. Visor de Cámara CameraX ────────────────────────────────────
            CameraPreviewWithValidation(
                modifier = Modifier.fillMaxSize(),
                estaEscaneandoActivo = scanState is ScanUiState.Idle && scanModalData == null,
                onQrDetectado = { codigo ->
                    viewModel.validarYMarcarParticipante(codigo)
                }
            )

            // ── 2. Mascara oscurecedora para enfoque central ──────────────────
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.3f)
                        .background(Color.Black.copy(alpha = 0.65f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2.4f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.8f)
                        .background(Color.Black.copy(alpha = 0.75f))
                )
            }

            // ── 3. Retícula de Escaneo Zendov (Figma focus-zone) ──────────────
            ZendovFocusZone(
                scanState = scanState,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)
                    .size(260.dp)
            )

            // ── 4. Indicador central mientras procesa ─────────────────────────
            AnimatedVisibility(
                visible = scanState is ScanUiState.Procesando,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = GreenBase,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Validando acreditación...",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

        } else {
            // ── Permiso de Cámara Requerido ───────────────────────────────────
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
                        tint = GreenLight1.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Permiso de cámara requerido",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Para escanear códigos QR de acreditación y entrega de beneficios necesitas habilitar el acceso a la cámara.",
                        color = GrayBase,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDark2)
                    ) {
                        Text("Permitir acceso", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── 5. Barra Superior Figma (1:1 Figma Make) ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Volver Ghost
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onVolver() }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = ZendovIcons.ArrowLeft,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Volver",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Indicador LIVE Figma (.camera-live)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(GreenBase, CircleShape)
                )
                Text(
                    text = "LIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenLight1,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // ── 6. Scanner Hint al pie de pantalla (1:1 Figma Make) ───────────────
        if (tienePermiso && scanState is ScanUiState.Idle && scanModalData == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 44.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Apunta la cámara hacia el código del participante",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                // Botón interactivo para simular lectura (como en Figma Make)
                Surface(
                    onClick = {
                        viewModel.validarYMarcarParticipante("PART-1001")
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, GreenDark1.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "TOCA PARA SIMULAR LECTURA",
                        color = GreenLight1,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ── 7. Modal BottomSheet de Resultado ─────────────────────────────────
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
// Retícula de Escaneo Inmersiva Zendov — 1:1 Figma Make
// ─────────────────────────────────────────────────────────────────────────────
private data class SparkPoint(val xRatio: Float, val yRatio: Float, val delayFraction: Float)

private val sparksList = listOf(
    SparkPoint(0.15f, 0.25f, 0.0f),
    SparkPoint(0.28f, 0.14f, 0.13f),
    SparkPoint(0.81f, 0.21f, 0.46f),
    SparkPoint(0.91f, 0.46f, 0.0f),
    SparkPoint(0.76f, 0.82f, 0.26f),
    SparkPoint(0.35f, 0.90f, 0.0f),
    SparkPoint(0.08f, 0.69f, 0.60f),
    SparkPoint(0.44f, 0.27f, 0.20f),
    SparkPoint(0.60f, 0.78f, 0.0f),
    SparkPoint(0.69f, 0.09f, 0.66f),
    SparkPoint(0.21f, 0.55f, 0.0f),
    SparkPoint(0.92f, 0.91f, 0.33f),
    SparkPoint(0.05f, 0.08f, 0.0f),
    SparkPoint(0.96f, 0.29f, 0.46f)
)

@Composable
private fun ZendovFocusZone(
    scanState: ScanUiState,
    modifier: Modifier = Modifier
) {
    val colorAcento = when (scanState) {
        is ScanUiState.Valido -> StatusSuccess
        is ScanUiState.YaEscaneado,
        is ScanUiState.FormatoInvalido -> StatusWarning
        is ScanUiState.NoEncontrado,
        is ScanUiState.ErrorScan -> StatusDanger
        else -> GreenLight1
    }

    // Animación continua del láser de escaneo
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    // Animación de centelleo para las 14 chispas atmosféricas (.spark twinkle)
    val twinkleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle_progress"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 4 Esquinas con curvatura suave de 15px según Figma + 14 Chispas atmosféricas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerLen = 42.dp.toPx()
            val radius = 15.dp.toPx()
            val strokeWidth = 2.dp.toPx()
            val w = size.width
            val h = size.height

            // Top-Left corner
            val pathTL = Path().apply {
                moveTo(0f, cornerLen)
                lineTo(0f, radius)
                quadraticTo(0f, 0f, radius, 0f)
                lineTo(cornerLen, 0f)
            }
            drawPath(pathTL, color = colorAcento, style = Stroke(width = strokeWidth))

            // Top-Right corner
            val pathTR = Path().apply {
                moveTo(w - cornerLen, 0f)
                lineTo(w - radius, 0f)
                quadraticTo(w, 0f, w, radius)
                lineTo(w, cornerLen)
            }
            drawPath(pathTR, color = colorAcento, style = Stroke(width = strokeWidth))

            // Bottom-Left corner
            val pathBL = Path().apply {
                moveTo(0f, h - cornerLen)
                lineTo(0f, h - radius)
                quadraticTo(0f, h, radius, h)
                lineTo(cornerLen, h)
            }
            drawPath(pathBL, color = colorAcento, style = Stroke(width = strokeWidth))

            // Bottom-Right corner
            val pathBR = Path().apply {
                moveTo(w - cornerLen, h)
                lineTo(w - radius, h)
                quadraticTo(w, h, w, h - radius)
                lineTo(w, h - cornerLen)
            }
            drawPath(pathBR, color = colorAcento, style = Stroke(width = strokeWidth))

            // 14 Chispas atmosféricas / Partículas flotantes (1:1 Figma Make .spark)
            sparksList.forEach { spark ->
                val sparkPhase = (twinkleProgress + spark.delayFraction) % 1f
                val alpha = (0.25f + 0.75f * kotlin.math.sin(sparkPhase * Math.PI.toFloat())).coerceIn(0.15f, 1f)
                val cx = w * spark.xRatio
                val cy = h * spark.yRatio
                // Resplandor exterior
                drawCircle(
                    color = GreenBase.copy(alpha = alpha * 0.4f),
                    radius = 4.5.dp.toPx(),
                    center = Offset(cx, cy)
                )
                // Punto de luz central
                drawCircle(
                    color = GreenLight1.copy(alpha = alpha),
                    radius = 1.8.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        // Línea Láser Animada con Gradiente Verde Neón (.scan-line)
        if (scanState is ScanUiState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (220.dp * laserProgress))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    colorAcento.copy(alpha = 0.5f),
                                    colorAcento,
                                    colorAcento.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Copy central de estado (.scan-copy 1:1 Figma Make)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = ZendovIcons.Scan,
                contentDescription = null,
                tint = GreenLight1,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Buscando código",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mantén el dispositivo estable",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraX Preview + ML Kit Analyzer
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CameraPreviewWithValidation(
    modifier: Modifier = Modifier,
    estaEscaneandoActivo: Boolean,
    onQrDetectado: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    // Control atómico de concurrencia y cooldown para la cámara
    val isProcessingQr = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    var ultimoTimestampEscaneo by remember { mutableLongStateOf(0L) }

    LaunchedEffect(estaEscaneandoActivo) {
        if (estaEscaneandoActivo) {
            isProcessingQr.set(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
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
                    val now = System.currentTimeMillis()
                    val cooldownActivo = (now - ultimoTimestampEscaneo) < 1500L

                    if (!estaEscaneandoActivo || isProcessingQr.get() || cooldownActivo) {
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
                                val barcode = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                if (barcode != null && isProcessingQr.compareAndSet(false, true)) {
                                    ultimoTimestampEscaneo = System.currentTimeMillis()
                                    onQrDetectado(barcode)
                                }
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
