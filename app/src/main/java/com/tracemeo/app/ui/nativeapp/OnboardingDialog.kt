package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingDialog(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onComplete: (userName: String, currencySymbol: String) -> Unit
) {
    var name by remember { mutableStateOf(settings.userName.ifBlank { "Charan" }) }
    var selectedCurrency by remember { mutableStateOf(settings.currencySymbol.ifBlank { "₹" }) }

    AlertDialog(
        onDismissRequest = {},
        containerColor = palette.surfaceElevated,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassBadge3D("📈", palette.primary, 40.dp)
                    Column {
                        Row {
                            Text("Trac", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                            Text("mio", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                        }
                        Text("FINANCE & EXPENSES TRACKER · v16.13", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "100% Offline, Encrypted Personal Finance Vault with Aurora Teal Liquid Glass UI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Select Primary Currency", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassSettings.ALLOWED_CURRENCIES.forEach { curr ->
                        val isSelected = selectedCurrency == curr
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) palette.primary.copy(alpha = 0.25f) else palette.surface)
                                .border(1.2.dp, if (isSelected) palette.primary else palette.surfaceBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedCurrency = curr }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(curr, fontWeight = FontWeight.ExtraBold, color = if (isSelected) palette.primary else palette.textPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onComplete(name.trim().ifBlank { "Charan" }, selectedCurrency) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enter Tracmio Vault →", color = palette.background, fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}
