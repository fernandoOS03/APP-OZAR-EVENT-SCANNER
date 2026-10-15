package com.example.ozareventscanner.ui.theme

import androidx.compose.ui.graphics.Color

// ═════════════════════════════════════════════════════════════════════════════
// Genesis Design Tokens (LeafyGreen / MongoDB Inspired)
// Main: #001E2B · Accent: #00684A · Vibrant: #00ED64 · Soft: #F9FBFA
// ═════════════════════════════════════════════════════════════════════════════

// ── 1. Green Scale ────────────────────────────────────────────────────────────
val GreenDark3  = Color(0xFF023430)
val GreenDark2  = Color(0xFF00684A)
val GreenDark1  = Color(0xFF00A35C)
val GreenBase   = Color(0xFF00ED64)
val GreenLight1 = Color(0xFF71F6BA)
val GreenLight2 = Color(0xFFC0FAE6)
val GreenLight3 = Color(0xFFE3FCF7)

// ── 2. Gray Scale ─────────────────────────────────────────────────────────────
val GrayDark4   = Color(0xFF000F17)
val GrayDark3   = Color(0xFF00141D)
val GrayDark2   = Color(0xFF001E2B)
val GrayDark1   = Color(0xFF0A2B3A)
val GrayBase    = Color(0xFF889397)
val GrayLight1  = Color(0xFFC1C7C6)
val GrayLight2  = Color(0xFFE8EDEB)
val GrayLight3  = Color(0xFFF9FBFA)

// ── 3. Semantic Aliases ───────────────────────────────────────────────────────
val ColorBackground         = GrayLight3           // #F9FBFA
val ColorForeground         = Color(0xFF003826)    // Zendov Dark Green Foreground
val ColorForegroundMuted    = Color(0xFF55746A)
val ColorCard               = Color(0xFFFFFFFF)
val ColorCardForeground     = Color(0xFF003826)
val ColorSurface            = ColorCard
val ColorSurfaceHigh        = GrayLight2           // #E8EDEB
val ZendovDarkBg            = GrayDark2            // #001E2B
val ZendovOrbitBorder       = Color(0x1A71F6BA)    // rgba(113, 246, 186, 0.10)
val ZendovRadialGlow        = Color(0x2E00ED64)    // rgba(0, 237, 100, 0.18)

// Primary (Vibrante) y PrimaryDark (para botones con alto contraste de texto)
val ColorPrimary            = GreenDark2           // #00684A para contraste perfecto en botones
val ColorPrimaryVibrant     = GreenBase            // #00ED64 verde vibrante para acentos y badges
val ColorPrimaryForeground  = Color(0xFFFFFFFF)
val ColorSecondary          = GreenLight3          // #E3FCF7
val ColorSecondaryForeground = Color(0xFF001E2B)

val ColorMuted              = GrayLight2           // #E8EDEB
val ColorMutedForeground    = GrayDark1            // #5C6C75
val ColorAccent             = GreenDark2           // #00684A
val ColorAccentForeground   = Color(0xFFFFFFFF)
val ColorDestructive        = Color(0xFFCF4A22)
val ColorBorder             = GrayLight2           // #E8EDEB
val ColorInput              = Color(0xFFFFFFFF)
val ColorRing               = GreenBase            // #00ED64

// ── 4. Typography / Text Colors ───────────────────────────────────────────────
val ColorTextPrimary        = Color(0xFF001E2B)    // #001E2B
val ColorTextSecondary      = GrayDark1            // #5C6C75
val ColorTextMuted          = GrayBase             // #889397
val ColorTextDisabled       = GrayLight1           // #C1C7C6

// ── 5. Status / Feedback Palette ──────────────────────────────────────────────
val StatusSuccess           = Color(0xFF10B981)
val StatusSuccessSoft       = Color(0x1410B981)    // rgba(16, 185, 129, 0.08)
val StatusSuccessText       = Color(0xFF065F46)

val StatusWarning           = Color(0xFFF59E0B)
val StatusWarningSoft       = Color(0x14F59E0B)    // rgba(245, 158, 11, 0.08)
val StatusWarningText       = Color(0xFF92400E)

val StatusDanger            = Color(0xFFEF4444)
val StatusDangerSoft        = Color(0x14EF4444)    // rgba(239, 68, 68, 0.08)
val StatusDangerText        = Color(0xFF7F1D1D)

val StatusInfo              = Color(0xFF009DE1)
val StatusInfoSoft          = Color(0x14009DE1)    // rgba(0, 157, 225, 0.08)
val StatusInfoText          = Color(0xFF1E3A8A)

// Compatibilidad hacia atrás
val ColorSuccess            = StatusSuccess
val ColorWarning            = StatusWarning
val ColorError              = StatusDanger
val ColorInfo               = StatusInfo

val ColorSuccessBg          = GreenLight3          // #E3FCF7
val ColorWarningBg          = Color(0xFFFFFBEB)
val ColorErrorBg            = Color(0xFFFEF2F2)
val ColorInfoBg             = Color(0xFFF0FDF4)
val ColorDark               = Color(0xFF001E2B)

// Gradiente de fondo suave
val GradientFondo           = listOf(ColorBackground, GrayLight2)