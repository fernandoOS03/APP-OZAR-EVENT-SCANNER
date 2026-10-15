package com.example.ozareventscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.theme.GreenBase
import com.example.ozareventscanner.ui.theme.GreenLight1

/**
 * Isotipo oficial de Zendov Scanner: 3 cápsulas verdes inclinadas a 32 grados.
 */
@Composable
fun ZendovBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    val scale = size / 34.dp
    Box(
        modifier = modifier.size(size)
    ) {
        // Cápsula 1 (izquierda)
        Box(
            modifier = Modifier
                .offset(x = 3.dp * scale, y = 10.dp * scale)
                .size(width = 9.dp * scale, height = 19.dp * scale)
                .rotate(32f)
                .background(
                    color = GreenBase,
                    shape = RoundedCornerShape(
                        topStart = 4.5.dp * scale,
                        topEnd = 4.5.dp * scale,
                        bottomStart = 1.dp * scale,
                        bottomEnd = 1.dp * scale
                    )
                )
        )
        // Cápsula 2 (centro, la más alta)
        Box(
            modifier = Modifier
                .offset(x = 12.dp * scale, y = 3.dp * scale)
                .size(width = 9.dp * scale, height = 28.dp * scale)
                .rotate(32f)
                .background(
                    color = GreenBase,
                    shape = RoundedCornerShape(
                        topStart = 4.5.dp * scale,
                        topEnd = 4.5.dp * scale,
                        bottomStart = 1.dp * scale,
                        bottomEnd = 1.dp * scale
                    )
                )
        )
        // Cápsula 3 (derecha)
        Box(
            modifier = Modifier
                .offset(x = 21.dp * scale, y = 12.dp * scale)
                .size(width = 9.dp * scale, height = 16.dp * scale)
                .rotate(32f)
                .background(
                    color = GreenBase,
                    shape = RoundedCornerShape(
                        topStart = 4.5.dp * scale,
                        topEnd = 4.5.dp * scale,
                        bottomStart = 1.dp * scale,
                        bottomEnd = 1.dp * scale
                    )
                )
        )
    }
}

/**
 * Logotipo completo de Zendov: Marca gráfica + "zendov" + "scanner".
 */
@Composable
fun ZendovBrand(
    modifier: Modifier = Modifier,
    mostrarProduct: Boolean = true,
    compacto: Boolean = false,
    colorTexto: Color = Color.White
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZendovBrandMark(size = if (compacto) 26.dp else 34.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "zendov",
            fontSize = if (compacto) 20.sp else 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colorTexto,
            letterSpacing = (-0.06).sp
        )
        if (mostrarProduct) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SCANNER",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = GreenLight1,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
