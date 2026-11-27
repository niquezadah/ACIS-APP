package cl.acis.app.connected

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import cl.acis.app.ui.*
import kotlinx.coroutines.*
import java.util.UUID
import java.text.DateFormat
import java.util.Date

@Composable fun EquipmentPicker(api:FieldApi,site:String?,name:String,error:String?,choose:(String)->Unit) {
    if(site==null){Text("Selecciona el local para ver sus equipos.");return}
    var rows by remember(site){mutableStateOf(emptyList<Equipment>())}
    var editing by remember(site){mutableStateOf(false)}
    var label by remember(site){mutableStateOf("")}
    var location by remember(site){mutableStateOf("")}
    var busy by remember(site){mutableStateOf(false)}
    var message by remember(site){mutableStateOf("")}
    var reload by remember(site){mutableIntStateOf(0)}
    val scope=rememberCoroutineScope()
    LaunchedEffect(site,reload){
        try{rows=api.equipment(site);message=""}catch(e:CancellationException){throw e}catch(e:Exception){message=problem(e)}
    }
    val selected=rows.firstOrNull {it.name==name}
    CatalogSelector("Equipo",selected?.id,name,rows.filter {it.active}.map {it.id to it.name},error,!busy){id->choose(rows.first {it.id==id}.name)}
    if(selected!=null){
        Text(selected.location+" · Última mantención: "+if(selected.lastService>0) DateFormat.getDateInstance().format(Date(selected.lastService)) else "Sin registrar")
    }
    TextButton(onClick={editing=!editing}){Text(if(editing) "Ocultar registro" else "Registrar equipo del local")}
    if(editing){
        Field("Identificación del equipo",label){label=it}
        Field("Dónde está instalado",location){location=it}
        OutlinedButton(enabled=!busy && label.isNotBlank() && location.isNotBlank(),onClick={
            busy=true
            scope.launch {try{
                val e=Equipment(UUID.randomUUID().toString(),site,label.trim(),location.trim(),true,0)
                api.saveEquipment(e.id,e);choose(e.name);editing=false;label="";location="";reload++
            }catch(e:CancellationException){throw e}catch(e:Exception){message=problem(e)}finally{busy=false}}
        }){Text("Guardar equipo")}
    }
    if(message.isNotBlank()){Text(message);TextButton(onClick={reload++}){Text("Reintentar")}}
}
