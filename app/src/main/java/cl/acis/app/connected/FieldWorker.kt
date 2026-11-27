package cl.acis.app.connected

import android.content.Context
import androidx.work.*
import cl.acis.app.BuildConfig
import com.google.gson.Gson
import java.io.IOException
import retrofit2.HttpException

object Sessions {
    private var current:FieldStore?=null
    private val prefsName = if (BuildConfig.ACCOUNTS_URL.startsWith("https://"))
        "access-"+java.net.URI(BuildConfig.ACCOUNTS_URL).host else "access"
    fun load(ctx:Context):Session? {
        val raw=ctx.getSharedPreferences(prefsName,Context.MODE_PRIVATE).getString("session",null) ?: return null
        return Gson().fromJson(raw,Session::class.java).takeIf {it.expires>System.currentTimeMillis()}
    }
    fun save(ctx:Context,s:Session?) {
        ctx.getSharedPreferences(prefsName,Context.MODE_PRIVATE).edit().putString("session",s?.let {Gson().toJson(it)}).commit()
    }
    @Synchronized fun store(ctx:Context,s:Session):FieldStore {
        return current?.takeIf {it.session==s} ?: FieldStore(ctx.applicationContext,s,Network(s.token)).also { current=it }
    }
}
class FieldWorker(private val ctx:Context,params:WorkerParameters):CoroutineWorker(ctx,params) {
    override suspend fun doWork():Result {
        val session=Sessions.load(ctx) ?: return Result.success()
        if(session.profile.id!=inputData.getString("account")) return Result.success()
        return try {Sessions.store(ctx,session).sync();Result.success()}
        catch(e:SyncConflict){Result.failure()}
        catch(e:IOException){Result.retry()}
        catch(e:HttpException){if(e.code()>=500) Result.retry() else Result.failure()}
    }
    companion object {
        fun schedule(ctx:Context,id:String) {
            val request=OneTimeWorkRequestBuilder<FieldWorker>().setInputData(workDataOf("account" to id))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("ordenes-"+id,ExistingWorkPolicy.APPEND_OR_REPLACE,request)
        }
    }
}
