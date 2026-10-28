package cl.acis.app.data

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.acis.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface OrderRepository { suspend fun all(): List<Order>; suspend fun save(order: Order): Long }
// El repositorio permite reemplazar el almacenamiento sin cambiar las pantallas.
class LocalOrderRepository(context: Context): SQLiteOpenHelper(context,"acis.db",null,1), OrderRepository {
 override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, payload TEXT NOT NULL)") }
 override fun onUpgrade(db: SQLiteDatabase,old: Int,new: Int) { error("Se requiere una migración de datos") }
 override suspend fun all(): List<Order> = withContext(Dispatchers.IO) {
  readableDatabase.rawQuery("SELECT id,payload FROM orders ORDER BY id DESC",null).use { c -> buildList {
   while(c.moveToNext()) { val j=JSONObject(c.getString(1)); add(Order(c.getLong(0),j.getString("site"),j.getString("address"),j.getString("equipment"),j.getString("technician"),j.getString("priority"),j.getString("type"),j.getString("problem"),Stage.valueOf(j.getString("stage")),j.getString("diagnosis"),j.getString("work"),j.getString("materials"),j.getString("photo"),j.getString("location"),j.getLong("arrival"),j.getLong("departure"),j.getString("customer"),j.getString("feedback"))) }
  } }
 }
 override suspend fun save(order: Order): Long = withContext(Dispatchers.IO) {
  val j=JSONObject().apply { with(order) {
   put("site",site);put("address",address);put("equipment",equipment);put("technician",technician);put("priority",priority);put("type",type);put("problem",problem);put("stage",stage.name);put("diagnosis",diagnosis);put("work",work);put("materials",materials);put("photo",photo);put("location",location);put("arrival",arrival);put("departure",departure);put("customer",customer);put("feedback",feedback)
  } }
  val values=ContentValues().apply { put("payload",j.toString()) }
  if(order.id==0L) writableDatabase.insertOrThrow("orders",null,values) else { check(writableDatabase.update("orders",values,"id=?",arrayOf(order.id.toString()))==1);order.id }
 }
}
