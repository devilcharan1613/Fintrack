package com.charan.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF071521)
private val Card = Color(0xFF102535)
private val Green = Color(0xFF19D99A)
private val Muted = Color(0xFF9CB2C1)
private val White = Color(0xFFF4FAFC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FinTrackApp() }
    }
}

private enum class Screen(val label: String) {
    Home("Home"), Accounts("Accounts"), Transactions("Transactions"), Reports("Reports"), More("More")
}

@Composable
private fun FinTrackApp() {
    var onboarded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(Screen.Home) }
    var showAdd by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Green, background = Ink, surface = Card, onPrimary = Ink,
        onBackground = White, onSurface = White
    )) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            if (!onboarded) WelcomeScreen { onboarded = true }
            else {
                Scaffold(
                    containerColor = Ink,
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF0B1D2A)) {
                            Screen.values().forEach { screen ->
                                val icon = when (screen) {
                                    Screen.Home -> Icons.Default.Home
                                    Screen.Accounts -> Icons.Default.AccountBalanceWallet
                                    Screen.Transactions -> Icons.Default.SwapHoriz
                                    Screen.Reports -> Icons.Default.BarChart
                                    Screen.More -> Icons.Default.MoreHoriz
                                }
                                NavigationBarItem(
                                    selected = selected == screen,
                                    onClick = { selected = screen },
                                    icon = { Icon(icon, contentDescription = screen.label) },
                                    label = { Text(screen.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Ink, selectedTextColor = Green,
                                        indicatorColor = Green, unselectedIconColor = Muted,
                                        unselectedTextColor = Muted
                                    )
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAdd = true },
                            containerColor = Green, contentColor = Ink) {
                            Icon(Icons.Default.Add, contentDescription = "Add transaction")
                        }
                    }
                ) { padding ->
                    AnimatedContent(
                        targetState = selected,
                        modifier = Modifier.padding(padding),
                        transitionSpec = {
                            (fadeIn(tween(180)) + slideInHorizontally { it / 12 }) togetherWith
                                (fadeOut(tween(120)) + slideOutHorizontally { -it / 12 })
                        },
                        label = "screen-transition"
                    ) { target ->
                        when (target) {
                            Screen.Home -> DashboardScreen()
                            Screen.Accounts -> PlaceholderScreen("Accounts", "Account management starts in Milestone 2.")
                            Screen.Transactions -> PlaceholderScreen("Transactions", "The transaction engine starts in Milestone 3.")
                            Screen.Reports -> PlaceholderScreen("Reports", "Reports and charts will arrive in a later milestone.")
                            Screen.More -> PlaceholderScreen("More", "Settings and backup will be added in later milestones.")
                        }
                    }
                }
                if (showAdd) AlertDialog(
                    onDismissRequest = { showAdd = false },
                    title = { Text("Quick add") },
                    text = { Text("The transaction form will be implemented in Milestone 3.") },
                    confirmButton = { TextButton(onClick = { showAdd = false }) { Text("Got it") } }
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(onStart: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        "Track your money" to "See your finances clearly, all in one private place.",
        "Plan with confidence" to "Prepare for budgets, goals and smarter money habits.",
        "Your data stays yours" to "FinTrack Free starts offline. No account or sign-in required."
    )
    val progress by animateFloatAsState((page + 1) / 3f, tween(350), label = "progress")
    Column(Modifier.fillMaxSize().padding(26.dp),
        verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onStart) { Text("Skip", color = Muted) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(112.dp).clip(RoundedCornerShape(32.dp)).background(Green),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ShowChart, null, tint = Ink, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(32.dp))
            Text("FinTrack", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = White)
            Text("FREE", color = Green, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Spacer(Modifier.height(40.dp))
            Text(pages[page].first, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = White)
            Spacer(Modifier.height(12.dp))
            Text(pages[page].second, color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(30.dp))
            LinearProgressIndicator(progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = Green, trackColor = Card)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(Modifier.size(if (i == page) 22.dp else 7.dp, 7.dp)
                        .clip(CircleShape).background(if (i == page) Green else Muted.copy(alpha = .4f)))
                }
            }
        }
        Button(onClick = { if (page < 2) page++ else onStart() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Ink),
            shape = RoundedCornerShape(18.dp)) {
            Text(if (page < 2) "Continue" else "Get started", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, null)
        }
    }
}

@Composable
private fun DashboardScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Good day", color = Muted, fontSize = 14.sp)
                Text("Your money overview", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.NotificationsNone, null, tint = White, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF123D43), Card))).padding(22.dp)) {
            Text("TOTAL BALANCE", color = Muted, fontSize = 12.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(10.dp))
            Text("₹0.00", color = White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("Add an account to get started", color = Green, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryChip("Income", "₹0.00", Icons.Default.TrendingUp, Green, Modifier.weight(1f))
                SummaryChip("Expenses", "₹0.00", Icons.Default.TrendingDown, Color(0xFFFF7C85), Modifier.weight(1f))
            }
        }
        Text("Quick actions", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Add income", Icons.Default.AddCircle, Modifier.weight(1f))
            QuickAction("Add expense", Icons.Default.RemoveCircle, Modifier.weight(1f))
            QuickAction("Transfer", Icons.Default.SwapHoriz, Modifier.weight(1f))
        }
        SectionCard("Get started", "Your dashboard is ready. Add your first account and transaction to see your finances here.", Icons.Default.Flag)
        SectionCard("Monthly budget", "Create a budget to keep your spending on track.", Icons.Default.PieChart)
        SectionCard("Recent transactions", "Your latest activity will appear here after you add transactions.", Icons.Default.ReceiptLong)
        Spacer(Modifier.height(4.dp))
        Text("FinTrack Free • Offline-first • No sign-in", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SummaryChip(title: String, amount: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B1E2A)).padding(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(amount, color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Card).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Green, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionCard(title: String, description: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).padding(17.dp),
        verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Green, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(13.dp))
        Column {
            Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(5.dp))
            Text(description, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Construction, null, tint = Green, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(message, color = Muted, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(18.dp))
        Text("Milestone 1 foundation screen", color = Green, fontSize = 12.sp)
    }
}
