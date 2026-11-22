package io.github.loki666.soundtrackplayer

import android.app.Application
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

class SoundtrackApp : Application() {
    /**
     * Created on first use, not in onCreate: after a force-stop or reboot WorkManager's startup
     * initializer restarts unfinished downloads on a background thread before onCreate runs, and
     * DownloadWorker reads the container in its constructor.
     */
    val container: AppContainer by lazy {
        installBouncyCastle()
        AppContainer(this)
    }

    init {
        // JavaSteam decompresses LZMA depot chunks on Dispatchers.IO and gives every thread its own
        // 8 MB window buffer (a ThreadLocal it never frees). IO's default 64 threads let that grow
        // past the heap, so cap it. Must run before anything touches Dispatchers.IO; the Application
        // constructor runs before content providers and onCreate.
        System.setProperty("kotlinx.coroutines.io.parallelism", IO_PARALLELISM.toString())
    }

    override fun onCreate() {
        super.onCreate()
        container // Build it now on a normal launch, as before.
    }

    /**
     * Android ships a stripped-down "BC" provider; JavaSteam needs the full one for
     * RSA/None/PKCS1Padding and AES/CBC/PKCS7Padding, so replace it before anything touches Steam.
     */
    private fun installBouncyCastle() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    private companion object {
        const val IO_PARALLELISM = 8
    }
}
