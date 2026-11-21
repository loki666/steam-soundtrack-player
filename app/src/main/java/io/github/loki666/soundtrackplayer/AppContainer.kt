package io.github.loki666.soundtrackplayer

import android.content.Context
import io.github.loki666.soundtrackplayer.download.DownloadScheduler
import io.github.loki666.soundtrackplayer.library.LibraryRepository
import io.github.loki666.soundtrackplayer.library.LibraryStore
import io.github.loki666.soundtrackplayer.library.SteamCatalog
import io.github.loki666.soundtrackplayer.playback.PlayerConnection
import io.github.loki666.soundtrackplayer.steam.SteamSession
import io.github.loki666.soundtrackplayer.steam.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-wired app-scoped dependencies. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val tokenStore = TokenStore(context)
    val steam = SteamSession(context, tokenStore, appScope)
    val libraryStore = LibraryStore(context)
    val downloads = DownloadScheduler(context)
    val library = LibraryRepository(
        store = libraryStore,
        catalog = SteamCatalog(steam),
        downloads = downloads,
        scope = appScope,
    )
    val player = PlayerConnection(context, appScope)
}

val Context.container: AppContainer
    get() = (applicationContext as SoundtrackApp).container
