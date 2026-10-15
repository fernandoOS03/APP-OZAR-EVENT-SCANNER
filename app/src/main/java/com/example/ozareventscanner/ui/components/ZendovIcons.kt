package com.example.ozareventscanner.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Íconos vectoriales oficiales 1:1 de Figma Make (Lucide Icons).
 * Creados con trazo outline de 1.8dp, bordes redondeados (round cap/join).
 */
object ZendovIcons {

    private fun buildIcon(name: String, pathData: String): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** Ícono de Inicio: Casa outline con puerta */
    val Home: ImageVector by lazy {
        buildIcon(
            name = "Home",
            pathData = "m3 11 9-8 9 8 M5 10v10h14V10 M9 20v-6h6v6"
        )
    }

    /** Ícono de Asistentes: 2 personas outline */
    val Users: ImageVector by lazy {
        buildIcon(
            name = "Users",
            pathData = "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75"
        )
    }

    /** Ícono de Overview: Gráfico de 4 barras verticales outline */
    val Chart: ImageVector by lazy {
        buildIcon(
            name = "Chart",
            pathData = "M4 19V9 M10 19V5 M16 19v-7 M22 19V3"
        )
    }

    /** Ícono de Perfil: Usuario outline con cabeza circular y hombros */
    val User: ImageVector by lazy {
        ImageVector.Builder(
            name = "User",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = PathParser().parsePathString("M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M4 21a8 8 0 0 1 16 0").toNodes(),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** Ícono de Visor Escáner Figma [ - ]: 4 esquinas con línea horizontal en el centro */
    val Scan: ImageVector by lazy {
        buildIcon(
            name = "Scan",
            pathData = "M3 7V5a2 2 0 0 1 2-2h2 M17 3h2a2 2 0 0 1 2 2v2 M21 17v2a2 2 0 0 1-2 2h-2 M7 21H5a2 2 0 0 1-2-2v-2 M7 12h10"
        )
    }

    /** Flecha Volver: ArrowLeft chevron */
    val ArrowLeft: ImageVector by lazy {
        buildIcon(
            name = "ArrowLeft",
            pathData = "m15 18-6-6 6-6"
        )
    }

    /** Flecha Continuar: ArrowRight chevron */
    val ArrowRight: ImageVector by lazy {
        buildIcon(
            name = "ArrowRight",
            pathData = "m9 18 6-6-6-6"
        )
    }

    /** Checkmark de validación */
    val Check: ImageVector by lazy {
        buildIcon(
            name = "Check",
            pathData = "m5 12 4 4L19 6"
        )
    }

    /** Ícono Cerrar Sesión: LogOut outline 1:1 Figma Make */
    val LogOut: ImageVector by lazy {
        buildIcon(
            name = "LogOut",
            pathData = "M10 17l5-5-5-5 M15 12H3 M14 3h5a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-5"
        )
    }
}
