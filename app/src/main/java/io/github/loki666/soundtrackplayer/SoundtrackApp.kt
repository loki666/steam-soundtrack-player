package io.github.loki666.soundtrackplayer

import android.app.Application
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

class SoundtrackApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Android ships a stripped-down "BC" provider; JavaSteam needs the full one for
        // RSA/None/PKCS1Padding and AES/CBC/PKCS7Padding, so replace it before anything touches Steam.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)

        container = AppContainer(this)
    }
}
