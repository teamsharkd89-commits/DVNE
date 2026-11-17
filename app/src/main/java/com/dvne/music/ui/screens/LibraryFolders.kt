package com.dvne.music.ui.screens

import com.dvne.music.R
import com.dvne.music.tr
import com.dvne.music.trp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dvne.music.data.DvneLibrary
import com.dvne.music.data.model.BrowseItem
import com.dvne.music.ui.LocalNavigator
import com.dvne.music.ui.components.EmptyState
import com.dvne.music.ui.components.DvneTextButton
import com.dvne.music.ui.components.RoundAction
import com.dvne.music.ui.components.SubPage
import com.dvne.music.ui.theme.LocalAppSettings
import com.dvne.music.ui.theme.LocalContainer
import com.dvne.music.ui.theme.DvneIcons

/**
 * Every playlist Library lists (Dvne's own, saved ones signed out, the account's), by id: what a folder's ids point at.
 * Ids the library no longer has (a playlist deleted elsewhere) simply don't show.
 */
@Composable
internal fun rememberLibraryPlaylists(): List<BrowseItem> {
    val c = LocalContainer.current
    val settings = LocalAppSettings.current
    val playlists by c.library.playlists.collectAsState()
    val dvneLists by c.dvneLibrary.lists.collectAsState()
    val saved by c.dvneLibrary.playlists.collectAsState()
    return remember(playlists, dvneLists, saved, settings.isLoggedIn) {
        dvneLists.map { it.item() } + (if (settings.isLoggedIn) emptyList() else saved.map { it.item() }) + playlists.filter { it.id != "LM" }
    }
}

/** A folder of playlists in Library (1.7.6): its playlists as rows; renamed or taken apart from the top bar. */
@Composable
fun FolderScreen(id: String, bottomPadding: Dp) {
    val c = LocalContainer.current
    val nav = LocalNavigator.current
    val folders by c.dvneLibrary.folders.collectAsState()
    val folder = folders.firstOrNull { it.id == id }
    val all = rememberLibraryPlaylists()
    var renaming by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<BrowseItem?>(null) }
    if (folder == null) {
        SubPage(tr(R.string.folder), bottomPadding) { item { EmptyState(DvneIcons.Folder, tr(R.string.this_folder_is_gone), tr(R.string.its_playlists_are_back_in_library)) } }
        return
    }
    val inside = folder.playlists.mapNotNull { pid -> all.firstOrNull { DvneLibrary.samePlaylist(it.id, pid) } }
    SubPage(folder.name, bottomPadding, subtitle = trp(R.plurals.n_playlists, inside.size), actions = {
        RoundAction(DvneIcons.Edit, tr(R.string.rename_folder)) { renaming = true }
        RoundAction(DvneIcons.Delete, tr(R.string.take_the_folder_apart)) { c.dvneLibrary.deleteFolder(id); nav.back() }
    }) {
        if (inside.isEmpty()) item {
            EmptyState(DvneIcons.Folder, tr(R.string.nothing_in_here_yet), tr(R.string.hold_a_playlist_in_library_playlists))
        }
        items(inside, key = { it.id }) { p -> ResultRow(p, onLongClick = { menuFor = p }) { nav.open(p) {} } }
    }
    if (renaming) FolderNameDialog(tr(R.string.rename_folder), folder.name, { renaming = false }) { c.dvneLibrary.renameFolder(id, it); renaming = false }
    menuFor?.let { p ->
        com.dvne.music.ui.components.PlayActionsSheet(
            com.dvne.music.ui.components.Playable.Of(p),
            extra = listOf(com.dvne.music.ui.components.SheetItem(DvneIcons.Folder, tr(R.string.take_out_of, folder.name)) { c.dvneLibrary.moveToFolder(p.id, null) }),
        ) { menuFor = null }
    }
}

/** Where a playlist goes: one of the folders, a new one, or out of its folder. */
@Composable
internal fun MoveToFolderSheet(playlist: BrowseItem, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val folders by c.dvneLibrary.folders.collectAsState()
    val current = folders.firstOrNull { f -> f.playlists.any { DvneLibrary.samePlaylist(it, playlist.id) } }
    var naming by remember { mutableStateOf(false) }
    if (naming) {
        FolderNameDialog(tr(R.string.new_folder), "", { naming = false }) { c.dvneLibrary.createFolder(it, playlist.id); naming = false; onDismiss() }
        return
    }
    com.dvne.music.ui.components.ActionSheet(c.covers.art(playlist), onDismiss) { close ->
        com.dvne.music.ui.components.ActionHeader(c.covers.art(playlist), playlist.title, current?.let { tr(R.string.in_text, it.name) } ?: tr(R.string.move_to_a_folder), placeholderIcon = DvneIcons.QueueMusic)
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            folders.filter { it.id != current?.id }.forEach { f ->
                FolderRow(DvneIcons.Folder, f.name, trp(R.plurals.n_playlists, f.playlists.size)) {
                    c.dvneLibrary.moveToFolder(playlist.id, f.id); close {}
                }
            }
            FolderRow(DvneIcons.Add, tr(R.string.new_folder), null) { naming = true }
            if (current != null) FolderRow(DvneIcons.Remove, tr(R.string.take_out_of, current.name), null) {
                c.dvneLibrary.moveToFolder(playlist.id, null); close {}
            }
        }
    }
}

@Composable
private fun FolderRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
internal fun FolderNameDialog(title: String, current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            com.dvne.music.ui.components.DvneTextField(
                text, { text = it.take(60) }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(tr(R.string.folder_name)) },
            )
        },
        confirmButton = { DvneTextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(tr(R.string.save)) } },
        dismissButton = { DvneTextButton(onClick = onDismiss) { Text(tr(R.string.cancel)) } },
    )
}
