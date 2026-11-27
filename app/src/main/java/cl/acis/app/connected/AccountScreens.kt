package cl.acis.app.connected

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import cl.acis.app.ui.*
import kotlinx.coroutines.*
import retrofit2.HttpException

internal fun problem(e:Exception)=when((e as? HttpException)?.code()) {
    400 -> "Revisa los datos ingresados."
    401 -> "Revisa tus credenciales o vuelve a iniciar sesión."
    403 -> "Tu cuenta no tiene permiso para esa acción."
    409 -> "El registro cambió o ya existe. Revisa los datos."
    else -> "No se pudo conectar con el servidor. Comprueba la conexión e intenta de nuevo."
}
@Composable internal fun SignIn(onLogin:(Session)->Unit) {
    val api=remember {Network().accounts}
    var mode by remember {mutableStateOf("Ingresar")}
    var email by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var name by remember {mutableStateOf("")}
    var recovery by remember {mutableStateOf("")}
    var issued by remember {mutableStateOf("")}
    var error by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    Page("ACIS en Terreno","Accede con el correo de tu cuenta.") {
        if(issued.isNotEmpty()){
            Text("Guarda esta clave de recuperación. La necesitarás si olvidas tu contraseña.")
            SelectionContainer {Text(issued)}
            Button(onClick={issued="";mode="Ingresar";password="";recovery=""}){Text("Ya guardé mi clave")}
        } else {
            Choice("Cuenta",listOf("Ingresar","Registrarme","Recuperar"),mode){if(!busy){mode=it;error=""}}
            Field("Correo",email,enabled=!busy){email=it}
            if(mode=="Registrarme") Field("Nombre",name,enabled=!busy){name=it}
            if(mode=="Recuperar") Field("Clave de recuperación",recovery,enabled=!busy){recovery=it}
            OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),enabled=!busy,singleLine=true,label={Text(if(mode=="Recuperar") "Nueva contraseña" else "Contraseña")},shape=ControlShape,visualTransformation=PasswordVisualTransformation())
            if(mode!="Ingresar") Text("Usa al menos 10 caracteres.")
            if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
            Button(enabled=!busy,onClick={
                error=accountError(mode,email,password,name,recovery)
                if(error.isEmpty()) {
                    busy=true
                    scope.launch {
                        try {
                            val c=Credentials(email.trim(),password,name.trim(),recovery.trim())
                            when(mode) {
                                "Ingresar" -> onLogin(api.login(c))
                                "Registrarme" -> issued=api.register(c).recovery
                                else -> issued=api.recover(c).recovery
                            }
                        } catch(e:CancellationException){throw e} catch(e:Exception){error=problem(e)} finally {busy=false}
                    }
                }
            },shape=ControlShape){Text(if(busy) "Un momento…" else mode)}
        }
    }
}

@Composable internal fun ProfileScreen(store:FieldStore,onExit:()->Unit,onBack:()->Unit) {
    val api=store.network.accounts
    val profile=store.session.profile
    var name by remember {mutableStateOf(profile.name)}
    var msg by remember {mutableStateOf("")}
    var users by remember {mutableStateOf(emptyList<Profile>())}
    var entries by remember {mutableStateOf(emptyList<Entry>())}
    var selected by remember {mutableStateOf<Profile?>(null)}
    var role by remember {mutableStateOf("Cliente")}
    var link by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    fun run(action:suspend ()->Unit){if(busy)return; busy=true;scope.launch {try{action()} catch(e:CancellationException){throw e} catch(e:Exception){msg=problem(e)} finally{busy=false}}}
    fun load(){run {name=api.me().name;if(profile.role=="Administrador"){users=api.users();entries=store.network.field.catalog()}}}
    LaunchedEffect(Unit){load()}
    Page("Mi cuenta",profile.email+" · "+profile.role){
        Field("Nombre",name){name=it}
        Button(onClick={run {api.edit(Credentials(name=name.trim()));msg="Nombre actualizado"}},enabled=!busy && name.trim().length in 2..120,shape=ControlShape){Text("Guardar nombre")}
        if(profile.role=="Administrador"){
            Text("Permisos de usuarios",style=MaterialTheme.typography.titleMedium)
            CatalogSelector("Cuenta",selected?.id,selected?.email.orEmpty(),users.filter {it.id!=profile.id}.map {it.id to it.email},null,!busy){ id->
                selected=users.first {it.id==id}; role=selected!!.role;link=selected!!.scope
            }
            selected?.let { user->
                Choice("Perfil",listOf("Cliente","Técnico","Jefatura","Administrador"),role){role=it;link=""}
                val kind=when(role){"Cliente"->"sites";"Técnico"->"technicians";else->""}
                if(kind.isNotEmpty()) CatalogSelector(if(role=="Cliente") "Local" else "Ficha del técnico",link.ifBlank {null},entries.find {it.id==link}?.name.orEmpty(),entries.filter {it.kind==kind && it.active}.map{it.id to it.name},null,!busy){link=it}
                Button(enabled=!busy && (kind.isEmpty() || link.isNotBlank()),onClick={run {api.access(user.id,Access(role,link));users=api.users();selected=null;msg="Permisos guardados"}},shape=ControlShape){Text("Guardar permisos")}
            }
            TextButton(onClick={load()},enabled=!busy){Text("Actualizar registros")}
        }
        if(msg.isNotBlank()) Text(msg)
        OutlinedButton(onClick={run {api.logout();onExit()}},enabled=!busy){Text("Cerrar sesión")}
        TextButton(onClick=onBack){Text("Volver a las órdenes")}
    }
}
