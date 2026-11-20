// MochyHome.kt — Fase 4: Inicio estilo Mochy en DVNE.
//
// Dos piezas del Inicio de Mochy, adaptadas:
// - El glow de arriba (HomeTopGlow.kt): dos brillos de esquina con los
//   colores de las portadas, que se apagan al bajar. Mochy lo dibuja con
//   android.graphics RadialGradient y un BlurMaskFilter; aquí se dibuja
//   con Brush.radialGradient de Compose, que es lo mismo sin el shader
//   manual. Los tonos salen de MochyGlowColor (Fase 2, Palette + Coil3).
// - El título de sección de Mochy: 16sp Bold, blanco al 69%, chevron a
//   la derecha cuando hay más. Mochy no tiene strapline bajo el título,
//   así que en modo Mochy no se muestra.
// Compromisos, no olvidos:
// - El glow es estático por ahora: el modo animado de Mochy (cicla hasta
//   4 tonos cada 4 s) queda para el pulido de Fase 13.
// - El título usa el padding efectivo de SectionHeader (20.dp a la
//   izquierda) para alinearse con las filas de DVNE, no el contentInset
//   de 36.dp de Mochy: las filas de DVNE empiezan cerca del borde.
// - El glow y el título solo se usan con la plantilla Mochy (Fase 4).
//   El resto de plantillas no cambia nada. El saludo, los atajos y las
//   tarjetas de cada sección siguen siendo los de DVNE.

package com.dvne.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dvne.music.ui.theme.DvneIcons
import com.dvne.music.ui.theme.MochyDesign
import com.dvne.music.ui.theme.MochyGlowColor
import com.dvne.music.ui.theme.MochyText

/**
 * El glow de Mochy detrás del saludo: los dos tonos de la primera
 * portada, uno en cada esquina superior, desvaneciéndose hacia abajo.
 * [alpha] lo apaga al bajar (en Home es 1f - edge).
 */
@Composable
fun MochyHomeGlow(urls: List<String>, alpha: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var tones by remember { mutableStateOf<Pair<Color, Color>?>(null) }
    LaunchedEffect(urls) {
        tones = null
        // Sin portadas no hay tonos que sacar: el glow fijo de Mochy.
        val url = urls.firstOrNull()
        if (url == null) {
            tones = MochyGlowFixedColor.BLUE.color to MochyGlowFixedColor.VIOLET.color
            return@LaunchedEffect
        }
        tones = MochyGlowColor.loadPair(context, url, 0.5f)
            ?: MochyGlowColor.load(context, url, 0.5f)?.let { it to it }
            // Armazón Mochy: si la portada no da tonos, el glow no
            // desaparece — cae a los dos fijos de Mochy (azul/violeta).
            ?: (MochyGlowFixedColor.BLUE.color to MochyGlowFixedColor.VIOLET.color)
    }
    val pair = tones
    Box(
        modifier.graphicsLayer { this.alpha = alpha }.drawBehind {
            val (left, right) = pair ?: return@drawBehind
            val radius = size.height * 1.1f
            drawRect(
                Brush.radialGradient(
                    listOf(left.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.08f, 0f), radius = radius,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(right.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.92f, 0f), radius = radius,
                ),
            )
        },
    )
}

/** El título de sección de Mochy, con chevron cuando [onMore] no es nulo. */
@Composable
fun MochySectionHeader(title: String, onMore: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 8.dp)
            .then(if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title, Modifier.weight(1f), style = MochyText.sectionLabel(),
            color = MochyDesign.sectionLabelColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (onMore != null) Icon(DvneIcons.ChevronRight, null, Modifier.size(18.dp), tint = MochyDesign.sectionChevronColor)
    }
}
