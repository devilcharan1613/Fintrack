package com.charan.fintrack

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.charan.fintrack.ui.nativeapp.NativeFinTrackActivity
import org.junit.Rule
import org.junit.Test

class NativeFinTrackNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<NativeFinTrackActivity>()

    @Test
    fun nativeDashboardIsRendered() {
        compose.onNodeWithText("FinTrack").fetchSemanticsNode()
        compose.onNodeWithText("TOTAL BALANCE").assertExists()
    }

    @Test
    fun bottomNavigationOpensAccountsAndReports() {
        compose.onNodeWithText("Accounts").performClick()
        compose.onNodeWithText("ACCOUNTS & WALLETS").assertExists()
        compose.onNodeWithText("Reports").performClick()
        compose.onNodeWithText("INCOME VS EXPENSE").assertExists()
    }



    @Test
    fun settingsExposesOfflineReminderBackgroundBackupAndUpdateControls() {
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Settings & Appearance").performClick()

        compose.onNodeWithText("Daily reminders").assertExists()
        compose.onNodeWithText("Custom background image").assertExists()
        compose.onNodeWithText("Export full JSON backup").assertExists()
        compose.onNodeWithText("Select update APK").assertExists()
    }
}
