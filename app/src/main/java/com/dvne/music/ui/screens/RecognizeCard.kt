package com.dvne.music.ui.screens

import com.dvne.music.R
import com.dvne.music.tr
import com.dvne.music.ui.components.DvneButton
import com.dvne.music.ui.components.DvneOutlinedButton
import com.dvne.music.ui.components.DvneTextButton
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.dvne.music.ui.theme.AppFonts
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dvne.music.DvneActivity
import com.dvne.music.container
import com.dvne.music.data.recognize.Recognizer
import com.dvne.music.ui.components.ActionSheet
import com.dvne.music.ui.components.Artwork
import com.dvne.music.ui.theme.LocalContainer
import com.dvne.music.ui.theme.LocalUi
import com.dvne.music.ui.theme.DvneIcons
import com.dvne.music.ui.theme.DvneTheme

/**
 * Song recognition as a card over whatever is on screen, for the Recognize widget and the Quick Settings tile: it
 * listens straight away (asking for the microphone the first time), then shows the song with Play and Open. The
 * microphone is only used while this card is on screen; leaving it stops listening.
 */
class RecognizeActivity : ComponentActivity() {
    // Dvne's language, when picked in Dvne (below Android 13; AppLanguage).
    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(com.dvne.music.AppLanguage.wrap(base))
    private val c by lazy { container }
    private val denied = mutableStateOf(false)
    private val ask = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        denied.value = !ok
        if (ok) c.recognizer.start(pauseDvne = true)
    }

    private fun granted() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun listen() {
        if (granted()) { denied.value = false; c.recognizer.start(pauseDvne = true) } else ask.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            c.recognizer.reset()
            listen()
        }
        setContent {
            val settings by c.settings.flow.collectAsState()
            CompositionLocalProvider(LocalContainer provides c) {
                DvneTheme(settings, null) {
                    RecognizeCard(
                        denied = denied.value,
                        onListen = ::listen,
                        onOpen = { uri ->
                            startActivity(Intent(this, DvneActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        onDone = ::finish,
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Never listening unseen: leaving the card (Home, another app) stops it.
        if (c.recognizer.state.value is Recognizer.State.Listening) c.recognizer.cancel()
        if (!isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && c.recognizer.state.value !is Recognizer.State.Listening) c.recognizer.reset()
    }
}

@Composable
private fun RecognizeCard(denied: Boolean, onListen: () -> Unit, onOpen: (String) -> Unit, onDone: () -> Unit) {
    val c = LocalContainer.current
    val state by c.recognizer.state.collectAsState()
    val found = state as? Recognizer.State.Found
    ActionSheet(found?.recognition?.cover ?: found?.song?.thumbnail, onDismiss = {
        if (c.recognizer.state.value is Recognizer.State.Listening) c.recognizer.cancel()
        onDone()
    }) { close ->
        // Fase 11: la tarjeta de Reconocer en estilo Mochy — laterales
        // de 20.dp, títulos en 20.sp negrita y textos en 15.sp.
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = state) {
                is Recognizer.State.Found -> {
                    val r = s.recognition
                    val song = s.song
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Artwork(r.cover ?: song?.thumbnail, Modifier.size(88.dp), LocalUi.current.art, placeholderIcon = DvneIcons.Song)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr(R.string.found_it), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(r.title ?: "", style = TextStyle(fontFamily = AppFonts.dvne, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(r.artist ?: "", style = TextStyle(fontFamily = AppFonts.dvne, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 19.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // #215: from the moment that was heard (the lyrics open with "Open", which shows the player).
                        if (song != null) DvneButton({ close { playHeard(c, s, song, lyrics = false) } }, Modifier.weight(1f)) {
                            Icon(DvneIcons.Play, null); Spacer(Modifier.width(6.dp)); Text(tr(R.string.widget_play))
                        }
                        // With a song: it plays and Dvne opens on the player; without one, on the Recognized list.
                        DvneOutlinedButton({
                            close {
                                if (song != null) playHeard(c, s, song)
                                onOpen(if (song != null) "dvne://player" else "dvne://recognized")
                            }
                        }, Modifier.weight(1f)) {
                            Text(if (song != null) tr(R.string.open_in_prism_2) else tr(R.string.see_in_prism))
                        }
                    }
                    if (song == null) Text(
                        tr(R.string.not_found_in_prism_music), Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DvneTextButton({ c.recognizer.reset(); onListen() }) { Text(tr(R.string.listen_again)) }
                }
                else -> {
                    val listening = s as? Recognizer.State.Listening
                    Text(tr(R.string.recognize), style = TextStyle(fontFamily = AppFonts.dvne, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp))
                    ListenButton(listening != null, listening?.level ?: 0f, size = 190.dp) {
                        if (listening != null) c.recognizer.cancel() else onListen()
                    }
                    Text(
                        when {
                            listening != null -> tr(R.string.listening_s, listening.heardMs / 1000)
                            denied -> tr(R.string.prism_needs_the_microphone_to_listen)
                            s == Recognizer.State.NotFound -> tr(R.string.couldn_t_recognize_that_one)
                            s == Recognizer.State.Saved -> tr(R.string.widget_recognize_waiting)
                            s == Recognizer.State.RateLimited -> tr(R.string.shazam_asks_to_wait_a_little)
                            s is Recognizer.State.Failed -> s.message
                            else -> tr(R.string.tap_to_listen)
                        },
                        style = TextStyle(fontFamily = AppFonts.dvne, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp), textAlign = TextAlign.Center,
                    )
                    Text(
                        when {
                            listening != null -> tr(R.string.hold_your_phone_near_the_music)
                            denied -> tr(R.string.tap_the_button_to_allow_it)
                            s == Recognizer.State.NotFound -> tr(R.string.try_again_closer_to_the_music)
                            s == Recognizer.State.Saved -> tr(R.string.you_ll_get_a_notification_when)
                            s == Recognizer.State.RateLimited -> tr(R.string.try_again_in_a_minute)
                            else -> tr(R.string.prism_listens_for_up_to_12)
                        },
                        Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
