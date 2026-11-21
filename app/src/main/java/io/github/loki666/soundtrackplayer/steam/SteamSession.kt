package io.github.loki666.soundtrackplayer.steam

import android.content.Context
import android.os.Build
import android.util.Log
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.EAuthSessionGuardType
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.AuthenticationException
import `in`.dragonbra.javasteam.steam.authentication.CredentialsAuthSession
import `in`.dragonbra.javasteam.steam.authentication.SteamAuthentication
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.LicenseListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import java.io.IOException
import kotlin.concurrent.thread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class SessionExpiredException : IOException("Your Steam session has expired. Please sign in again.")

/**
 * One long-lived connection to Steam's client servers (the same protocol the desktop client uses).
 *
 * Sign-in uses Steam's credential flow with the "SteamClient" platform type, which is what lets the
 * resulting refresh token log on to the CM servers and download depots. The refresh token is the
 * only thing persisted; we log on with it lazily whenever something needs the network.
 */
class SteamSession(
    context: Context,
    private val tokens: TokenStore,
    private val scope: CoroutineScope,
) {
    val client = SteamClient()
    private val callbacks = CallbackManager(client)
    private val user = requireNotNull(client.getHandler<SteamUser>())
    val apps: SteamApps = requireNotNull(client.getHandler<SteamApps>())
    private val authentication = SteamAuthentication(client)

    private val _auth = MutableStateFlow<AuthState>(
        tokens.load()?.let { AuthState.SignedIn(it.accountName) } ?: AuthState.SignedOut()
    )
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    private val licenses = MutableStateFlow<List<License>?>(null)
    private val connectLock = Mutex()
    private val logOnLock = Mutex()
    @Volatile private var connected = CompletableDeferred<Unit>()
    @Volatile private var loggedOn: CompletableDeferred<LoggedOnCallback>? = null
    @Volatile private var isLoggedOn = false

    // Pending Steam Guard step of an in-progress sign-in.
    private var guardSession: CredentialsAuthSession? = null
    private var guardCodeType: EAuthSessionGuardType? = null
    private var signInJob: Job? = null

    private val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifEmpty { "Android" } +
        " (" + context.packageManager.getApplicationLabel(context.applicationInfo) + ")"

    init {
        callbacks.subscribe<ConnectedCallback> { connected.complete(Unit) }
        callbacks.subscribe<DisconnectedCallback> {
            isLoggedOn = false
            licenses.value = null
            connected = CompletableDeferred()
            loggedOn?.completeExceptionally(IOException("Disconnected from Steam"))
        }
        callbacks.subscribe<LoggedOnCallback> { loggedOn?.complete(it) }
        callbacks.subscribe<LoggedOffCallback> {
            isLoggedOn = false
            if (it.result.isTokenProblem()) expireSession()
        }
        callbacks.subscribe<LicenseListCallback> {
            if (it.result == EResult.OK) licenses.value = it.licenseList
        }
        thread(isDaemon = true, name = "steam-callbacks") {
            while (true) callbacks.runWaitCallbacks(1_000L)
        }
    }

    // region Sign-in

    fun signIn(username: String, password: String) {
        signInJob?.cancel()
        _auth.value = AuthState.SigningIn
        signInJob = scope.launch {
            try {
                ensureConnected()
                val details = AuthSessionDetails().apply {
                    this.username = username.trim()
                    this.password = password
                    persistentSession = true
                    deviceFriendlyName = deviceName
                    guardData = tokens.guardData(username.trim())
                }
                val session = authentication.beginAuthSessionViaCredentials(details).await()
                val types = session.allowedConfirmations.map { it.confirmationType }

                if (EAuthSessionGuardType.k_EAuthSessionGuardType_None !in types) {
                    guardSession = session
                    guardCodeType = when {
                        EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode in types ->
                            EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode
                        EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode in types ->
                            EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode
                        else -> null
                    }
                    _auth.value = AuthState.GuardRequired(
                        canApproveInApp = EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation in types,
                        codeKind = when (guardCodeType) {
                            EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode -> CodeKind.AuthenticatorApp
                            EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode -> CodeKind.Email
                            else -> null
                        },
                        emailHint = session.allowedConfirmations
                            .firstOrNull { it.confirmationType == EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode }
                            ?.associatedMessage?.takeIf { it.isNotBlank() },
                    )
                }

                // Polling covers every path: no guard, an approval in the Steam app, or a code
                // submitted through submitGuardCode() (the next poll then returns the tokens).
                val result = pollUntilAuthenticated(session)
                finishSignIn(result)
            } catch (e: AuthenticationException) {
                Log.w(TAG, "Sign-in failed", e)
                _auth.value = AuthState.SignedOut(e.result.describe())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Sign-in failed", e)
                _auth.value = AuthState.SignedOut("Couldn't reach Steam. Check your connection and try again.")
            } finally {
                guardSession = null
                guardCodeType = null
            }
        }
    }

    fun submitGuardCode(code: String) {
        val session = guardSession ?: return
        val type = guardCodeType ?: return
        _auth.update { (it as? AuthState.GuardRequired)?.copy(submittingCode = true, codeError = null) ?: it }
        scope.launch {
            try {
                session.sendSteamGuardCode(code.trim().uppercase(), type).await()
                // Success is picked up by the polling loop in signIn().
            } catch (e: Exception) {
                val wrong = (e as? AuthenticationException)?.result in setOf(
                    EResult.InvalidLoginAuthCode, EResult.TwoFactorCodeMismatch,
                )
                _auth.update {
                    (it as? AuthState.GuardRequired)?.copy(
                        submittingCode = false,
                        codeError = if (wrong) "That code isn't right. Try again." else "Couldn't verify the code.",
                    ) ?: it
                }
            }
        }
    }

    fun cancelSignIn() {
        signInJob?.cancel()
        _auth.value = AuthState.SignedOut()
    }

    private suspend fun pollUntilAuthenticated(session: CredentialsAuthSession): AuthPollResult {
        while (true) {
            session.pollAuthSessionStatus().await()?.let { return it }
            delay(POLL_INTERVAL_MS)
        }
    }

    private suspend fun finishSignIn(result: AuthPollResult) {
        result.newGuardData?.takeIf { it.isNotEmpty() }?.let { tokens.saveGuardData(result.accountName, it) }
        val login = StoredLogin(result.accountName, result.refreshToken)
        tokens.save(login)
        logOn(login)
        _auth.value = AuthState.SignedIn(login.accountName)
    }

    // endregion

    // region Connection

    /** Makes sure we're connected and logged on with the stored refresh token. */
    suspend fun ensureLoggedOn() {
        if (isLoggedOn && client.isConnected) return
        val login = tokens.load() ?: throw SessionExpiredException()
        logOn(login)
    }

    /** The account's licenses, which are needed to resolve owned apps and depot access. */
    suspend fun licenses(): List<License> {
        ensureLoggedOn()
        return withTimeout(30_000) { licenses.filterNotNull().first() }
    }

    private suspend fun ensureConnected() = connectLock.withLock {
        if (client.isConnected) return@withLock
        connected = CompletableDeferred()
        client.connect()
        withTimeout(CONNECT_TIMEOUT_MS) { connected.await() }
    }

    private suspend fun logOn(login: StoredLogin) = logOnLock.withLock {
        if (isLoggedOn && client.isConnected) return@withLock
        ensureConnected()
        val pending = CompletableDeferred<LoggedOnCallback>().also { loggedOn = it }
        user.logOn(
            LogOnDetails(
                username = login.accountName,
                accessToken = login.refreshToken,
                shouldRememberPassword = true,
                machineName = deviceName,
            )
        )
        val callback = withTimeout(CONNECT_TIMEOUT_MS) { pending.await() }
        when {
            callback.result == EResult.OK -> isLoggedOn = true
            callback.result.isTokenProblem() -> {
                expireSession()
                throw SessionExpiredException()
            }
            else -> throw IOException("Steam log on failed: ${callback.result}")
        }
    }

    fun signOut() {
        scope.launch {
            runCatching { if (isLoggedOn) user.logOff() }
            runCatching { client.disconnect() }
        }
        isLoggedOn = false
        tokens.clearLogin()
        _auth.value = AuthState.SignedOut()
    }

    private fun expireSession() {
        tokens.clearLogin()
        _auth.value = AuthState.SignedOut("Your Steam session has expired. Please sign in again.")
    }

    // endregion

    private companion object {
        const val TAG = "SteamSession"
        const val POLL_INTERVAL_MS = 2_000L
        const val CONNECT_TIMEOUT_MS = 30_000L

        fun EResult.isTokenProblem() = this in setOf(
            EResult.InvalidPassword, EResult.AccessDenied, EResult.Expired, EResult.Revoked, EResult.InvalidSignature,
        )

        fun EResult?.describe(): String = when (this) {
            EResult.InvalidPassword -> "Wrong account name or password."
            EResult.RateLimitExceeded, EResult.AccountLoginDeniedThrottle ->
                "Too many sign-in attempts. Wait a few minutes and try again."
            EResult.Expired, EResult.FileNotFound -> "The sign-in request expired. Please try again."
            else -> "Sign-in failed${this?.let { " ($it)" } ?: ""}."
        }
    }
}
