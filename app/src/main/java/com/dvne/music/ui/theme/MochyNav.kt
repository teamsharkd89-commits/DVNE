// MochyNav.kt — Fase 3: navegación estilo Mochy en DVNE.
//
// El highlight deslizante de Mochy (AppNavigation.kt): UNA píldora que se
// desliza detrás del tab activo, en vez de una cápsula que aparece y
// desaparece detrás de cada tab. Adaptado, no copiado:
// - Mochy tiene 4 tabs fijos con iconos PNG y sin etiquetas. DVNE tiene
//   tabs configurables (NavTab: Home, Search, Library, Replay, Settings,
//   Local, Downloads, Liked, Podcasts), etiquetas (NavLabels) y su propio
//   selectTab con stacks por tab. Todo eso se conserva; solo cambia el
//   highlight. Por eso esto es un Modifier, no una barra nueva.
// - El color del highlight lo pasa quien llama (MainActivity lo saca de
//   frostFill(1f): blanco al 10% en oscuro, igual que ChromeHighlightColor
//   de Mochy). Así respeta el glass de DVNE en vez de hardcodear blanco.
// - La barra en sí (FLOATING/DOCKED/MINIMAL, tamaños, glass) no se toca.
//   El highlight solo se usa en FLOATING, la píldora que más se parece
//   al estadio de Mochy. DOCKED y MINIMAL quedan como estaban.
// - NavRail (pantallas anchas) tampoco se toca: Mochy no tiene rail.
// - Doble toque en Buscar (Mochy abre el campo de texto) queda para
//   Fase 6 (Búsqueda), donde se puede cablear el foco; aquí el toque en
//   el tab activo sigue haciendo lo de DVNE: volver arriba de ese tab.

package com.dvne.music.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Alto de la barra de Mochy (64.dp = STANDARD en DVNE). */
val MochyNavBarHeight: Dp = 64.dp

/** El highlight se mete esto desde arriba, abajo y ambos extremos. */
private val MochyNavHighlightInset = 8.dp

/** Hueco entre el highlight y donde estaría el del vecino. */
private val MochyNavHighlightGap = 4.dp

/**
 * El borde delantero del highlight salta al tab nuevo más rápido que el
 * trasero, así se estira hacia el tab y se asienta en él en vez de
 * deslizarse como un bloque rígido (AppNavigation.kt).
 */
private val MochyNavHighlightLead = spring<Float>(dampingRatio = 0.82f, stiffness = 700f)
private val MochyNavHighlightTrail = spring<Float>(dampingRatio = 0.82f, stiffness = 320f)

/**
 * Un highlight detrás del tab activo que se desliza al siguiente, en vez
 * de que cada tab encienda y apague el suyo. Solo se lee al dibujar, así
 * el deslizamiento nunca recompone la barra.
 *
 * [selectedIndex] -1 (página profunda que no es ningún tab) apaga el
 * highlight con un fundido en vez de deslizarlo desde el tab anterior.
 * [color] es el color del highlight, ya resuelto por quien llama.
 */
@Composable
fun Modifier.mochyNavHighlight(selectedIndex: Int, tabCount: Int, color: Color): Modifier {
    val start = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val end = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val shown = remember { Animatable(if (selectedIndex >= 0) 1f else 0f) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) {
            shown.animateTo(0f, tween(200))
            return@LaunchedEffect
        }
        val target = selectedIndex.toFloat()
        if (shown.value == 0f) {
            start.snapTo(target)
            end.snapTo(target)
        }
        launch { shown.animateTo(1f, tween(200)) }
        val movingRight = target > (start.value + end.value) / 2f
        launch { start.animateTo(target, if (movingRight) MochyNavHighlightTrail else MochyNavHighlightLead) }
        end.animateTo(target, if (movingRight) MochyNavHighlightLead else MochyNavHighlightTrail)
    }

    return drawBehind {
        drawMochyNavHighlight(this, start.value, end.value, shown.value, tabCount, color)
    }
}

private fun drawMochyNavHighlight(
    scope: DrawScope,
    start: Float,
    end: Float,
    alpha: Float,
    tabCount: Int,
    color: Color,
) {
    if (alpha <= 0f || tabCount == 0) return
    with(scope) {
        val slot = size.width / tabCount
        val halfGap = MochyNavHighlightGap.toPx() / 2f
        val inset = MochyNavHighlightInset.toPx()
        val left = start * slot + halfGap
        val right = (end + 1f) * slot - halfGap
        val height = size.height - inset * 2f
        if (height <= 0f || right <= left) return
        drawRoundRect(
            color = color,
            topLeft = Offset(left, inset),
            size = Size(right - left, height),
            cornerRadius = CornerRadius(height / 2f),
            alpha = alpha,
        )
    }
}
