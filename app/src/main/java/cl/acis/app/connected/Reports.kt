package cl.acis.app.connected

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import cl.acis.app.model.*
import cl.acis.app.ui.ControlShape
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.Date

@Composable internal fun ReportButton(orders:List<Order>) {
    val ctx=LocalContext.current
    val scope=rememberCoroutineScope()
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->
        if(uri!=null) scope.launch {
            try{
                withContext(Dispatchers.IO){checkNotNull(ctx.contentResolver.openOutputStream(uri)).bufferedWriter(Charsets.UTF_8).use {it.write("\uFEFF");it.write(report(orders,System.currentTimeMillis()))}}
                Toast.makeText(ctx,"Informe guardado",Toast.LENGTH_SHORT).show()
            }catch(e:CancellationException){throw e}catch(e:Exception){Toast.makeText(ctx,"No se pudo guardar el informe",Toast.LENGTH_SHORT).show()}
        }
    }
    OutlinedButton(onClick={launcher.launch("ordenes-acis.csv")},shape=ControlShape){Text("Exportar informe")}
}
@Composable internal fun History(api:FieldApi,id:Long,revision:Long) {
    var expanded by remember(id){mutableStateOf(false)}
    var events by remember(id){mutableStateOf(emptyList<Event>())}
    var message by remember(id){mutableStateOf("")}
    TextButton(onClick={expanded=!expanded}){Text(if(expanded) "Ocultar historial" else "Ver historial de la orden")}
    LaunchedEffect(id,revision,expanded){if(expanded){
        try{events=api.history(id);message=""}catch(e:CancellationException){throw e}catch(e:Exception){message=problem(e)}
    }}
    if(expanded){
        if(message.isNotEmpty()) Text(message)
        events.forEach {event->
            val action=Stage.entries.find {it.name==event.action}?.label ?: event.action
            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(event.at))+" · "+event.name+" · "+action)
        }
    }
}
