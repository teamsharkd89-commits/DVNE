// MochyDesign.kt — Fase 2: sistema de diseño Mochy dentro de DVNE.
//
// TARGET PATH (cuando se instale en el repo):
//   app/src/main/java/com/dvne/music/ui/theme/MochyDesign.kt
//   paquete com.dvne.music.ui.theme, repo teamsharkd89-commits/DVNE
//   versión destino: 1.8.7.9 / versionCode 70 (siguiente cambio tras la 1.8.7.8)
//
// Qué es: SOLO tokens, estilos y el muestreador del glow. No toca ninguna
// pantalla existente, no cambia DvneTheme, no añade dependencias y no usa
// ningún archivo de Mochy tal cual. Las fases 3-4 lo empezarán a usar.
//
// Fuentes verificadas en código (no en resúmenes web):
// - Mochy Theme.kt: dark-only, fondo negro, surfaceVariant #1A1A1A,
//   ForceDarkTheme = true, Inter como fuente, extractThemeColor /
//   extractGradientColors con Palette + materialkolor Score.
// - Mochy HomeCards.kt: HomeContentInset 36.dp, HomeCardSpacing 15.dp,
//   HomeTitleStyle 24sp Bold -0.06em, HomeLabelStyle 16sp Bold,
//   HomeCardTitle 14sp SemiBold -0.03em, subtitle 12sp Medium -0.02em,
//   Rank 22sp Bold, HomeRowShape 14.dp, HomeRowHeight 54.dp,
//   card 137.dp (grande) / 104.dp (pequeña).
// - Mochy ContainerTransform.kt:44: HomeCardCornerRadius por defecto 25.dp.
// - Mochy HomeTopGlow.kt: HomeGlowSettings por defecto (mode COVERS,
//   strength 0.9, length 150dp, vividness 0.5, animated true), colores
//   fijos Amber/Blue/Violet/Green/Red/Pink, y HomeGlowColor (Palette +
//   Coil, sin Score): tono principal, par de tonos y paleta de hasta 4.
// - DVNE Theme.kt / Tokens.kt: DvneUi, AppFonts (Outfit por defecto,
//   sin Inter en res/font), pickSwatch con Palette, schemeFromSeed.
//
// Adaptaciones deliberadas (por qué no es un copy-paste):
// 1. Score de materialkolor NO existe en DVNE: el glow se muestrea solo
//    con Palette, como ya hace HomeGlowColor en Mochy. Sin esa dependencia.
// 2. Inter NO está en res/font de DVNE (tiene Outfit, Manrope, Plus Jakarta
//    Sans, Urbanist, Nunito...). La cara Mochy usa AppFonts.dvne (Outfit),
//    la predeterminada de DVNE y visualmente cercana. Añadir los 7 .ttf de
//    Inter es un paso aparte con archivos binarios, no se hace aquí.
// 3. Coil: Mochy usa context.imageLoader; DVNE usa
//    SingletonImageLoader.get(context) (ver Frosted.kt:151, Backdrop.kt:142).
// 4. Sin R.string: los modos/colores del glow usan claves planas ("covers",
//    "vivo", "fixed", "amber"...). Sus etiquetas en español llegan en
//    Fase 10 (Ajustes), no se inventan claves de strings aquí.
// 5. Mochy es dark-only y DVNE no: estos tokens son para el look Mochy en
//    modo oscuro. En modo claro DVNE sigue con su tema; el look Mochy no
//    se fuerza sobre él hasta que Fase 10 lo decida con Daniel.

package com.dvne.music.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Medidas y formas de Mochy. Valores literales de HomeCards.kt y
 * ContainerTransform.kt; nada aquí se calcula desde DvneUi para que el
 * look Mochy no se mueva si Daniel cambia los ajustes de DVNE.
 */
object MochyDesign {
    /** Inset lateral del contenido de Inicio (HomeCards.kt). */
    val contentInset: Dp = 36.dp
    /** Separación entre tarjetas en una fila (HomeCards.kt). */
    val cardSpacing: Dp = 15.dp
    /** Margen lateral de la zona táctil del título de sección. */
    val sectionTapMargin: Dp = 12.dp
    /** Alto de una fila de canción en Inicio. */
    val rowHeight: Dp = 54.dp
    /** Tamaño de tarjeta cuadrada: grande / pequeña (HomeCards.kt). */
    val cardSizeBig: Dp = 137.dp
    val cardSizeSmall: Dp = 104.dp

    /** Radio de esquina de las tarjetas: 25.dp por defecto en Mochy. */
    val cardCornerRadius: Dp = 25.dp
    /** Radio de la portada dentro de una fila (0.56 * 25, máx 20.dp). */
    val rowCoverCornerRadius: Dp = 14.dp
    /** Radio de una fila de canción. */
    val rowCornerRadius: Dp = 14.dp
    /** Radio del highlight del título de sección (píldora). */
    val sectionTitleCornerRadius: Dp = 100.dp
    /** Radio del highlight de una fila de canción. */
    val trackRowCornerRadius: Dp = 22.dp

    val cardShape: RoundedCornerShape get() = RoundedCornerShape(cardCornerRadius)
    val rowShape: RoundedCornerShape get() = RoundedCornerShape(rowCornerRadius)
    val rowCoverShape: RoundedCornerShape get() = RoundedCornerShape(rowCoverCornerRadius)

    /** Fondo y superficies del tema oscuro de Mochy (Theme.kt). */
    val background: Color = Color.Black
    val surface: Color = Color.Black
    val surfaceVariant: Color = Color(0xFF1A1A1A)
    val primary: Color = Color(0xFFE0E0E0)
    val secondary: Color = Color(0xFFB0B0B0)
    /** Texto de título de sección: blanco al 69%. */
    val sectionLabelColor: Color = Color.White.copy(alpha = 0.69f)
    /** Texto secundario de sección: blanco al 45%. */
    val sectionHintColor: Color = Color.White.copy(alpha = 0.45f)
    /** Chevron de sección: blanco al 55%. */
    val sectionChevronColor: Color = Color.White.copy(alpha = 0.55f)
}

/**
 * Estilos de texto de Mochy (HomeCards.kt). La fuente es la de DVNE por
 * defecto (Outfit, AppFonts.dvne): Inter no está empaquetada en DVNE.
 * El tracking negativo es lo que le da el aire compacto de Mochy.
 */
object MochyText {
    fun title(font: FontFamily = AppFonts.dvne): TextStyle = TextStyle(
        fontFamily = font, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.06).em,
    )

    fun sectionLabel(font: FontFamily = AppFonts.dvne): TextStyle = TextStyle(
        fontFamily = font, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 19.sp, letterSpacing = (-0.06).em,
    )

    fun cardTitle(font: FontFamily = AppFonts.dvne): TextStyle = TextStyle(
        fontFamily = font, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = (-0.03).em,
    )

    fun cardSubtitle(font: FontFamily = AppFonts.dvne): TextStyle = TextStyle(
        fontFamily = font, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = (-0.02).em,
    )

    fun rank(font: FontFamily = AppFonts.dvne): TextStyle = TextStyle(
        fontFamily = font, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, letterSpacing = (-0.06).em,
    )
}

/** De dónde salen los colores del glow superior de Inicio (HomeTopGlow.kt). */
enum class MochyGlowMode(val key: String) {
    /** Las dos primeras portadas del feed (comportamiento original). */
    COVERS("covers"),
    /** La portada de la canción que suena; sin ella, las del feed. */
    VIVO("vivo"),
    /** Un color fijo para las dos esquinas. */
    FIXED("fixed");

    companion object {
        fun fromKey(key: String): MochyGlowMode = entries.find { it.key == key } ?: COVERS
    }
}

/** Colores fijos que puede tomar el glow (HomeTopGlow.kt). */
enum class MochyGlowFixedColor(val key: String, val color: Color) {
    AMBER("amber", Color(0xFFE09112)),
    BLUE("blue", Color(0xFF2E7DE1)),
    VIOLET("violet", Color(0xFF7C4DFF)),
    GREEN("green", Color(0xFF2E9E5B)),
    RED("red", Color(0xFFE14B4B)),
    PINK("pink", Color(0xFFE14B9B));

    companion object {
        fun fromKey(key: String): MochyGlowFixedColor = entries.find { it.key == key } ?: AMBER
    }
}

/**
 * Ajustes del glow (HomeTopGlow.kt). Valores por defecto de una
 * instalación nueva de Mochy. Largo en dp pasado el inset superior.
 */
data class MochyGlowSettings(
    val mode: MochyGlowMode = MochyGlowMode.COVERS,
    val fixedColor: Color = MochyGlowFixedColor.AMBER.color,
    /** 0..1, alfa máximo del glow. */
    val strength: Float = 0.9f,
    /** Hasta dónde baja el degradado; rango 80..400. */
    val length: Float = 150f,
    /** 0..1: 0 es el atenuado viejo, 1 deja los colores a tope. */
    val vividness: Float = 0.5f,
    /** true: los colores caminan por los tonos de la portada cada 4 s. */
    val animated: Boolean = true,
) {
    companion object {
        const val LENGTH_MIN = 80f
        const val LENGTH_MAX = 400f
    }
}

/**
 * Muestreador del glow, adaptado de HomeGlowColor (HomeTopGlow.kt).
 * Solo Palette + Coil3, ambos ya en DVNE. Sin materialkolor Score.
 * Un color por portada, cacheado para no muestrear dos veces; el
 * vividness se aplica al leer, así el slider repinta al instante.
 */
object MochyGlowColor {
    private val cache = LruCache<String, Int>(48)
    private val pairCache = LruCache<String, Pair<Int, Int>>(48)
    private val paletteCache = LruCache<String, List<Int>>(48)

    fun cached(url: String, vividness: Float): Color? =
        cache.get(url)?.let { Color(dimArgb(it, vividness)) }

    suspend fun load(context: Context, url: String, vividness: Float): Color? {
        cached(url, vividness)?.let { return it }
        val bitmap = loadBitmap(context, url) ?: return null
        val argb = withContext(Dispatchers.Default) { rawTone(bitmap) } ?: return null
        cache.put(url, argb)
        return Color(dimArgb(argb, vividness))
    }

    fun cachedPair(url: String, vividness: Float): Pair<Color, Color>? =
        pairCache.get(url)?.let { Color(dimArgb(it.first, vividness)) to Color(dimArgb(it.second, vividness)) }

    suspend fun loadPair(context: Context, url: String, vividness: Float): Pair<Color, Color>? {
        cachedPair(url, vividness)?.let { return it }
        val bitmap = loadBitmap(context, url) ?: return null
        val tones = withContext(Dispatchers.Default) { rawPairTones(bitmap) } ?: return null
        pairCache.put(url, tones)
        return Color(dimArgb(tones.first, vividness)) to Color(dimArgb(tones.second, vividness))
    }

    fun cachedPalette(url: String, vividness: Float): List<Color>? =
        paletteCache.get(url)?.map { Color(dimArgb(it, vividness)) }?.takeIf { it.size >= 2 }

    suspend fun loadPalette(context: Context, url: String, vividness: Float): List<Color>? {
        cachedPalette(url, vividness)?.let { return it }
        val bitmap = loadBitmap(context, url) ?: return null
        val tones = withContext(Dispatchers.Default) { rawPaletteTones(bitmap) } ?: return null
        paletteCache.put(url, tones)
        return tones.map { Color(dimArgb(it, vividness)) }.takeIf { it.size >= 2 }
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        return try {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(64)
                    .allowHardware(false)
                    .build()
                (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun rawTone(bitmap: Bitmap): Int? {
        val swatches = Palette.from(bitmap).maximumColorCount(12).generate().swatches
        return pickGlowSwatch(swatches)?.rgb
    }

    private fun rawPairTones(bitmap: Bitmap): Pair<Int, Int>? {
        val swatches = Palette.from(bitmap).maximumColorCount(12).generate().swatches
        val first = pickGlowSwatch(swatches) ?: return null
        val second = swatches
            .filter { it !== first && hueDistance(it.hsl[0], first.hsl[0]) >= 30f }
            .filter { it.hsl[1] >= 0.2f && it.hsl[2] in 0.08f..0.9f && it.population >= first.population / 8 }
            .maxByOrNull { it.population }
            ?: swatches.filter { it !== first }.maxByOrNull { it.population }
            ?: first
        return first.rgb to second.rgb
    }

    private fun rawPaletteTones(bitmap: Bitmap): List<Int>? {
        val swatches = Palette.from(bitmap).maximumColorCount(12).generate().swatches
        val dominant = swatches.maxByOrNull { it.population } ?: return null
        val ranked = swatches
            .filter { it.hsl[1] >= 0.2f && it.hsl[2] in 0.08f..0.9f && it.population >= dominant.population / 8 }
            .sortedByDescending { it.population }
        val picked = mutableListOf<Palette.Swatch>()
        for (swatch in ranked) {
            if (picked.size >= 4) break
            if (picked.any { hueDistance(it.hsl[0], swatch.hsl[0]) < 30f }) continue
            picked += swatch
        }
        return picked.map { it.rgb }.takeIf { it.size >= 2 }
    }

    private fun pickGlowSwatch(swatches: List<Palette.Swatch>): Palette.Swatch? {
        val dominant = swatches.maxByOrNull { it.population } ?: return null
        return swatches
            .filter { it.hsl[1] >= 0.2f && it.hsl[2] in 0.08f..0.9f && it.population >= dominant.population / 6 }
            .maxByOrNull { it.population }
            ?: dominant
    }

    /** Atenuado del glow: vividness 0 = atenuado viejo (sat ≤ 0.6, luz 0.2-0.32). */
    fun dimmed(color: Color, vividness: Float): Color = Color(dimArgb(color.toArgb(), vividness))

    private fun dimArgb(argb: Int, vividness: Float): Int {
        val v = vividness.coerceIn(0f, 1f)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = hsl[1].coerceAtMost(0.6f + 0.4f * v)
        hsl[2] = hsl[2].coerceIn(0.2f, 0.32f + 0.28f * v)
        return ColorUtils.HSLToColor(hsl)
    }

    private fun hueDistance(a: Float, b: Float): Float {
        val d = kotlin.math.abs(a - b) % 360f
        return minOf(d, 360f - d)
    }
}
