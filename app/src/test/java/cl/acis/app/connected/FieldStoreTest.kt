package cl.acis.app.connected

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import cl.acis.app.data.remote.OrderResponseDto
import cl.acis.app.model.*
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class FieldStoreTest {
    private val gson=Gson()
    private val ctx=ApplicationProvider.getApplicationContext<Context>()
    private val site=UUID.randomUUID().toString()
    private val technician=UUID.randomUUID().toString()
    private val plan=Plan(site,technician,"2026-12-01",-33.45,-70.65,24)
    private val planOrder=Order(id=3,siteId=site,technicianId=technician,site="Centro",address="Av. Prueba 111",
        equipment="Unidad 4",technician="Ana",problem="Revisión de filtro",scheduled=plan.scheduled)
    private val key=UUID.randomUUID().toString()
    private fun visit(revision:Long,stage:Stage=Stage.ASSIGNED,photos:List<String> = emptyList())=Visit(
        OrderResponseDto(3,key,"Centro","Av. Prueba 111","Unidad 4","Ana","Normal","Preventiva","Revisión de filtro",
            stage,"","","","",0,0,"","",false,photos.size),plan,revision,1,photos)
    private fun reply(server:MockWebServer,data:Any)=server.enqueue(MockResponse().setBody(gson.toJson(data)))
    private fun session(role:String)=Session("token",Long.MAX_VALUE,Profile(UUID.randomUUID().toString(),"campo@example.invalid","Prueba",role,technician))
    @Test fun visitSurvivesRestartRetriesAndDownloadsPhotos()=runBlocking {
        MockWebServer().use { server ->
            val api=Network("token",server.url("/").toString(),server.url("/").toString())
            val user=session("Técnico")
            val store=FieldStore(ctx,user,api){_,_->}
            reply(server,listOf(visit(1)))
            store.sync()
            val accepted=store.all().single().copy(stage=Stage.ACCEPTED)
            store.save(accepted)
            assertEquals(1,store.pending())
            val reopened=FieldStore(ctx,user,api){_,_->}
            assertEquals(Stage.ACCEPTED,reopened.all().single().stage)
            server.enqueue(MockResponse().setResponseCode(503))
            assertTrue(runCatching {reopened.sync()}.isFailure)
            assertEquals(1,reopened.pending())
            reply(server,visit(2,Stage.ACCEPTED))
            reply(server,listOf(visit(2,Stage.ACCEPTED)))
            reopened.sync()
            assertEquals(0,reopened.pending())
            assertTrue(runCatching {reopened.save(accepted)}.isFailure)
            val file=File.createTempFile("campo","jpg",ctx.cacheDir).apply {writeText("foto de prueba")}
            val digest=hash(file)
            val active=visit(3,Stage.ACTIVE)
            reply(server,listOf(active));reopened.sync()
            val withPhoto=reopened.all().single().withPhotos(listOf(file.absolutePath))
            reopened.save(withPhoto);file.delete()
            server.enqueue(MockResponse().setResponseCode(204))
            reply(server,visit(4,Stage.ACTIVE,listOf(digest)))
            reply(server,listOf(visit(4,Stage.ACTIVE,listOf(digest))))
            reopened.sync()
            assertEquals("foto de prueba",File(reopened.all().single().photos.single()).readText())
            val client=FieldStore(ctx,session("Cliente"),api){_,_->}
            reply(server,listOf(visit(4,Stage.DONE,listOf(digest))))
            server.enqueue(MockResponse().setBody("foto de prueba"))
            client.sync()
            assertEquals(digest,hash(File(client.all().single().photos.single())))
            reply(server,emptyList<Visit>());client.sync()
            assertTrue(client.all().isEmpty())
        }
    }
    @Test fun conflictKeepsTheDraftUntilAnExportSucceeds()=runBlocking {
        MockWebServer().use { server ->
            val store=FieldStore(ctx,session("Técnico"),Network("token",server.url("/").toString(),server.url("/").toString())){_,_->}
            reply(server,listOf(visit(3,Stage.ACTIVE)));store.sync()
            store.save(store.all().single().copy(diagnosis="Diagnóstico sin señal"))
            server.enqueue(MockResponse().setResponseCode(409))
            reply(server,listOf(visit(4,Stage.ACTIVE)))
            assertTrue(runCatching {store.sync()}.exceptionOrNull() is SyncConflict)
            assertEquals(1,store.pending())
            assertEquals("Diagnóstico sin señal",store.all().single().diagnosis)
            val zip=File.createTempFile("respaldo",".zip",ctx.cacheDir)
            reply(server,listOf(visit(4,Stage.ACTIVE)))
            store.exportAndResolve(3,android.net.Uri.fromFile(zip))
            java.util.zip.ZipFile(zip).use { backup ->
                assertTrue(backup.getInputStream(backup.getEntry("orden.json")).bufferedReader().readText().contains("Diagnóstico sin señal"))
            }
            assertEquals(0,store.pending())
            assertEquals("",store.all().single().diagnosis)
        }
    }
    @Test fun managerWritesReadsAndRemovesRemoteRecords()=runBlocking {
        MockWebServer().use { server ->
            val store=FieldStore(ctx,session("Jefatura"),Network("token",server.url("/").toString(),server.url("/").toString())){_,_->}
            val id=UUID.randomUUID().toString()
            repeat(2){server.enqueue(MockResponse().setResponseCode(204))}
            store.catalogs.saveSite(Site(id,"Centro","Av. Prueba 111","Empresa"))
            store.catalogs.saveTechnician(Technician(technician,"Ana","ana@example.invalid"))
            reply(server,listOf(Entry(id,"sites","Centro","Av. Prueba 111","Empresa"), Entry(technician,"technicians","Ana",email="ana@example.invalid")))
            val catalog=store.catalogs.read()
            assertEquals("Centro",catalog.sites.single().name)
            assertEquals("Ana",catalog.technicians.single().name)
            server.enqueue(MockResponse().setResponseCode(409))
            assertTrue(runCatching {store.catalogs.saveSite(catalog.sites.single())}.exceptionOrNull() is CatalogConflictException)
            reply(server,visit(1))
            assertEquals(3L,store.save(planOrder.copy(id=0,creationKey=key)))
            assertEquals(1L,store.savedSnapshot(planOrder,3).serverRevision)
            reply(server,visit(2))
            store.save(store.all().single().copy(problem="Cambio de equipo"))
            server.enqueue(MockResponse().setResponseCode(204))
            store.delete(3)
            assertTrue(store.all().isEmpty())
        }
    }
}
