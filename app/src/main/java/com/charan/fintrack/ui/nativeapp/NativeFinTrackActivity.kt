package com.charan.fintrack.ui.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider

/**
 * Native Compose application host. MainActivity acts as the one-time migration
 * bootstrap, then routes future app launches here after Room import succeeds.
 */
class NativeFinTrackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NativeFinTrackViewModel::class.java]
        val quickAction = intent?.getStringExtra(com.charan.fintrack.MainActivity.EXTRA_QUICK_ACTION)
        setContent { NativeFinTrackApp(viewModel, quickAction) }
    }
}
