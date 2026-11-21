package io.github.loki666.soundtrackplayer.data

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val appId: Int,
    val name: String,
    val artist: String?,
    val imageUrl: String,
    /** The game this soundtrack belongs to, when Steam knows it. */
    val parentAppId: Int? = null,
) {
    val storeUrl: String get() = "https://store.steampowered.com/app/$appId/"
}
