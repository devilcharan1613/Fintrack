package com.charan.fintrack

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun settingsExposesOfflineReminderBackgroundBackupResetAndUpdateControls() {
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Settings & Appearance").performClick()

        compose.onNodeWithText("Daily reminders").fetchSemanticsNode()
        compose.onNodeWithText("Custom background image").fetchSemanticsNode()
        compose.onNodeWithText("Background visibility").fetchSemanticsNode()
        compose.onNodeWithText("Navigation and action icon size").fetchSemanticsNode()
        compose.onNodeWithText("Choose photo").fetchSemanticsNode()
        compose.onNodeWithText("Export full JSON backup").fetchSemanticsNode()
        compose.onNodeWithText("Select update APK").fetchSemanticsNode()
        compose.onNodeWithText("Reset all finance data").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Reset all finance data?").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Reset all finance data?").fetchSemanticsNode()
        compose.onNodeWithText("Cancel").performClick()
    }

    @Test
    fun accountAndCategoryEditorsExposeTheirFullMetadataFields() {
        compose.onNodeWithText("Accounts").performClick()
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("Bank / Institution (optional)").fetchSemanticsNode()
        compose.onNodeWithText("Account reference (optional)").fetchSemanticsNode()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Categories").performClick()
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("Subcategories (one per line)").fetchSemanticsNode()
    }
}
