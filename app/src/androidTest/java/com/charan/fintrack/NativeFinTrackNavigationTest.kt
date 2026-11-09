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
        compose.onNodeWithText("TOTAL BALANCE").fetchSemanticsNode()
    }

    @Test
    fun bottomNavigationOpensAccountsAndReports() {
        compose.onNodeWithText("Accounts").performClick()
        compose.onNodeWithText("ACCOUNTS & WALLETS").fetchSemanticsNode()
        compose.onNodeWithText("Reports").performClick()
        compose.onNodeWithText("INCOME VS EXPENSE").fetchSemanticsNode()
    }



    @Test
    fun settingsExposesOfflineReminderBackgroundBackupAndUpdateControls() {
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Settings & Appearance").performClick()

        compose.onNodeWithText("Daily reminders").fetchSemanticsNode()
        compose.onNodeWithText("Custom background image").fetchSemanticsNode()
        compose.onNodeWithText("Export full JSON backup").fetchSemanticsNode()
        compose.onNodeWithText("Select update APK").fetchSemanticsNode()
    }
}
