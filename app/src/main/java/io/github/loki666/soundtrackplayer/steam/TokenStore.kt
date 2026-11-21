package io.github.loki666.soundtrackplayer.steam

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class StoredLogin(val accountName: String, val refreshToken: String)

/**
 * Keeps the Steam refresh token (a full client credential) encrypted with an Android Keystore
 * AES key that never leaves the device. The password itself is never stored.
 */
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("steam_auth", Context.MODE_PRIVATE)

    fun load(): StoredLogin? {
        val account = prefs.getString(KEY_ACCOUNT, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null)?.let(::decrypt) ?: return null
        return StoredLogin(account, token)
    }

    fun save(login: StoredLogin) {
        prefs.edit()
            .putString(KEY_ACCOUNT, login.accountName)
            .putString(KEY_TOKEN, encrypt(login.refreshToken))
            .apply()
    }

    /** Steam Guard "remember this device" data, so the next sign-in can skip the code. */
    fun guardData(accountName: String): String? =
        prefs.getString("$KEY_GUARD.${accountName.lowercase()}", null)?.let(::decrypt)

    fun saveGuardData(accountName: String, data: String) {
        prefs.edit().putString("$KEY_GUARD.${accountName.lowercase()}", encrypt(data)).apply()
    }

    fun clearLogin() {
        prefs.edit().remove(KEY_ACCOUNT).remove(KEY_TOKEN).apply()
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val out = cipher.iv + cipher.doFinal(plain.toByteArray())
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String? = runCatching {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_SIZE))
        }
        String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE))
    }.getOrNull()

    private companion object {
        const val KEY_ALIAS = "steam_refresh_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val KEY_ACCOUNT = "account"
        const val KEY_TOKEN = "refresh_token"
        const val KEY_GUARD = "guard"
    }
}
