package io.github.loki666.soundtrackplayer.ui.login

import androidx.lifecycle.ViewModel
import io.github.loki666.soundtrackplayer.steam.AuthState
import io.github.loki666.soundtrackplayer.steam.SteamSession
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(private val steam: SteamSession) : ViewModel() {
    val state: StateFlow<AuthState> = steam.auth

    fun signIn(username: String, password: String) = steam.signIn(username, password)
    fun submitCode(code: String) = steam.submitGuardCode(code)
    fun cancel() = steam.cancelSignIn()
}
