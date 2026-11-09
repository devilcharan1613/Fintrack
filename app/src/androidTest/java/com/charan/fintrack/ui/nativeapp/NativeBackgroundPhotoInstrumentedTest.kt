package com.charan.fintrack.ui.nativeapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeBackgroundPhotoInstrumentedTest {
    @Test
    fun selectedPhotoIsCopiedIntoPrivateStorageAndCanBeLoadedAfterPickerGrantEnds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.cacheDir, "fintrack-background-source.png")
        val sample = Bitmap.createBitmap(96, 64, Bitmap.Config.ARGB_8888)
        try {
            FileOutputStream(source).use { output ->
                assertTrue(sample.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            sample.recycle()
        }

        val stored = persistBackgroundImage(context, Uri.fromFile(source))
        assertTrue(stored.exists())
        assertTrue(stored.absolutePath.startsWith(context.filesDir.absolutePath))
        val restored = BitmapFactory.decodeFile(stored.absolutePath)
        assertNotNull(restored)
        assertEquals(96, restored!!.width)
        assertEquals(64, restored.height)
        restored.recycle()
    }
}
