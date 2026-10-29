package cl.acis.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoLoaderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val red = Color.RED
    private val green = Color.GREEN
    private val blue = Color.BLUE
    private val yellow = Color.YELLOW

    private fun fixture(width: Int = 80, height: Int = 40): File {
        val file = File.createTempFile("orientation_test_", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        listOf(red, green, blue, yellow).forEachIndexed { i, color ->
            paint.color = color
            val x = (i % 2) * width / 2f
            val y = (i / 2) * height / 2f
            canvas.drawRect(x, y, x + width / 2f, y + height / 2f, paint)
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
        return file
    }

    @Test fun respectsAllEightExifOrientationsWithoutChangingOriginal() {
        val expected = listOf(
            listOf(red, green, blue, yellow), listOf(green, red, yellow, blue),
            listOf(yellow, blue, green, red), listOf(blue, yellow, red, green),
            listOf(red, blue, green, yellow), listOf(blue, red, yellow, green),
            listOf(yellow, green, blue, red), listOf(green, yellow, red, blue)
        )
        for (orientation in 1..8) {
            val file = fixture()
            try {
                ExifInterface(file.path).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    saveAttributes()
                }
                val original = file.readBytes()
                val decoded = requireNotNull(loadEvidencePhoto(file.path))
                try {
                    assertEquals("width for EXIF $orientation", if (orientation >= 5) 40 else 80, decoded.width)
                    assertEquals("height for EXIF $orientation", if (orientation >= 5) 80 else 40, decoded.height)
                    expected[orientation - 1].forEachIndexed { index, color ->
                        val actual = decoded.getPixel(
                            decoded.width * (if (index % 2 == 0) 1 else 3) / 4,
                            decoded.height * (if (index < 2) 1 else 3) / 4
                        )
                        listOf(Color.red(color) - Color.red(actual), Color.green(color) - Color.green(actual), Color.blue(color) - Color.blue(actual)).forEach {
                            assertTrue("corner $index for EXIF $orientation", kotlin.math.abs(it) < 40)
                        }
                    }
                    assertArrayEquals(original, file.readBytes())
                } finally { decoded.recycle() }
            } finally { file.delete() }
        }
    }

    @Test fun photoWithoutOrientationKeepsItsDimensions() {
        val file = fixture()
        try {
            val decoded = requireNotNull(loadEvidencePhoto(file.path))
            assertEquals(80, decoded.width)
            assertEquals(40, decoded.height)
            decoded.recycle()
        } finally { file.delete() }
    }

    @Test fun largePhotoIsReducedForDisplay() {
        val file = fixture(3200, 1600)
        try {
            val decoded = requireNotNull(loadEvidencePhoto(file.path))
            assertTrue(decoded.width <= 1600 && decoded.height <= 1600)
            decoded.recycle()
        } finally { file.delete() }
    }

    @Test fun unavailableOrDamagedPhotoDoesNotCrash() {
        assertNull(loadEvidencePhoto(""))
        assertNull(loadEvidencePhoto(File(context.cacheDir, "missing-evidence.jpg").path))
        val file = File.createTempFile("damaged_", ".jpg", context.cacheDir)
        try {
            file.writeText("invalid photo")
            assertNull(loadEvidencePhoto(file.path))
        } finally { file.delete() }
    }
}
