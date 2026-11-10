package com.charan.fintrack.ui.nativeapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeBackgroundPhotoInstrumentedTest {
    @Test
    fun selectedPhotoIsCopiedIntoPrivateStorageAndCanBeLoadedAfterPickerGrantEnds() = runBlocking {
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

        val storedPath = persistBackgroundImage(context, Uri.fromFile(source))
        val stored = Uri.parse(storedPath)
        assertEquals("file", stored.scheme)
        assertTrue(File(stored.path!!).exists())
        assertTrue(File(stored.path!!).absolutePath.startsWith(context.filesDir.absolutePath))
        val restored = BitmapFactory.decodeFile(stored.path)
        assertNotNull(restored)
        assertEquals(96, restored!!.width)
        assertEquals(64, restored.height)
        restored.recycle()
        source.delete()
    }

    @Test
    fun replacingPhotoOverwritesPrivateFileWithNewImageContent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firstSource = File(context.cacheDir, "fintrack-background-first.png")
        val secondSource = File(context.cacheDir, "fintrack-background-second.png")
        writeSolidImage(firstSource, 96, 64, Color.RED)
        writeSolidImage(secondSource, 128, 80, Color.BLUE)

        try {
            val firstUri = persistBackgroundImage(context, Uri.fromFile(firstSource))
            val secondUri = persistBackgroundImage(context, Uri.fromFile(secondSource))

            assertEquals("The private storage path is stable by design", firstUri, secondUri)
            val storedUri = Uri.parse(secondUri)
            val replaced = BitmapFactory.decodeFile(storedUri.path!!)
            assertNotNull(replaced)
            assertEquals(128, replaced!!.width)
            assertEquals(80, replaced.height)
            val pixel = replaced.getPixel(replaced.width / 2, replaced.height / 2)
            assertTrue("The replacement image must be blue, not the previous red photo", Color.blue(pixel) > Color.red(pixel))
            replaced.recycle()
        } finally {
            firstSource.delete()
            secondSource.delete()
        }
    }


    @Test
    fun concurrentPhotoSelectionsDoNotLeavePartialOrCorruptFiles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val redSource = File(context.cacheDir, "fintrack-background-concurrent-red.png")
        val blueSource = File(context.cacheDir, "fintrack-background-concurrent-blue.png")
        writeSolidImage(redSource, 96, 64, Color.RED)
        writeSolidImage(blueSource, 128, 80, Color.BLUE)

        try {
            val redSave = async(Dispatchers.IO) { persistBackgroundImage(context, Uri.fromFile(redSource)) }
            val blueSave = async(Dispatchers.IO) { persistBackgroundImage(context, Uri.fromFile(blueSource)) }
            val savedPaths = listOf(redSave.await(), blueSave.await())
            assertEquals("Both selections use the stable preference path", 1, savedPaths.distinct().size)

            val stored = Uri.parse(savedPaths.last())
            val bitmap = BitmapFactory.decodeFile(stored.path!!)
            assertNotNull("The final selected photo must remain readable", bitmap)
            assertTrue(bitmap!!.width == 96 || bitmap.width == 128)
            assertTrue(bitmap.height == 64 || bitmap.height == 80)
            bitmap.recycle()
            assertTrue(
                "No partial photo files should remain",
                context.filesDir.listFiles()?.none { it.name.startsWith("fintrack_background-") && it.name.endsWith(".tmp") } != false
            )
        } finally {
            redSource.delete()
            blueSource.delete()
        }
    }

    private fun writeSolidImage(file: File, width: Int, height: Int, color: Int) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(color)
            FileOutputStream(file).use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
