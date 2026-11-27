package cl.acis.app.connected

import android.content.Context
import android.database.sqlite.*
import androidx.core.database.sqlite.transaction
import cl.acis.app.data.*
import cl.acis.app.data.remote.*
import cl.acis.app.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.security.MessageDigest
import java.util.UUID

internal fun hash(file:File):String {
    val digest=MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { stream ->
        val bytes=ByteArray(65536)
        while(true) { val read=stream.read(bytes); if(read<0) break; digest.update(bytes,0,read) }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

class SyncConflict(val orderId:Long):Exception("La orden cambió en el servidor")

class FieldStore(private val context:Context,val session:Session,val network:Network, private val schedule:(Context,String)->Unit=FieldWorker.Companion::schedule):OrderRepository {
    private val api=network.field
    private val json=Gson()
    private val partition=UUID.nameUUIDFromBytes((session.profile.id+"|"+session.profile.role+"|"+session.profile.scope).toByteArray()).toString()
    private val directory=File(context.filesDir,"cuentas/"+partition).also { it.mkdirs() }
    private val helper=object:SQLiteOpenHelper(context,"campo-"+partition+".db",null,1){
        override fun onCreate(db:SQLiteDatabase){
            db.execSQL("create table visits(id integer primary key,revision integer not null,payload text not null,server_key text not null)")
            db.execSQL("create table pending(seq integer primary key autoincrement,id integer not null,body text not null)")
        }
        override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int)=Unit
    }
    private val db get()=helper.writableDatabase
    private companion object { val locks=java.util.concurrent.ConcurrentHashMap<String,Mutex>() }
    private val lock=locks.computeIfAbsent(partition){Mutex()}
    private suspend fun <T> serial(action:suspend ()->T):T=withContext(Dispatchers.IO){lock.withLock { action() }}
    override suspend fun all():List<Order> = serial {
        db.rawQuery("select payload from visits order by id desc",null).use { c -> buildList { while(c.moveToNext()) add(json.fromJson(c.getString(0),Order::class.java)) } }
    }
    override suspend fun savedSnapshot(order:Order,id:Long):Order=all().first {it.id==id}
    suspend fun pending():Int=serial { count("select count(*) from pending") }
    private fun count(sql:String)=db.rawQuery(sql,null).use {it.moveToFirst();it.getInt(0)}
    private fun store(o:Order,rev:Long,key:String) {
        db.execSQL("insert or replace into visits values(?,?,?,?)",arrayOf(o.id,rev,json.toJson(o),key))
    }
    private suspend fun cache(v:Visit) {
        val id=v.order.id
        val paths=v.photos.map { digest ->
            require(digest.matches(Regex("[a-f0-9]{64}")))
            val file=File(directory,digest)
            if(!file.exists()) {
                val tmp=File.createTempFile("download-",null,directory)
                try {
                    api.download(id,digest).use { body->tmp.outputStream().use { body.byteStream().copyTo(it) } }
                    check(hash(tmp)==digest && tmp.renameTo(file))
                } finally { tmp.delete() }
            }
            file.absolutePath
        }
        val p=v.plan
        val o=v.order.toRemoteOrder(id).order.copy(siteId=p.site,technicianId=p.technician,scheduled=p.scheduled,
            latitude=p.latitude?.toString().orEmpty(),longitude=p.longitude?.toString().orEmpty(),slaHours=p.slaHours,created=v.created,serverRevision=v.revision).withPhotos(paths)
        store(o,v.revision,requireNotNull(v.order.clientOrderId))
    }
    override suspend fun save(order:Order):Long=serial {
        val plan=Plan(requireNotNull(order.siteId),requireNotNull(order.technicianId),order.scheduled,order.latitude.toDoubleOrNull(),order.longitude.toDoubleOrNull(),order.slaHours)
        val operation=UUID.randomUUID().toString()
        if(order.id==0L){
            val key=order.creationKey.ifBlank {operation}
            val v=api.create(Write(key,0,plan,order.toCreateRequestDto(key),emptyList()))
            cache(v)
            return@serial v.order.id
        }
        val meta=db.rawQuery("select revision,server_key from visits where id=?",arrayOf(order.id.toString())).use {check(it.moveToFirst());it.getLong(0) to it.getString(1)}
        check(meta.first==order.serverRevision) {"La orden cambió. Vuelve a abrirla antes de guardar."}
        val photos=order.evidencePhotos.map { path ->
            val file=File(path); require(file.length() in 1L..16L*1024*1024)
            val digest=hash(file); val target=File(directory,digest)
            if(!target.exists()) { val tmp=File.createTempFile("photo-",null,directory);try {file.copyTo(tmp,true);check(tmp.renameTo(target))} finally {tmp.delete()} }
            digest
        }
        val request=Write(operation,meta.first,plan,order.toUpdateRequestDto(meta.second),photos)
        if(session.profile.role!="Técnico") { cache(api.update(order.id,request));return@serial order.id }
        db.transaction {
            store(order.withPhotos(photos.map {File(directory,it).absolutePath}).copy(serverRevision=meta.first+1),meta.first+1,meta.second)
            execSQL("insert into pending(id,body) values(?,?)",arrayOf(order.id,json.toJson(request)))
        }
        schedule(context,session.profile.id)
        order.id
    }
    override suspend fun delete(id:Long)=serial {
        val rev=db.rawQuery("select revision from visits where id=? and not exists(select 1 from pending where id=?)",arrayOf(id.toString(),id.toString())).use {check(it.moveToFirst());it.getLong(0)}
        api.delete(id,rev)
        db.delete("visits","id=?",arrayOf(id.toString()))
        Unit
    }
    suspend fun sync():Unit=serial {
        val conflicts=mutableSetOf<Long>()
        while(true) {
            val excluded=if(conflicts.isEmpty()) "" else " where id not in ("+conflicts.joinToString(",")+")"
            val item=db.rawQuery("select seq,id,body from pending"+excluded+" order by seq limit 1",null).use {
                if(it.moveToFirst()) Triple(it.getLong(0),it.getLong(1),json.fromJson(it.getString(2),Write::class.java)) else null
            } ?: break
            val (seq,id,w)=item
            try {
            if(session.profile.role=="Técnico") w.photos.forEach { digest ->
                api.upload(id,digest,File(directory,digest).asRequestBody("application/octet-stream".toMediaType()))
            }
            api.update(id,w)
            db.delete("pending","seq=?",arrayOf(seq.toString()))
            } catch(e:HttpException){
                if(e.code() !in listOf(400,403,404,409)) throw e
                conflicts.add(id)
            }
        }
        val remote=api.orders()
        remote.filterNot {it.order.id in conflicts}.forEach { cache(it) }
        val allowed=remote.map { it.order.id }.toSet()
        val ids=db.rawQuery("select id from visits",null).use {c->buildList {while(c.moveToNext()) add(c.getLong(0))}}
        (ids-allowed-conflicts).forEach {db.delete("visits","id=?",arrayOf(it.toString()))}
        discardUnusedPhotos()
        conflicts.firstOrNull()?.let {throw SyncConflict(it)}
        Unit
    }
    private fun discardUnusedPhotos() {
        val used=mutableSetOf<String>()
        db.rawQuery("select payload from visits",null).use { c->while(c.moveToNext()) gsonOrder(c.getString(0)).evidencePhotos.forEach {used.add(File(it).name)} }
        db.rawQuery("select body from pending",null).use {c->while(c.moveToNext()) used.addAll(json.fromJson(c.getString(0),Write::class.java).photos)}
        directory.listFiles()?.filter {it.name.matches(Regex("[a-f0-9]{64}")) && it.name !in used}?.forEach {it.delete()}
    }
    private fun gsonOrder(raw:String):Order=json.fromJson(raw,Order::class.java)
    suspend fun exportAndResolve(id:Long,uri:android.net.Uri)=serial {
        val saved=db.rawQuery("select payload from visits where id=?",arrayOf(id.toString())).use {check(it.moveToFirst());it.getString(0)}
        val changes=db.rawQuery("select body from pending where id=? order by seq",arrayOf(id.toString())).use {c->buildList {while(c.moveToNext()) add(json.fromJson(c.getString(0),Write::class.java))}}
        check(changes.isNotEmpty())
        val remote=api.orders().firstOrNull {it.order.id==id}
        java.util.zip.ZipOutputStream(checkNotNull(context.contentResolver.openOutputStream(uri))).use {zip->
            zip.putNextEntry(java.util.zip.ZipEntry("orden.json"));zip.write(saved.toByteArray());zip.closeEntry()
            zip.putNextEntry(java.util.zip.ZipEntry("cambios.json"));zip.write(json.toJson(changes).toByteArray());zip.closeEntry()
            changes.flatMap {it.photos}.distinct().forEach {photo->zip.putNextEntry(java.util.zip.ZipEntry("fotos/"+photo));File(directory,photo).inputStream().use {it.copyTo(zip)};zip.closeEntry()}
        }
        if(remote!=null) cache(remote)
        db.transaction {
            if(remote==null) delete("visits","id=?",arrayOf(id.toString()))
            delete("pending","id=?",arrayOf(id.toString()))
        }
    }
    val catalogs=object:CatalogRepository {
        override suspend fun read():Catalog {
            val rows=api.catalog()
            return Catalog(rows.filter {it.kind=="technicians"}.map {Technician(it.id,it.name,it.email,it.active)},
                rows.filter {it.kind=="sites"}.map {Site(it.id,it.name,it.address,it.client,it.active)})
        }
        private suspend fun save(e:Entry) {
            try { api.entry(e.id,e) } catch(ex:HttpException) { if(ex.code()==409) throw CatalogConflictException(if(e.kind=="sites") "name" else "email");throw ex }
        }
        override suspend fun saveSite(value:Site)=save(Entry(value.id.ifBlank {UUID.randomUUID().toString()},"sites",value.name,value.address,value.client,active=value.active))
        override suspend fun saveTechnician(value:Technician)=save(Entry(value.id.ifBlank {UUID.randomUUID().toString()},"technicians",value.name,email=value.email,active=value.active))
    }
}
