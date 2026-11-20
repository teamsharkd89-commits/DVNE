// MochyPlayer.kt — Fase 7: Reproductor estilo Mochy en DVNE.
//
// La primera rebanada del reproductor es el mini reproductor: los
// botones de Mochy (MorphingCover.kt: MiniPlayerButton, MiniIconSwap,
// TouchFeedback.kt: pressBounce, RoundedHighlight). Adaptados, no
// copiados:
// - El highlight redondo es el de Mochy (esquinas 22.dp, se sale 4.dp,
//   aparece en 90 ms y al soltar parpadea y se apaga en 340 ms), pero
//   su color es el frostFill(1f) de DVNE — blanco 10% en oscuro —, el
//   mismo que el highlight deslizante de la barra (Fase 3).
// - El rebote al presionar es el de Mochy: se encoge a 0.88 con un
//   muelle rígido y vuelve con un poco de rebote.
// - El cambio play/pausa aparece desde pequeño y el viejo se encoge
//   (fade 160/120 ms, escala 220/160 ms desde 0.6).
// La página completa del reproductor (controles, slider, información
// de la canción) es la siguiente rebanada de esta misma fase.

package com.dvne.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * El cambio de play a pausa (y al revés) aparece desde pequeño
 * mientras el viejo se encoge: para usarlo como transitionSpec de
 * un AnimatedContent sobre el estado de reproducción.
 */
val MochyMiniIconSwap =
    (fadeIn(tween(160)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.6f)) togetherWith
        (fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.6f))

/**
 * Se encoge bajo el dedo y vuelve con un poco de rebote. Un toque
 * rápido junta el presionar y el soltar, así que al soltar primero
 * se muestra el encogido cuando nunca llegó a verse.
 */
@Composable
fun Modifier.mochyPressBounce(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.88f,
): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(interactionSource, pressedScale) {
        var pressed = 0
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressed++
                    launch { scale.animateTo(pressedScale, spring(dampingRatio = 1f, stiffness = 1600f)) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    pressed = (pressed - 1).coerceAtLeast(0)
                    if (pressed == 0) {
                        launch {
                            if (scale.value > pressedScale + 0.03f) scale.animateTo(pressedScale, tween(70))
                            scale.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 520f))
                        }
                    }
                }
            }
        }
    }
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/**
 * Una caja redondeada suave que aparece detrás de lo que se presiona,
 * en lugar del ripple cuadrado de Material. Se dibuja bajo el
 * contenido para que nada se recorte, y puede salirse de los bordes
 * para que un botón tenga aire alrededor.
 */
@Immutable
class MochyRoundedHighlight(
    private val cornerRadius: Dp,
    private val spillX: Dp = 0.dp,
    private val spillY: Dp = 0.dp,
    private val color: Color,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        MochyRoundedHighlightNode(interactionSource, cornerRadius, spillX, spillY, color)

    override fun equals(other: Any?): Boolean =
        other is MochyRoundedHighlight &&
            other.cornerRadius == cornerRadius &&
            other.spillX == spillX &&
            other.spillY == spillY &&
            other.color == color

    override fun hashCode(): Int {
        var result = cornerRadius.hashCode()
        result = 31 * result + spillX.hashCode()
        result = 31 * result + spillY.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }
}

private class MochyRoundedHighlightNode(
    private val interactionSource: InteractionSource,
    private val cornerRadius: Dp,
    private val spillX: Dp,
    private val spillY: Dp,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    private val strength = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            val presses = ArrayList<PressInteraction.Press>()
            var focused = false
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses.add(interaction)
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                    else -> return@collect
                }
                if (presses.isNotEmpty() || focused) {
                    launch { strength.animateTo(1f, tween(90)) }
                } else {
                    launch {
                        // Un toque rápido suelta antes de que la caja se haya visto: parpadea primero.
                        if (strength.value < 1f) strength.animateTo(1f, tween(70))
                        strength.animateTo(0f, tween(340))
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = strength.value
        if (s > 0f) {
            val dx = spillX.toPx()
            val dy = spillY.toPx()
            drawRoundRect(
                color = color.copy(alpha = color.alpha * s),
                topLeft = Offset(-dx, -dy),
                size = Size(size.width + dx * 2f, size.height + dy * 2f),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
            )
        }
        drawContent()
    }
}

/**
 * El highlight redondo detrás de un botón del mini reproductor:
 * el color del highlight de la barra, 4.dp más allá del botón de
 * 36.dp.
 */
@Composable
fun mochyMiniButtonHighlight(): MochyRoundedHighlight =
    MochyRoundedHighlight(
        cornerRadius = 22.dp,
        spillX = 4.dp,
        spillY = 4.dp,
        color = com.dvne.music.ui.components.frostFill(1f),
    )

/**
 * Uno de los botones del mini reproductor: un highlight redondo
 * aparece bajo el dedo y el botón se encoge con él.
 */
@Composable
fun MochyMiniButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier = modifier
            .mochyPressBounce(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = mochyMiniButtonHighlight(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}
