package io.github.loki666.soundtrackplayer.steam

enum class CodeKind { AuthenticatorApp, Email }

sealed interface AuthState {
    data class SignedOut(val error: String? = null) : AuthState

    data object SigningIn : AuthState

    /**
     * Steam wants a second factor. Steam can accept several at once: an approval in the Steam
     * mobile app (polled automatically) and/or a typed code.
     */
    data class GuardRequired(
        val canApproveInApp: Boolean,
        val codeKind: CodeKind?,
        val emailHint: String?,
        val submittingCode: Boolean = false,
        val codeError: String? = null,
    ) : AuthState

    data class SignedIn(val accountName: String) : AuthState
}
