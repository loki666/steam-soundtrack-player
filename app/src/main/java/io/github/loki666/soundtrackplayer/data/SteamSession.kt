package io.github.loki666.soundtrackplayer.data

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The Steam session lives entirely in the WebView cookie jar: the user signs in on Steam's own
 * login page, and we only read the resulting cookies. The app never sees the password.
 */
object SteamSession {
    const val STORE_URL = "https://store.steampowered.com"

    /** Store login; Steam transfers the session to steamcommunity.com as part of the flow. */
    const val LOGIN_URL = "$STORE_URL/login/?redir=account%2F"

    private const val LOGIN_COOKIE = "steamLoginSecure"

    fun storeCookies(): String? = CookieManager.getInstance().getCookie(STORE_URL)

    /** The SteamID64 of the logged-in user, or null if there is no session. */
    fun steamId(): String? {
        val raw = storeCookies()
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("$LOGIN_COOKIE=") }
            ?.substringAfter('=')
            ?: return null
        // Cookie value is "<steamid64>||<jwt>", URL-encoded.
        val id = Uri.decode(raw).substringBefore("||")
        return id.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
    }

    fun isLoggedIn(): Boolean = steamId() != null

    fun persist() = CookieManager.getInstance().flush()

    suspend fun logout() {
        WebStorage.getInstance().deleteAllData()
        suspendCancellableCoroutine { cont ->
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
                cont.resume(Unit)
            }
        }
    }
}
