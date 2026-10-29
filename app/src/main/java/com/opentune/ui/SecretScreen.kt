package com.opentune.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.opentune.data.AppLock
import kotlinx.coroutines.launch

/** The personal edition's first screen on a new phone: the Secret, and an arrow. */
@Composable
fun SecretScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var secret by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var wrong by remember { mutableStateOf(false) }

    fun submit() {
        if (secret.isBlank() || checking) return
        checking = true
        scope.launch {
            wrong = !AppLock.unlock(context, secret)
            checking = false
        }
    }

    Column(
        Modifier.fillMaxSize().imePadding().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("OpenTune", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it; wrong = false },
                label = { Text("Secret") },
                singleLine = true,
                isError = wrong,
                supportingText = if (wrong) ({ Text("That's not it") }) else null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(12.dp))
            FilledIconButton(onClick = ::submit, enabled = secret.isNotBlank() && !checking, modifier = Modifier.size(52.dp)) {
                if (checking) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Unlock")
            }
        }
    }
}
