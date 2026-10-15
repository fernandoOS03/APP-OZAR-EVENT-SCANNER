package com.example.ozareventscanner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ═════════════════════════════════════════════════════════════════════════════
// Genesis Design Tokens — Spacing & Border Radius Scale
// ═════════════════════════════════════════════════════════════════════════════

val RadiusXs   = 4.dp
val RadiusSm   = 8.dp       // 0.5rem — elementos pequeños
val RadiusMd   = 12.dp      // 0.75rem — inputs, botones compactos
val RadiusLg   = 16.dp      // 1rem — tarjetas secundarias
val RadiusXl   = 20.dp      // 1.25rem — tarjetas intermedias
val Radius2xl  = 24.dp      // 1.5rem — contenedores y tarjetas principales
val Radius3xl  = 28.dp      // 1.75rem — hojas modales (BottomSheets)
val Radius4xl  = 32.dp      // 2rem — bloques destacados
val RadiusPill = 9999.dp    // píldora — badges, botones de acción

// RoundedCornerShapes listos para Compose
val ShapeRadiusXs   = RoundedCornerShape(RadiusXs)
val ShapeRadiusSm   = RoundedCornerShape(RadiusSm)
val ShapeRadiusMd   = RoundedCornerShape(RadiusMd)
val ShapeRadiusLg   = RoundedCornerShape(RadiusLg)
val ShapeRadiusXl   = RoundedCornerShape(RadiusXl)
val ShapeRadius2xl  = RoundedCornerShape(Radius2xl)
val ShapeRadius3xl  = RoundedCornerShape(Radius3xl)
val ShapeRadius4xl  = RoundedCornerShape(Radius4xl)
val ShapeRadiusPill = RoundedCornerShape(RadiusPill)

// Configuración Material3 Shapes
val AppShapes = Shapes(
    extraSmall = ShapeRadiusXs,
    small      = ShapeRadiusSm,
    medium     = ShapeRadiusMd,
    large      = ShapeRadiusLg,
    extraLarge = ShapeRadius2xl
)
