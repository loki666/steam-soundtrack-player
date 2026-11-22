package io.github.loki666.soundtrackplayer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.security.Security
import javax.inject.Inject
import org.bouncycastle.jce.provider.BouncyCastleProvider

@HiltAndroidApp
class SoundtrackApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    init {
        // JavaSteam decompresses LZMA depot chunks on Dispatchers.IO and gives every thread its own
        // 8 MB window buffer (a ThreadLocal it never frees). IO's default 64 threads let that grow
        // past the heap, so cap it. Must run before anything touches Dispatchers.IO; the Application
        // constructor runs before content providers and onCreate.
        System.setProperty("kotlinx.coroutines.io.parallelism", IO_PARALLELISM.toString())
    }

    override fun onCreate() {
        // Before super.onCreate(), where Hilt injects this class and may start building singletons.
        installBouncyCastle()
        super.onCreate()
    }

    /**
     * WorkManager is initialized on demand (its startup initializer is removed in the manifest), so
     * restarted downloads are created by Hilt only after onCreate has injected [workerFactory].
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

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
