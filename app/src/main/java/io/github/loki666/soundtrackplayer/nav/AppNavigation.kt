package io.github.loki666.soundtrackplayer.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.loki666.soundtrackplayer.container
import io.github.loki666.soundtrackplayer.steam.AuthState
import io.github.loki666.soundtrackplayer.ui.album.AlbumScreen
import io.github.loki666.soundtrackplayer.ui.library.LibraryScreen
import io.github.loki666.soundtrackplayer.ui.login.SignInScreen
import io.github.loki666.soundtrackplayer.ui.login.SteamGuardScreen
import io.github.loki666.soundtrackplayer.ui.player.NowPlayingScreen

@Composable
fun AppNavigation() {
    val steam = LocalContext.current.container.steam
    val auth by steam.auth.collectAsStateWithLifecycle()

    val backStack = rememberNavBackStack(
        if (auth is AuthState.SignedIn) LibraryRoute else SignInRoute
    )

    // The auth state machine drives the sign-in part of the stack.
    LaunchedEffect(auth) {
        when (auth) {
            is AuthState.GuardRequired ->
                if (backStack.lastOrNull() != SteamGuardRoute) backStack.add(SteamGuardRoute)
            is AuthState.SignedIn ->
                if (backStack.any { it == SignInRoute || it == SteamGuardRoute }) backStack.resetTo(LibraryRoute)
            is AuthState.SignedOut ->
                if (backStack.lastOrNull() != SignInRoute) backStack.resetTo(SignInRoute)
            AuthState.SigningIn -> Unit
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.lastOrNull() == SteamGuardRoute) steam.cancelSignIn() else backStack.removeLastOrNull()
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<SignInRoute> { SignInScreen() }
            entry<SteamGuardRoute> { SteamGuardScreen(onBack = steam::cancelSignIn) }
            entry<LibraryRoute> {
                LibraryScreen(
                    onOpenAlbum = { backStack.add(AlbumRoute(it)) },
                    onOpenPlayer = { backStack.add(NowPlayingRoute) },
                )
            }
            entry<AlbumRoute> { key ->
                AlbumScreen(
                    appId = key.appId,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenPlayer = { backStack.add(NowPlayingRoute) },
                )
            }
            entry<NowPlayingRoute>(
                metadata = NavDisplay.transitionSpec {
                    slideInVertically { it } togetherWith fadeOut()
                } + NavDisplay.popTransitionSpec {
                    fadeIn() togetherWith slideOutVertically { it }
                } + NavDisplay.predictivePopTransitionSpec {
                    fadeIn() togetherWith slideOutVertically { it }
                },
            ) {
                NowPlayingScreen(
                    onClose = { backStack.removeLastOrNull() },
                    onOpenAlbum = { appId ->
                        backStack.removeLastOrNull()
                        if ((backStack.lastOrNull() as? AlbumRoute)?.appId != appId) backStack.add(AlbumRoute(appId))
                    },
                )
            }
        },
    )
}

private fun NavBackStack<NavKey>.resetTo(key: NavKey) {
    add(key)
    removeAll { it != key }
}
