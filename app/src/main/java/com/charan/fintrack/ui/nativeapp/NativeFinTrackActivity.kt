package com.charan.fintrack.ui.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider

/**
 * Internal native-screen host for development and automated testing.
 * It is intentionally not the launcher until legacy migration and parity gates pass.
 */
class NativeFinTrackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NativeFinTrackViewModel::class.java]
        setContent { NativeFinTrackApp(viewModel) }
    }
}
