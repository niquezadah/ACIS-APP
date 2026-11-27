package cl.acis.app.connected

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import cl.acis.app.viewmodel.AcisViewModel
import kotlinx.coroutines.*

@Composable fun ResolveConflict(store:FieldStore,id:Long,vm:AcisViewModel) {
    val scope=rememberCoroutineScope()
    var busy by remember(id){mutableStateOf(false)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->
        if(uri!=null) scope.launch {
            busy=true
            try { store.exportAndResolve(id,uri);vm.retry();vm.notify("Copia guardada y orden actualizada") }
            catch(e:CancellationException){throw e}
            catch(e:Exception){vm.notify("No se pudo completar la copia. Tus cambios siguen en el teléfono.")}
            finally {busy=false}
        }
    }
    Text("Puedes guardar tu avance y sus fotos en un ZIP y recuperar la versión del servidor.")
    OutlinedButton(enabled=!busy,onClick={launcher.launch("avance-OT-"+id+".zip")}){Text("Guardar copia y actualizar OT")}
}
