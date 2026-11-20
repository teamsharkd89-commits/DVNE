// MochyLibrary.kt — Fase 5: Biblioteca estilo Mochy en DVNE.
//
// Las tarjetas de categoría de Mochy (LibraryDesign.kt: LibraryCategoryCard,
// LibraryWideCard, LibraryCardGrid, LibraryCoverStack): media tarjeta de
// 78.dp con el nombre arriba, el conteo grande debajo y dos portadas
// reales abanicadas en la esquina inferior derecha; tarjetas anchas de
// 104.dp para "Añadidas recientemente" y "Me gusta". Adaptadas, no copiadas:
// - Mochy es dark-only con Tile #121214, Primary blanco y Secondary #9A9AA0.
//   Aquí el fondo es surfaceContainerHigh y los textos onSurface /
//   onSurfaceVariant del tema de DVNE, así la tarjeta se ve bien también
//   en modo claro. La forma, las medidas, los ángulos y la tipografía
//   son los de Mochy.
// - Las portadas las carga Artwork de DVNE (Coil), no el LibraryGridArtwork
//   de Mochy. Sin containerTransform: la tarjeta solo navega; las
//   transiciones las pone DVNE.
// - El padding lateral es el de la rejilla de Biblioteca de DVNE (12.dp),
//   no los 24.dp de Mochy, para alinearse con el resto de la pantalla.
// - La vista general (las tarjetas) vive como primer tab de Biblioteca,
//   "Resumen"; los tabs y los datos de DVNE no se tocan.

package com.dvne.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dvne.music.ui.components.Artwork
import com.dvne.music.ui.theme.AppFonts

/** La etiqueta de la tarjeta: un paso por encima de su conteo. */
private val MochyLibraryLabelStyle = TextStyle(
    fontFamily = AppFonts.dvne, fontWeight = FontWeight.Bold,
    fontSize = 15.sp, lineHeight = 18.sp, letterSpacing = (-0.02).em,
)

/** El conteo grande y gris de cada tarjeta de Biblioteca. */
private val MochyLibraryCountStyle = TextStyle(
    fontFamily = AppFonts.dvne, fontWeight = FontWeight.Bold,
    fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.03).em,
)

/**
 * Dos portadas reales, abanicadas y metidas en la esquina inferior
 * derecha de una tarjeta. La de atrás va atenuada y girada al revés para
 * que el par se lea como pila. No dibuja nada sin portadas.
 */
@Composable
private fun MochyLibraryCoverStack(
    covers: List<String?>,
    tile: Dp = 46.dp,
    round: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shown = remember(covers) { covers.filterNotNull().take(2) }
    if (shown.isEmpty()) return
    val back = shown.getOrNull(1)
    val corner = if (round) tile / 2 else tile * 0.22f
    val shape = if (round) CircleShape else RoundedCornerShape(corner)
    Box(modifier.size(width = tile * 1.55f, height = tile)) {
        if (back != null) {
            Box(
                Modifier.align(Alignment.CenterStart).size(tile)
                    .graphicsLayer { alpha = 0.55f; rotationZ = if (round) 0f else -9f },
            ) {
                Artwork(back, Modifier.fillMaxSize(), shape, size = 226)
            }
        }
        Box(
            Modifier.align(Alignment.CenterEnd).size(tile)
                .graphicsLayer { rotationZ = if (round) 0f else 7f },
        ) {
            Artwork(shown[0], Modifier.fillMaxSize(), shape, size = 226)
        }
    }
}

data class MochyLibraryCardEntry(
    val title: String,
    val count: Int,
    val covers: List<String?>,
    val onClick: () -> Unit,
    /** Artistas: sus portadas son círculos. */
    val roundCovers: Boolean = false,
)

/** Una de las tarjetas de media anchura: Canciones, Listas, Álbumes, Artistas. */
@Composable
fun MochyLibraryCategoryCard(
    title: String,
    count: Int,
    covers: List<String?>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    roundCovers: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier.height(78.dp).clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerHigh).clickable(onClick = onClick),
    ) {
        // Las portadas se salen por la esquina y el clip las recorta:
        // eso da el aire "metido" en vez de una miniatura flotando.
        MochyLibraryCoverStack(
            covers = covers, round = roundCovers,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 12.dp),
        )
        Column(Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 11.dp, end = 12.dp)) {
            Text(
                title, color = scheme.onSurface, style = MochyLibraryLabelStyle,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(count.toString(), color = scheme.onSurfaceVariant, style = MochyLibraryCountStyle, maxLines = 1)
        }
    }
}

/** Tarjetas de dos en dos; la última guarda su media anchura si va sola. */
@Composable
fun MochyLibraryCardGrid(entries: List<MochyLibraryCardEntry>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { e ->
                    MochyLibraryCategoryCard(e.title, e.count, e.covers, e.onClick, Modifier.weight(1f), e.roundCovers)
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Las tarjetas anchas: "Añadidas recientemente" y "Me gusta". */
@Composable
fun MochyLibraryWideCard(
    title: String,
    count: Int,
    covers: List<String?>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainerHigh).clickable(onClick = onClick),
    ) {
        MochyLibraryCoverStack(
            covers = covers, tile = 60.dp,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 14.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 14.dp, end = 110.dp)) {
            Text(
                title, color = scheme.onSurface, style = MochyLibraryLabelStyle,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(count.toString(), color = scheme.onSurfaceVariant, style = MochyLibraryCountStyle, maxLines = 1)
        }
    }
}
