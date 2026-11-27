package cl.acis.app.data

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.acis.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

interface OrderRepository {
 suspend fun all(): List<Order>
 suspend fun read(): OrderReadResult = OrderReadResult(all())
 suspend fun save(order: Order): Long
 suspend fun delete(id: Long)
}
// El repositorio permite reemplazar el almacenamiento sin cambiar las pantallas.
class LocalOrderRepository(context: Context, databaseName: String = "acis.db"): SQLiteOpenHelper(context,databaseName,null,2), OrderRepository {
 override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, payload TEXT NOT NULL)"); createCatalogTables(db) }
 override fun onUpgrade(db: SQLiteDatabase,old: Int,new: Int) { check(old == 1 && new == 2) { "Se requiere una migración de datos" }; createCatalogTables(db) }
 override suspend fun all(): List<Order> = read().orders
 override suspend fun read(): OrderReadResult = withContext(Dispatchers.IO) {
  readableDatabase.rawQuery("SELECT id,payload FROM orders ORDER BY id DESC",null).use { cursor ->
   readOrderRows(sequence {
    while(cursor.moveToNext()) yield(cursor.getLong(0) to cursor.getString(1))
   })
  }
 }
 override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
  check(writableDatabase.delete("orders","id=?",arrayOf(id.toString()))==1) {"Orden no disponible"}
 }
 override suspend fun save(order: Order): Long = withContext(Dispatchers.IO) {
  require(Rules.photoLimit(order).isEmpty()) { "Máximo 10 fotografías" }
  val db=writableDatabase
  db.beginTransaction()
  try {
  if(order.id==0L && (order.siteId!=null || order.technicianId!=null)) {
   val catalog=readCatalog(db)
   require(CatalogRules.selection(order,catalog).isEmpty()) { "Selecciona registros activos" }
   require(CatalogRules.snapshot(order,catalog)==order) { "Los datos cambiaron. Vuelve a seleccionarlos." }
  }
  val j=JSONObject().apply { with(order) {
   put("siteId",siteId ?: JSONObject.NULL);put("technicianId",technicianId ?: JSONObject.NULL);put("siteClient",siteClient)
   put("customerConfirmed",customerConfirmed)
   put("site",site);put("address",address);put("equipment",equipment);put("technician",technician);put("priority",priority);put("type",type);put("problem",problem);put("stage",stage.name);put("diagnosis",diagnosis);put("work",work);put("materials",materials);put("photo",evidencePhotos.firstOrNull().orEmpty());put("photos",JSONArray(evidencePhotos));put("location",location);put("arrival",arrival);put("departure",departure);put("customer",customer);put("feedback",feedback)
  } }
  val values=ContentValues().apply { put("payload",j.toString()) }
  val id=if(order.id==0L) db.insertOrThrow("orders",null,values) else { check(db.update("orders",values,"id=?",arrayOf(order.id.toString()))==1);order.id }
  db.setTransactionSuccessful()
  id
  } finally { db.endTransaction() }
 }
}
