package com.charan.fintrack.ui.nativeapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
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
        val source = createPhotoUri(context, "fintrack-background-source", 96, 64, Color.RED)

        val storedPath = persistBackgroundImage(context, source)
        val stored = Uri.parse(storedPath)
        assertEquals("file", stored.scheme)
        assertTrue(FileForUri(stored).exists())
        assertTrue(FileForUri(stored).absolutePath.startsWith(context.filesDir.absolutePath))

        // Remove the source media item to prove the saved background no longer relies on
        // the picker/media-provider URI or its temporary read permission.
        assertEquals(1, context.contentResolver.delete(source, null, null))
        val restored = BitmapFactory.decodeFile(stored.path)
        assertNotNull(restored)
        assertEquals(96, restored!!.width)
        assertEquals(64, restored.height)
        restored.recycle()
        Unit
    }

    @Test
    fun replacingPhotoOverwritesPrivateFileWithNewImageContent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firstSource = createPhotoUri(context, "fintrack-background-first", 96, 64, Color.RED)
        val secondSource = createPhotoUri(context, "fintrack-background-second", 128, 80, Color.BLUE)

        try {
            val firstUri = persistBackgroundImage(context, firstSource)
            val secondUri = persistBackgroundImage(context, secondSource)

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
            context.contentResolver.delete(firstSource, null, null)
            context.contentResolver.delete(secondSource, null, null)
        }
    }

    @Test
    fun failedPhotoReplacementKeepsPreviouslySavedBackground() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val validSource = createPhotoUri(context, "fintrack-background-valid-before-failure", 96, 64, Color.RED)
        val missingSource = createPhotoUri(context, "fintrack-background-missing-source", 64, 48, Color.BLUE)
        context.contentResolver.delete(missingSource, null, null)

        try {
            val savedPath = persistBackgroundImage(context, validSource)
            val failedReplacement = runCatching {
                persistBackgroundImage(context, missingSource)
            }
            assertTrue("A missing source image must fail", failedReplacement.isFailure)

            val saved = BitmapFactory.decodeFile(Uri.parse(savedPath).path!!)
            assertNotNull("The previous background must remain readable after a failed replacement", saved)
            val pixel = saved!!.getPixel(saved.width / 2, saved.height / 2)
            assertTrue("The previous red background must not be removed", Color.red(pixel) > Color.blue(pixel))
            saved.recycle()
        } finally {
            context.contentResolver.delete(validSource, null, null)
            context.contentResolver.delete(missingSource, null, null)
        }
    }

    @Test
    fun concurrentPhotoSelectionsDoNotLeavePartialOrCorruptFiles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val redSource = createPhotoUri(context, "fintrack-background-concurrent-red", 96, 64, Color.RED)
        val blueSource = createPhotoUri(context, "fintrack-background-concurrent-blue", 128, 80, Color.BLUE)

        try {
            val redSave = async(Dispatchers.IO) { persistBackgroundImage(context, redSource) }
            val blueSave = async(Dispatchers.IO) { persistBackgroundImage(context, blueSource) }
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
            context.contentResolver.delete(redSource, null, null)
            context.contentResolver.delete(blueSource, null, null)
        }
    }

    /**
     * Creates a real content:// image URI, matching the kind of URI returned by Android's
     * photo picker and media providers. The row belongs to this test app and is removed by
     * each test after its assertions complete.
     */
    private fun createPhotoUri(
        context: Context,
        displayName: String,
        width: Int,
        height: Int,
        color: Int
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName-${UUID.randomUUID()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FinTrackTests")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create an instrumentation photo in MediaStore")

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(color)
            val output = resolver.openOutputStream(uri, "w")
                ?: error("Could not open the instrumentation photo for writing")
            output.use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                    "Could not encode the instrumentation photo"
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            }
            return uri
        } catch (failure: Throwable) {
            resolver.delete(uri, null, null)
            throw failure
        } finally {
            bitmap.recycle()
        }
    }

    private fun FileForUri(uri: Uri): java.io.File = java.io.File(uri.path!!)
}
