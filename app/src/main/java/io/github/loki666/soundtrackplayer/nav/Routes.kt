package io.github.loki666.soundtrackplayer.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object SignInRoute : NavKey
@Serializable data object SteamGuardRoute : NavKey
@Serializable data object LibraryRoute : NavKey
@Serializable data class AlbumRoute(val appId: Int) : NavKey
@Serializable data object NowPlayingRoute : NavKey
