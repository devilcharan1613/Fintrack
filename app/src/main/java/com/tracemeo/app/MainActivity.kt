package com.tracemeo.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.tracemeo.app.data.PreferencesAndThemeStore
import com.tracemeo.app.ui.nativeapp.NativeHostCallbacksImpl
import com.tracemeo.app.ui.nativeapp.NativeTracmioApp
import com.tracemeo.app.ui.nativeapp.NativeTracmioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: NativeTracmioViewModel by viewModels()

    private var pendingCreateDocCallback: ((Uri) -> Unit)? = null
    private var pendingOpenDocCallback: ((Uri) -> Unit)? = null
    private var pendingPickMediaCallback: ((Uri) -> Unit)? = null
    private var pendingTakePictureCallback: ((Boolean) -> Unit)? = null
    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) pendingCreateDocCallback?.invoke(uri)
        pendingCreateDocCallback = null
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingOpenDocCallback?.invoke(uri)
        pendingOpenDocCallback = null
    }

    private val pickVisualMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingPickMediaCallback?.invoke(uri)
        pendingPickMediaCallback = null
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        pendingTakePictureCallback?.invoke(success)
        pendingTakePictureCallback = null
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingPermissionCallback?.invoke(granted)
        pendingPermissionCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationQuickAction(intent)

        lifecycleScope.launch(Dispatchers.IO) {
            TracmioReminderScheduler.ensureChannel(applicationContext)
            val prefs = PreferencesAndThemeStore(applicationContext)
            if (prefs.isDailyReminderEnabled()) {
                val (hour, minute) = prefs.getDailyReminderTime()
                val settings = prefs.getInitialSettings()
                TracmioReminderScheduler.scheduleDailyReminder(applicationContext, hour, minute, settings.userName)
            }
        }

        val hostCallbacks = NativeHostCallbacksImpl(
            activity = this,
            viewModel = viewModel,
            onLaunchCreateDoc = { _, suggestedName, callback ->
                pendingCreateDocCallback = callback
                createDocumentLauncher.launch(suggestedName)
            },
            onLaunchOpenDoc = { mimeTypes, callback ->
                pendingOpenDocCallback = callback
                openDocumentLauncher.launch(mimeTypes)
            },
            onLaunchPickVisualMedia = { callback ->
                pendingPickMediaCallback = callback
                pickVisualMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onLaunchTakePicture = { outputUri, callback ->
                pendingTakePictureCallback = callback
                takePictureLauncher.launch(outputUri)
            },
            onRequestPermission = { permission, callback ->
                pendingPermissionCallback = callback
                requestPermissionLauncher.launch(permission)
            }
        )

        setContent {
            NativeTracmioApp(
                viewModel = viewModel,
                hostCallbacks = hostCallbacks
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationQuickAction(intent)
    }

    private fun handleNotificationQuickAction(intent: Intent?) {
        val action = intent?.getStringExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION) ?: return
        viewModel.triggerNotificationAction(action)
        intent.removeExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION)
    }
}
