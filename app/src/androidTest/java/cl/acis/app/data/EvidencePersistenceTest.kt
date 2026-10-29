package cl.acis.app.data

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.acis.app.model.Order
import cl.acis.app.ui.loadEvidencePhoto
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvidencePersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun tenPhotosSurviveReopeningAndCanBeRemoved() = runBlocking {
        val name = "test_${UUID.randomUUID()}.db"
        var repo = LocalOrderRepository(context, name)
        try {
            val photos = (1..10).map { "foto_$it.jpg" }
            val id = repo.save(Order(site="Prueba").withPhotos(photos))
            repo.close()
            repo = LocalOrderRepository(context, name)
            val loaded = repo.all().single()
            assertEquals(id, loaded.id)
            assertEquals(photos, loaded.evidencePhotos)
            repo.save(loaded.withPhotos(photos.drop(1)))
            assertEquals(photos.drop(1), repo.all().single().evidencePhotos)
            repo.save(loaded.withPhotos(emptyList()))
            assertTrue(repo.all().single().evidencePhotos.isEmpty())
        } finally { repo.close(); context.deleteDatabase(name) }
    }

    @Test fun legacyJsonWithoutPhotoListRemainsReadable() = runBlocking {
        val name = "test_${UUID.randomUUID()}.db"
        val repo = LocalOrderRepository(context, name)
        try {
            repo.save(Order(photo="foto_antigua.jpg"))
            val payload = repo.readableDatabase.rawQuery("SELECT payload FROM orders", null).use {
                it.moveToFirst(); JSONObject(it.getString(0)).apply { remove("photos") }
            }
            repo.writableDatabase.update("orders", ContentValues().apply { put("payload", payload.toString()) }, null, null)
            assertEquals(listOf("foto_antigua.jpg"), repo.all().single().evidencePhotos)
        } finally { repo.close(); context.deleteDatabase(name) }
    }

    @Test fun repositoryRejectsElevenPhotos() = runBlocking {
        val name = "test_${UUID.randomUUID()}.db"
        val repo = LocalOrderRepository(context, name)
        try {
            try {
                repo.save(Order().withPhotos((1..11).map { "foto_$it.jpg" }))
                fail("No debe guardar más de diez fotos")
            } catch (_: IllegalArgumentException) { }
            assertTrue(repo.all().isEmpty())
        } finally { repo.close(); context.deleteDatabase(name) }
    }

    @Test fun importedImageIsIndependentOfOriginalAndInvalidFileIsSkipped() = runBlocking {
        val source = File.createTempFile("import_test_", ".jpg", context.cacheDir)
        val invalid = File.createTempFile("invalid_test_", ".jpg", context.cacheDir)
        val store = EvidenceStore(context)
        var imported = emptyList<String>()
        try {
            val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.BLUE)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            bitmap.recycle()
            invalid.writeText("not an image")
            val original = source.readBytes()
            imported = store.importPhotos(listOf(Uri.fromFile(source), Uri.fromFile(invalid)))
            assertEquals(1, imported.size)
            assertArrayEquals(original, File(imported.single()).readBytes())
            source.delete()
            val decoded = requireNotNull(loadEvidencePhoto(imported.single()))
            assertEquals(80, decoded.width)
            assertEquals(120, decoded.height)
            decoded.recycle()
        } finally { source.delete(); invalid.delete(); store.discard(imported) }
    }
}
