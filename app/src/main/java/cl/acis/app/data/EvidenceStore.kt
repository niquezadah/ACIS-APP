package cl.acis.app.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Copia las imágenes elegidas a almacenamiento privado para conservarlas sin conexión. */
class EvidenceStore(context: Context) {
    private val appContext = context.applicationContext
    suspend fun importPhotos(uris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        val directory = File(appContext.filesDir, "evidencias")
        if (!directory.exists() && !directory.mkdirs()) return@withContext emptyList()
        uris.mapNotNull { uri ->
            var target: File? = null
            try {
                val file = File.createTempFile("evidencia_", ".img", directory)
                target = file
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("Imagen no disponible")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Imagen no válida")
                file.absolutePath
            } catch (_: IOException) {
                target?.delete()
                null
            } catch (_: SecurityException) {
                target?.delete()
                null
            }
        }
    }

    suspend fun discard(paths: List<String>) = withContext(Dispatchers.IO) {
        val directory = File(appContext.filesDir, "evidencias").canonicalFile
        paths.forEach { path ->
            val file = File(path).canonicalFile
            if (file.parentFile == directory) file.delete()
        }
    }
}
