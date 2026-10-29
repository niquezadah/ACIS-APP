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

interface OrderRepository { suspend fun all(): List<Order>; suspend fun save(order: Order): Long }
// El repositorio permite reemplazar el almacenamiento sin cambiar las pantallas.
class LocalOrderRepository(context: Context, databaseName: String = "acis.db"): SQLiteOpenHelper(context,databaseName,null,1), OrderRepository {
 override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, payload TEXT NOT NULL)") }
 override fun onUpgrade(db: SQLiteDatabase,old: Int,new: Int) { error("Se requiere una migración de datos") }
 override suspend fun all(): List<Order> = withContext(Dispatchers.IO) {
  readableDatabase.rawQuery("SELECT id,payload FROM orders ORDER BY id DESC",null).use { c -> buildList {
   while(c.moveToNext()) { val j=JSONObject(c.getString(1)); add(Order(c.getLong(0),j.getString("site"),j.getString("address"),j.getString("equipment"),j.getString("technician"),j.getString("priority"),j.getString("type"),j.getString("problem"),Stage.valueOf(j.getString("stage")),j.getString("diagnosis"),j.getString("work"),j.getString("materials"),j.getString("photo"),j.getString("location"),j.getLong("arrival"),j.getLong("departure"),j.getString("customer"),j.getString("feedback"),readPhotos(j))) }
  } }
 }
 private fun readPhotos(json: JSONObject): List<String> {
  val array=json.optJSONArray("photos") ?: return listOf(json.optString("photo")).filter {it.isNotBlank()}
  return (0 until array.length()).map { array.getString(it) }.filter {it.isNotBlank()}
 }
 override suspend fun save(order: Order): Long = withContext(Dispatchers.IO) {
  require(Rules.photoLimit(order).isEmpty()) { "Máximo 10 fotografías" }
  val j=JSONObject().apply { with(order) {
   put("site",site);put("address",address);put("equipment",equipment);put("technician",technician);put("priority",priority);put("type",type);put("problem",problem);put("stage",stage.name);put("diagnosis",diagnosis);put("work",work);put("materials",materials);put("photo",evidencePhotos.firstOrNull().orEmpty());put("photos",JSONArray(evidencePhotos));put("location",location);put("arrival",arrival);put("departure",departure);put("customer",customer);put("feedback",feedback)
  } }
  val values=ContentValues().apply { put("payload",j.toString()) }
  if(order.id==0L) writableDatabase.insertOrThrow("orders",null,values) else { check(writableDatabase.update("orders",values,"id=?",arrayOf(order.id.toString()))==1);order.id }
 }
}
