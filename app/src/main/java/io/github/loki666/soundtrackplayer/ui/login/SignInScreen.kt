package io.github.loki666.soundtrackplayer.ui.login

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.loki666.soundtrackplayer.steam.AuthState
import io.github.loki666.soundtrackplayer.ui.theme.AppColors

@Composable
fun SignInScreen() {
    val vm: AuthViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val busy = state is AuthState.SigningIn
    val error = (state as? AuthState.SignedOut)?.error
    val canSubmit = username.isNotBlank() && password.isNotEmpty() && !busy
    val submit = { if (canSubmit) vm.signIn(username, password) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Box(
            Modifier.size(56.dp).background(AppColors.Accent, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = AppColors.OnAccent, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Your soundtracks,\nin your pocket.", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Sign in with your Steam account to see the albums you own.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.TextSecondary,
        )

        Spacer(Modifier.height(40.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Steam account name") },
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            enabled = !busy,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() }),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = submit,
            enabled = canSubmit,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(22.dp), color = AppColors.OnAccent, strokeWidth = 2.5.dp)
            } else {
                Text("Sign in", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.weight(1f).height(40.dp))
        Surface(color = AppColors.Surface, shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp))
                Text(
                    "Your password is encrypted and sent straight to Steam. It is never stored on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = AppColors.Surface,
    focusedContainerColor = AppColors.Surface,
    disabledContainerColor = AppColors.Surface,
    unfocusedBorderColor = AppColors.Outline,
    focusedBorderColor = AppColors.Accent,
)
