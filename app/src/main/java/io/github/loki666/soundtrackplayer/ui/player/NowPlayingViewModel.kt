package io.github.loki666.soundtrackplayer.ui.player

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.loki666.soundtrackplayer.playback.PlayerConnection
import javax.inject.Inject

/** Gives the Now Playing screen the app-wide player connection. */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(val player: PlayerConnection) : ViewModel()
