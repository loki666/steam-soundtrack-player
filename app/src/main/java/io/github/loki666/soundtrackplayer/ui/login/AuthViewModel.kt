package io.github.loki666.soundtrackplayer.ui.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import io.github.loki666.soundtrackplayer.steam.AuthState
import io.github.loki666.soundtrackplayer.steam.SteamSession
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AuthViewModel @Inject constructor(private val steam: SteamSession) : ViewModel() {
    val state: StateFlow<AuthState> = steam.auth

    fun signIn(username: String, password: String) = steam.signIn(username, password)
    fun submitCode(code: String) = steam.submitGuardCode(code)
    fun cancel() = steam.cancelSignIn()
}
