package com.charan.tracmio.ui.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.charan.tracmio.data.local.TracmioDatabase
import com.charan.tracmio.data.repository.FinanceRepository

class NativeTracmioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = TracmioDatabase.getDatabase(applicationContext)
        val repository = FinanceRepository(database)
        val preferencesStore = TracmioPreferencesStore(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            NativeTracmioViewModel.factory(repository, preferencesStore)
        )[NativeTracmioViewModel::class.java]

        setContent {
            NativeTracmioApp(viewModel = viewModel)
        }
    }
}
