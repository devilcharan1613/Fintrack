package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracemeo.app.data.CashCountLine
import com.tracemeo.app.data.LiquidGlassSettings
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeIntelligenceCalculators
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.SubscriptionItem
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.security.AppLockState
import java.util.UUID
import kotlin.math.abs

fun displayAmount(amount: Double, settings: LiquidGlassSettings, decoySeed: Double = 12450.0): String {
    if (settings.stealthModeActive) {
        val decoy = if (amount == 0.0) 0.0 else 25.0 + ((abs(amount.toLong()) * 31L + decoySeed.toLong()) % 156L).toDouble()
        val signed = if (amount < 0) -decoy else decoy
        return MoneyPaise.formatCurrency(signed, masked = false, symbol = settings.currencySymbol)
    }
    return MoneyPaise.formatCurrency(amount, masked = settings.privacyMaskEnabled, symbol = settings.currencySymbol)
}

fun evalSimpleMath(expr: String): Double? {
    return NativeLedgerCalculator.evaluateInlineMath(expr)
}

@Composable
fun AppLockOverlayScreen(
    lockState: AppLockState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    hostCallbacks: NativeHostCallbacks,
    onVerifyPin: (String) -> Boolean,
    onBiometricUnlock: () -> Unit,
    onRecoverWithSecret: (String, String?) -> Boolean
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showRecovery by remember { mutableStateOf(false) }
    var recoverySecret by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    val securityCapability = remember { hostCallbacks.getDeviceSecurityCapability() }

    LaunchedEffect(lockState.biometricEnabled) {
        if (lockState.biometricEnabled) {
            hostCallbacks.authenticateBiometric(
                onSuccess = onBiometricUnlock,
                onFailure = { /* Stay on PIN pad */ }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background.copy(alpha = 0.96f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassBadge3D(emoji = "🛡️", accent = palette.primary, sizeDp = 54.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (showRecovery) "Recover Tracmio Vault" else "Tracmio Vault Locked",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (showRecovery) {
                        lockState.securityQuestion.ifBlank { "Enter your Security Answer or Recovery Key (TRAC-XXXX-XXXX)" }
                    } else {
                        "Enter your ${lockState.pinLength}-digit Liquid Glass PIN"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))

                if (!showRecovery) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(lockState.pinLength) { idx ->
                            val filled = idx < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (filled) palette.primary else Color.Transparent)
                                    .border(1.5.dp, if (filled) palette.primary else palette.textSecondary, CircleShape)
                            )
                        }
                    }
                    errorText?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(18.dp))

                    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { digit ->
                                Surface(
                                    shape = CircleShape,
                                    color = palette.surfaceElevated.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            errorText = null
                                            if (enteredPin.length < lockState.pinLength) {
                                                val next = enteredPin + digit
                                                enteredPin = next
                                                if (next.length == lockState.pinLength) {
                                                    if (!onVerifyPin(next)) {
                                                        errorText = "Incorrect PIN. Try again."
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("pin_key_$digit")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(digit, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (lockState.biometricEnabled) {
                            IconButton(
                                onClick = { hostCallbacks.authenticateBiometric(onBiometricUnlock) { errorText = it } },
                                modifier = Modifier.size(64.dp).testTag("biometric_unlock_btn")
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = "Biometric Unlock", tint = palette.primary, modifier = Modifier.size(32.dp))
                            }
                        } else {
                            Spacer(Modifier.size(64.dp))
                        }
                        Surface(
                            shape = CircleShape,
                            color = palette.surfaceElevated.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable {
                                    errorText = null
                                    if (enteredPin.length < lockState.pinLength) {
                                        val next = enteredPin + "0"
                                        enteredPin = next
                                        if (next.length == lockState.pinLength) {
                                            if (!onVerifyPin(next)) {
                                                errorText = "Incorrect PIN"
                                                enteredPin = ""
                                            }
                                        }
                                    }
                                }
                                .testTag("pin_key_0")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("0", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                            }
                        }
                        IconButton(
                            onClick = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) },
                            modifier = Modifier.size(64.dp).testTag("pin_backspace")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = palette.textSecondary)
                        }
                    }

                    if (securityCapability.deviceCredentialReady) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                hostCallbacks.authenticateDeviceCredential(
                                    onSuccess = onBiometricUnlock,
                                    onFailure = { errorText = it }
                                )
                            },
                            modifier = Modifier.testTag("device_credential_fallback_btn")
                        ) {
                            Text("Use Device PIN / Pattern", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { showRecovery = true; errorText = null }) {
                        Text("Forgot PIN? Use Security Answer or Recovery Key", color = palette.primary)
                    }
                } else {
                    OutlinedTextField(
                        value = recoverySecret,
                        onValueChange = { recoverySecret = it },
                        label = { Text("Security Answer or Recovery Key (TRAC-...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("Optional New 4 or 6-Digit PIN (blank = unlock & disable)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorText?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showRecovery = false }, modifier = Modifier.weight(1f)) {
                            Text("Back to PIN")
                        }
                        Button(
                            onClick = {
                                val pinArg = newPin.takeIf { it.length == 4 || it.length == 6 }
                                if (newPin.isNotEmpty() && pinArg == null) {
                                    errorText = "New PIN must be 4 or 6 digits"
                                } else if (!onRecoverWithSecret(recoverySecret, pinArg)) {
                                    errorText = "Answer or Recovery Key did not match"
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("recover_unlock_btn")
                        ) {
                            Text("Verify & Unlock")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockSetupScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinLen by remember { mutableIntStateOf(state.appLockState.pinLength) }
    var bioEnabled by remember { mutableStateOf(state.appLockState.biometricEnabled) }
    var secQuestion by remember { mutableStateOf(state.appLockState.securityQuestion.ifBlank { "What was your first school or childhood nickname?" }) }
    var secAnswer by remember { mutableStateOf("") }
    var generatedKey by remember { mutableStateOf<String?>(null) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    var verifyDisablePin by remember { mutableStateOf("") }
    var verifyDisableError by remember { mutableStateOf<String?>(null) }
    val securityCapability = remember { hostCallbacks.getDeviceSecurityCapability() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Text("🛡️ App Lock & Recovery", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (state.appLockState.isEnabled) "App Lock Active (${state.appLockState.pinLength}-digit)" else "App Lock Disabled",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                    }
                    if (state.appLockState.isEnabled) {
                        OutlinedButton(onClick = { viewModel.lockNow() }, modifier = Modifier.testTag("lock_now_btn")) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Lock Now")
                        }
                    }
                }
                Text(
                    "PIN + salted SHA-256 hash stored inside Android Keystore EncryptedSharedPreferences. Offline recovery uses your mandatory Security Question or Recovery Key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        if (generatedKey != null) {
            GlassCard(settings = settings, borderColor = palette.primary, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save Your Recovery Key Now", style = MaterialTheme.typography.titleMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                    Text(generatedKey!!, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                    Text("Also export a Full JSON Backup right now so you never lose access to your vault.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { hostCallbacks.copyToClipboard(generatedKey!!) }, modifier = Modifier.weight(1f)) {
                            Text("Copy Key")
                        }
                        Button(onClick = { hostCallbacks.exportPlainJsonBackup("tracmio_v16.13_backup.json") }, modifier = Modifier.weight(1f)) {
                            Text("Export JSON")
                        }
                    }
                }
            }
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configure PIN & Mandatory Recovery", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = pinLen == 4, onClick = { pinLen = 4; pin = ""; confirmPin = "" }, label = { Text("4-Digit PIN") })
                    FilterChip(selected = pinLen == 6, onClick = { pinLen = 6; pin = ""; confirmPin = "" }, label = { Text("6-Digit PIN") })
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= pinLen && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("Enter $pinLen-digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("setup_pin_input")
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= pinLen && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Allow Biometric / Device PIN Launch Unlock", color = palette.textPrimary)
                        Switch(checked = bioEnabled, onCheckedChange = {
                            bioEnabled = it
                            if (state.appLockState.isEnabled) viewModel.updateBiometricOptIn(it)
                        })
                    }
                    Text(
                        securityCapability.summaryLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary
                    )
                }
                OutlinedTextField(
                    value = secQuestion,
                    onValueChange = { secQuestion = it },
                    label = { Text("Mandatory Security Question") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secAnswer,
                    onValueChange = { secAnswer = it },
                    label = { Text("Security Answer (for forgot-PIN reset)") },
                    modifier = Modifier.fillMaxWidth().testTag("setup_sec_answer")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (pin.length != pinLen || pin != confirmPin) {
                                viewModel.showStatus("PINs must match and be $pinLen digits")
                                return@Button
                            }
                            viewModel.enableAppLock(pin, bioEnabled, secQuestion, secAnswer)
                                .onSuccess { generatedKey = it; pin = ""; confirmPin = ""; secAnswer = "" }
                                .onFailure { viewModel.showStatus(it.message ?: "Fill security answer") }
                        },
                        modifier = Modifier.weight(1f).testTag("enable_applock_btn")
                    ) {
                        Text(if (state.appLockState.isEnabled) "Update Lock" else "Enable App Lock")
                    }
                    if (state.appLockState.isEnabled) {
                        OutlinedButton(
                            onClick = {
                                verifyDisablePin = ""
                                verifyDisableError = null
                                showDisableConfirm = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            modifier = Modifier.testTag("disable_applock_btn")
                        ) {
                            Text("Disable")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }

    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            containerColor = palette.surface,
            title = { Text("Verify Current PIN to Disable", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your current ${state.appLockState.pinLength}-digit PIN to turn off App Lock.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    OutlinedTextField(
                        value = verifyDisablePin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) verifyDisablePin = it },
                        label = { Text("Current PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_disable_pin_input")
                    )
                    verifyDisableError?.let {
                        Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    onClick = {
                        if (viewModel.verifyPin(verifyDisablePin)) {
                            viewModel.disableAppLock()
                            generatedKey = null
                            showDisableConfirm = false
                        } else {
                            verifyDisableError = "Incorrect PIN"
                        }
                    }
                ) { Text("Verify & Disable") }
            },
            dismissButton = { TextButton(onClick = { showDisableConfirm = false }) { Text("Cancel") } }
        )
    }
}

/**
 * 3-Step Liquid Glass Cash Denomination Counter (Notes ₹500–₹10 & Coins ₹20–₹1 + Note).
 */
@Composable
fun CashDenominationDialog(
    accounts: List<AccountEntity>,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onApplyReconcile: (List<CashCountLine>, String, AccountEntity?) -> Unit
) {
    val notes = listOf(500, 200, 100, 50, 20, 10)
    val coins = listOf(20, 10, 5, 2, 1)

    var selectedType by remember { mutableStateOf("NOTE") }
    var selectedDenom by remember { mutableIntStateOf(500) }
    var stepCount by remember { mutableIntStateOf(1) }
    val lines = remember { mutableStateListOf<CashCountLine>().apply { addAll(settings.cashCountsList) } }
    var noteText by remember { mutableStateOf(settings.cashNote) }
    val cashAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    var selectedAccount by remember(cashAccounts) {
        mutableStateOf(cashAccounts.firstOrNull { it.accountType.equals("Cash", true) } ?: cashAccounts.firstOrNull())
    }
    var accMenuExpanded by remember { mutableStateOf(false) }

    val countedTotal = NativeLedgerCalculator.computeCashDenominationTotal(lines)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💵 Cash Counter (Notes & Coins)", color = palette.textPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step 1: Pick Note or Coin Denomination
                Text("Step 1 · Pick Note or Coin", style = MaterialTheme.typography.labelLarge, color = palette.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    notes.forEach { v ->
                        val sel = selectedType == "NOTE" && selectedDenom == v
                        FilterChip(
                            selected = sel,
                            onClick = { selectedType = "NOTE"; selectedDenom = v },
                            label = { Text("₹$v Note") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    coins.forEach { v ->
                        val sel = selectedType == "COIN" && selectedDenom == v
                        FilterChip(
                            selected = sel,
                            onClick = { selectedType = "COIN"; selectedDenom = v },
                            label = { Text("₹$v Coin") }
                        )
                    }
                }

                // Step 2: Select Count (- / +) & Tap "+ Add"
                Text("Step 2 · Count for ₹$selectedDenom ${if (selectedType == "NOTE") "Note" else "Coin"}", style = MaterialTheme.typography.labelLarge, color = palette.primary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { if (stepCount > 1) stepCount-- },
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(palette.surfaceElevated)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease count", tint = palette.textPrimary)
                        }
                        Text("$stepCount", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { stepCount++ },
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(palette.surfaceElevated)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase count", tint = palette.textPrimary)
                        }
                    }
                    Button(
                        onClick = {
                            val idx = lines.indexOfFirst { it.denomType == selectedType && it.value == selectedDenom }
                            if (idx >= 0) {
                                val old = lines[idx]
                                lines[idx] = old.copy(count = old.count + stepCount)
                            } else {
                                lines.add(CashCountLine(selectedType, selectedDenom, stepCount))
                            }
                            stepCount = 1
                        },
                        modifier = Modifier.testTag("cash_count_add_btn")
                    ) {
                        Text("+ Add ₹${selectedDenom * stepCount}")
                    }
                }

                // Step 3: Added Rows, Live Total, Optional Note & Apply
                Text("Step 3 · Added Cash & Reconcile", style = MaterialTheme.typography.labelLarge, color = palette.primary)
                if (lines.isEmpty()) {
                    Text("No denominations added yet.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                } else {
                    lines.forEachIndexed { idx, line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceElevated.copy(alpha = 0.7f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "₹${line.value} ${if (line.denomType == "NOTE") "Note" else "Coin"} × ${line.count}",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    MoneyPaise.formatCurrency((line.value * line.count).toDouble()),
                                    color = palette.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { lines.removeAt(idx) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove row", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.primary.copy(alpha = 0.16f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Counted Total", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        MoneyPaise.formatCurrency(countedTotal),
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box {
                    OutlinedButton(onClick = { accMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Wallet: ${selectedAccount?.name ?: "Select Account"}")
                    }
                    DropdownMenu(expanded = accMenuExpanded, onDismissRequest = { accMenuExpanded = false }) {
                        cashAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.accountType})") },
                                onClick = { selectedAccount = acc; accMenuExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Optional Note (e.g. Desk drawer cash)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyReconcile(lines.toList(), noteText, selectedAccount) },
                enabled = lines.isNotEmpty(),
                modifier = Modifier.testTag("apply_reconcile_cash_btn")
            ) {
                Text("Apply & Reconcile Cash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun TransferFundsDialog(
    accounts: List<AccountEntity>,
    initialFromId: Long? = null,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onTransfer: (Long, Long, Double, String) -> Unit
) {
    val active = accounts.filter { !it.isArchived }
    var fromAcc by remember(initialFromId, active) {
        mutableStateOf(active.firstOrNull { it.id == initialFromId } ?: active.firstOrNull())
    }
    var toAcc by remember(initialFromId, active) {
        mutableStateOf(active.firstOrNull { it.id != (initialFromId ?: active.firstOrNull()?.id) } ?: active.firstOrNull())
    }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var expandFrom by remember { mutableStateOf(false) }
    var expandTo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("🔁 Transfer Between Accounts", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedButton(onClick = { expandFrom = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("From: ${fromAcc?.name ?: "Select"}")
                    }
                    DropdownMenu(expanded = expandFrom, onDismissRequest = { expandFrom = false }) {
                        active.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { fromAcc = a; expandFrom = false }) }
                    }
                }
                Box {
                    OutlinedButton(onClick = { expandTo = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("To: ${toAcc?.name ?: "Select"}")
                    }
                    DropdownMenu(expanded = expandTo, onDismissRequest = { expandTo = false }) {
                        active.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { toAcc = a; expandTo = false }) }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (${MoneyPaise.activeCurrencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = fromAcc?.id ?: return@Button
                    val t = toAcc?.id ?: return@Button
                    val amt = amountText.toDoubleOrNull() ?: return@Button
                    if (f != t && amt > 0) onTransfer(f, t, amt, note)
                },
                modifier = Modifier.testTag("confirm_transfer_btn")
            ) {
                Text("Transfer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FinancialXRayScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    val report = remember(
        state.accounts, state.transactions, state.budgets, state.goals,
        state.loans, state.loanPayments, state.subscriptions, state.academicEvents, settings.currencySymbol
    ) {
        NativeIntelligenceCalculators.computeFinancialXRay(
            accounts = state.accounts,
            transactions = state.transactions,
            budgets = state.budgets,
            goals = state.goals,
            loans = state.loans,
            loanPayments = state.loanPayments,
            subscriptions = state.subscriptions,
            academicEvents = state.academicEvents,
            currencySymbol = settings.currencySymbol
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }
                Text("🩻 Financial X-Ray", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            }
            OutlinedButton(
                onClick = {
                    val lines = buildList {
                        add("TRACMIO FINANCIAL X-RAY DIAGNOSTIC" to true)
                        add("Health Score: ${report.overallScore}/100 (${report.gradeLabel})" to true)
                        add("Net Worth: ${MoneyPaise.formatCurrency(report.netWorth)} | Liquid: ${MoneyPaise.formatCurrency(report.liquid)} | Debt: ${MoneyPaise.formatCurrency(report.totalDebt)}" to false)
                        add("10 VITALS BREAKDOWN" to true)
                        report.metrics.forEach { m -> add("• ${m.label}: ${m.valueText} (${m.scorePct}/100) — ${m.hint}" to false) }
                        add("SMART OBSERVATIONS" to true)
                        report.observations.forEach { obs -> add("• $obs" to false) }
                    }
                    hostCallbacks.printCustomReport("Tracmio_Financial_XRay", "Tracmio Financial X-Ray", lines)
                },
                modifier = Modifier.testTag("export_xray_pdf_btn")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export PDF")
            }
        }

        // Animated 0-100 Health Score Ring + Net Worth Hero
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = palette.surfaceBorder,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 16f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = if (report.overallScore >= 70) palette.primary else if (report.overallScore >= 45) Color(0xFFF59E0B) else Color(0xFFEF4444),
                            startAngle = -90f,
                            sweepAngle = (report.overallScore / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 16f, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${report.overallScore}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                        Text("/ 100", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(report.gradeLabel, style = MaterialTheme.typography.titleMedium, color = palette.primary, fontWeight = FontWeight.ExtraBold)
                    Text("Net Worth: ${displayAmount(report.netWorth, settings)}", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Liquid Cash: ${displayAmount(report.liquid, settings)}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Text("Total Debt: ${displayAmount(report.totalDebt, settings)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF43F5E))
                }
            }
        }

        // 10-Metric 2-Column Diagnostic Grid
        Text("10 Financial Vitals", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
        report.metrics.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { metric ->
                    GlassCard(settings = settings, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(metric.label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(metric.valueText, style = MaterialTheme.typography.titleMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(
                                progress = { (metric.scorePct / 100f).coerceIn(0f, 1f) },
                                color = if (metric.scorePct >= 65) palette.primary else Color(0xFFF59E0B),
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp))
                            )
                            Text(metric.hint, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 2)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // Smart Observations
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Smart Diagnostic Observations", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                report.observations.forEach { obs ->
                    Text("• $obs", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    onBack: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var editingSub by remember { mutableStateOf<SubscriptionItem?>(null) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("Monthly") }
    var nextDate by remember { mutableStateOf("2026-12-01") }
    var subCat by remember { mutableStateOf("Bills") }

    val monthlyBurn = state.subscriptions.sumOf {
        if (it.billingCycle.equals("Yearly", true)) it.amount / 12.0 else it.amount
    }
    val incomePct = if (state.totalIncome > 0) ((monthlyBurn / state.totalIncome) * 100.0).coerceIn(0.0, 100.0) else 0.0
    val defaultAccountId = state.accounts.firstOrNull { !it.isArchived }?.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }
                Text("🔄 Subscriptions", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            }
            Button(
                onClick = {
                    editingSub = null
                    name = ""; amount = ""; cycle = "Monthly"; nextDate = "2026-12-01"; subCat = "Bills"
                    showAdd = true
                },
                modifier = Modifier.testTag("add_subscription_btn")
            ) {
                Text("+ Add")
            }
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ESTIMATED MONTHLY BURN", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                Text(displayAmount(monthlyBurn, settings), style = MaterialTheme.typography.headlineMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                Text(
                    "Annualized: ${displayAmount(monthlyBurn * 12.0, settings)} · ${incomePct.toInt()}% of recorded income",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
            }
        }

        if (state.subscriptions.isEmpty()) {
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Text("No recurring subscriptions tracked yet. Add Netflix, iCloud, gym, or domain renewals.", modifier = Modifier.padding(14.dp), color = palette.textSecondary)
            }
        }

        state.subscriptions.forEach { sub ->
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                            Text("${sub.billingCycle} · ${sub.category} · Next: ${sub.nextBillingDate}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        Text(displayAmount(sub.amount, settings), fontWeight = FontWeight.Bold, color = palette.primary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        if (defaultAccountId != null) {
                            OutlinedButton(
                                onClick = { viewModel.payAndLogSubscription(sub, defaultAccountId) },
                                modifier = Modifier.testTag("pay_sub_${sub.id}")
                            ) {
                                Text("Pay & Log")
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        IconButton(onClick = {
                            editingSub = sub
                            name = sub.name
                            amount = MoneyPaise.formatInput(MoneyPaise.toPaise(sub.amount))
                            cycle = sub.billingCycle
                            nextDate = sub.nextBillingDate
                            subCat = sub.category
                            showAdd = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit subscription", tint = palette.secondary)
                        }
                        IconButton(onClick = { viewModel.deleteSubscription(sub.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete subscription", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false; editingSub = null },
            containerColor = palette.surface,
            title = { Text(if (editingSub == null) "Add Recurring Subscription" else "Edit Subscription", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Service Name") })
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = cycle == "Monthly", onClick = { cycle = "Monthly" }, label = { Text("Monthly") })
                        FilterChip(selected = cycle == "Yearly", onClick = { cycle = "Yearly" }, label = { Text("Yearly") })
                    }
                    OutlinedTextField(value = subCat, onValueChange = { subCat = it }, label = { Text("Category") })
                    OutlinedTextField(value = nextDate, onValueChange = { nextDate = it }, label = { Text("Next Billing (YYYY-MM-DD)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    if (name.isNotBlank() && amt > 0) {
                        val existing = editingSub
                        if (existing == null) {
                            viewModel.addSubscription(
                                SubscriptionItem(
                                    id = "sub_${UUID.randomUUID().toString().take(8)}",
                                    name = name.trim(),
                                    amount = amt,
                                    billingCycle = cycle,
                                    nextBillingDate = nextDate.trim(),
                                    category = subCat.trim().ifBlank { "Bills" }
                                )
                            )
                        } else {
                            viewModel.updateSubscription(
                                existing.copy(
                                    name = name.trim(),
                                    amount = amt,
                                    billingCycle = cycle,
                                    nextBillingDate = nextDate.trim(),
                                    category = subCat.trim().ifBlank { "Bills" }
                                )
                            )
                        }
                        name = ""; amount = ""; editingSub = null; showAdd = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false; editingSub = null }) { Text("Cancel") } }
        )
    }
}
