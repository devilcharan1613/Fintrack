package com.charan.fintrack

import androidx.compose.ui.test.assertExists
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
        compose.onNodeWithText("FinTrack").assertExists()
        compose.onNodeWithText("TOTAL BALANCE").assertExists()
    }

    @Test
    fun bottomNavigationOpensAccountsAndReports() {
        compose.onNodeWithText("Accounts").performClick()
        compose.onNodeWithText("Accounts and wallets").assertExists()
        compose.onNodeWithText("Reports").performClick()
        compose.onNodeWithText("INCOME VS EXPENSE").assertExists()
    }
}
