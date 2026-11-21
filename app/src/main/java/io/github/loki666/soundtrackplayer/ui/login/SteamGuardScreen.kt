package io.github.loki666.soundtrackplayer.ui.login

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.PhonelinkLock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.loki666.soundtrackplayer.container
import io.github.loki666.soundtrackplayer.steam.AuthState
import io.github.loki666.soundtrackplayer.steam.CodeKind
import io.github.loki666.soundtrackplayer.ui.theme.AppColors

private const val STEAM_APP_PACKAGE = "com.valvesoftware.android.steam.community"

@Composable
fun SteamGuardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val steam = context.container.steam
    val vm = viewModel { AuthViewModel(steam) }
    val state = vm.state.collectAsStateWithLifecycle().value as? AuthState.GuardRequired ?: return

    var code by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(
                Modifier.size(120.dp).border(2.dp, AppColors.Outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(88.dp).background(AppColors.Surface, CircleShape).border(2.dp, AppColors.Accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.canApproveInApp || state.codeKind == CodeKind.AuthenticatorApp) Icons.Outlined.PhonelinkLock
                        else Icons.Outlined.MarkEmailUnread,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            if (state.canApproveInApp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Approve in the Steam app", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(
                        "Open the Steam app on this phone and tap Approve on the sign-in request. We'll continue automatically.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Waiting for approval…", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(
                    onClick = {
                        val launch = context.packageManager.getLaunchIntentForPackage(STEAM_APP_PACKAGE)
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$STEAM_APP_PACKAGE"))
                        runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Steam app", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your Steam Guard code", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(
                        when (state.codeKind) {
                            CodeKind.Email -> "We sent a code to ${state.emailHint ?: "your email address"}."
                            else -> "Open the Steam app and copy the code from Steam Guard."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        val kind = state.codeKind
        if (kind != null) {
            if (state.canApproveInApp) {
                Row(
                    Modifier.padding(top = 40.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = AppColors.Outline)
                    Text("or enter a code", color = AppColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(Modifier.weight(1f), color = AppColors.Outline)
                }
            } else {
                Spacer(Modifier.height(32.dp))
            }

            val canVerify = code.length >= 5 && !state.submittingCode
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isLetterOrDigit).take(5).uppercase() },
                    label = { Text(if (kind == CodeKind.Email) "Code from email" else "Steam Guard code") },
                    singleLine = true,
                    isError = state.codeError != null,
                    supportingText = state.codeError?.let { { Text(it) } },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 8.sp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (canVerify) vm.submitCode(code) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    modifier = Modifier.weight(1f).semantics { contentType = ContentType.SmsOtpCode },
                )
                Button(
                    onClick = { vm.submitCode(code) },
                    enabled = canVerify,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp).height(56.dp),
                ) {
                    if (state.submittingCode) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = AppColors.OnAccent, strokeWidth = 2.dp)
                    } else {
                        Text("Verify")
                    }
                }
            }
        }
    }
}
