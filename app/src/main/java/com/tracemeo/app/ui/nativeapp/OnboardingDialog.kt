package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.LiquidGlassSettings

@Composable
fun WelcomeOnboardingDialog(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    isFirstLaunch: Boolean,
    onDismiss: () -> Unit,
    onComplete: (userName: String, currencySymbol: String) -> Unit
) {
    var name by remember(settings.userName) { mutableStateOf(settings.userName) }
    var symbol by remember(settings.currencySymbol) { mutableStateOf(settings.currencySymbol) }

    val currencyLabels = mapOf(
        "₹" to "₹ INR",
        "$" to "$ USD",
        "€" to "€ EUR",
        "£" to "£ GBP"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = {
            Column {
                Text(
                    text = if (isFirstLaunch) "Welcome to Tracmio" else "Profile & Currency",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
                Text(
                    text = "100% Offline SQLCipher Finance Vault",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Personalize your vault display name and primary currency symbol. Everything stays encrypted on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Primary Currency",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    LiquidGlassSettings.ALLOWED_CURRENCIES.forEach { sym ->
                        FilterChip(
                            selected = symbol == sym,
                            onClick = { symbol = sym },
                            label = { Text(currencyLabels[sym] ?: sym) },
                            modifier = Modifier.testTag("onboarding_currency_$sym")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onComplete(name.trim(), symbol) },
                modifier = Modifier.testTag("onboarding_save_btn")
            ) {
                Text(if (isFirstLaunch) "Start Using Tracmio" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFirstLaunch) "Skip" else "Close")
            }
        }
    )
}
