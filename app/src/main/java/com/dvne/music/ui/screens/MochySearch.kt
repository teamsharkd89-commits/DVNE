// MochySearch.kt — Fase 6: Búsqueda estilo Mochy en DVNE.
//
// Doble toque en el tab Buscar (Mochy): pide el foco del campo de
// búsqueda para escribir sin tocar el campo. TabItem lo escribe,
// SearchScreen lo consume con un LaunchedEffect sobre [requestAt].
// Es solo un instante en milisegundos: si Búsqueda no está compuesta
// cuando llega, lo lee al componerse; si ya lo está, el efecto se
// dispara al cambiar. Los títulos de sección de Búsqueda (landing y
// resultados) son directamente los de Mochy — MochySectionHeader, sin
// strapline — porque Búsqueda no tiene plantilla como el Inicio.

package com.dvne.music.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

object MochySearchFocus {
    var requestAt by mutableLongStateOf(0L)
        private set

    fun request() {
        requestAt = System.currentTimeMillis()
    }
}
