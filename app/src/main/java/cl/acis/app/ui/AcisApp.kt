package cl.acis.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.ui.layout.ContentScale
import cl.acis.app.R
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import cl.acis.app.model.*
import cl.acis.app.viewmodel.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue=Color(0xFF125B9A)
private val Sky=Color(0xFFE4F2FC)
private fun priorityColor(priority:String)=when(priority) {
 "Alta" -> Color(0xFF87451B)
 "Urgente" -> Color(0xFF9B3038)
 else -> Blue
}
private val ControlShape=RoundedCornerShape(6.dp)
@Composable fun AcisTheme(content: @Composable ()->Unit) {
 MaterialTheme(colorScheme=lightColorScheme(primary=Blue,onPrimary=Color.White,primaryContainer=Sky,onPrimaryContainer=Color(0xFF123A58),background=Color(0xFFF5F8FC),surface=Color.White,secondary=Color(0xFF387A9D),secondaryContainer=Sky,onSecondaryContainer=Blue),content=content)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AcisApp(vm: AcisViewModel) {
 val s by vm.state.collectAsStateWithLifecycle()
 val nav=rememberNavController()
 val entry by nav.currentBackStackEntryAsState()
 val route=entry?.destination?.route ?: "orders"
 val keyboardVisible=WindowInsets.ime.getBottom(LocalDensity.current)>0
 val snackbar=remember { SnackbarHostState() }
 LaunchedEffect(s.message) { s.message?.let { snackbar.showSnackbar(it);vm.notify(null) } }
 Scaffold(topBar={ TopAppBar(title={Column { Text("ACIS",fontWeight=FontWeight.Bold);Text("SERVICIOS EN TERRENO",style=MaterialTheme.typography.labelSmall) }},navigationIcon={if(route!="orders") TextButton(shape=ControlShape,onClick={nav.popBackStack()}) { Text("‹ Volver") }},colors=TopAppBarDefaults.topAppBarColors(containerColor=Color.White)) },snackbarHost={SnackbarHost(snackbar)},containerColor=MaterialTheme.colorScheme.background) { padding ->
 Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),contentAlignment=Alignment.TopCenter) {
 Column(Modifier.widthIn(max=840.dp).fillMaxWidth()) {
 if(s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
 NavHost(
  navController=nav,
  startDestination="orders",
  modifier=Modifier.weight(1f).background(MaterialTheme.colorScheme.background),
  enterTransition={slideInHorizontally(tween(160,easing=FastOutSlowInEasing)) {it}},
  exitTransition={slideOutHorizontally(tween(160,easing=FastOutSlowInEasing)) {-it}},
  popEnterTransition={slideInHorizontally(tween(160,easing=FastOutSlowInEasing)) {-it}},
  popExitTransition={slideOutHorizontally(tween(160,easing=FastOutSlowInEasing)) {it}}
 ) {
 composable("orders") { val current by vm.state.collectAsStateWithLifecycle(); OrderList(current,vm,onOpen={nav.navigate("detail/$it")},onNew={vm.edit(Order());nav.navigate("new")}) }
 composable("new") { val current by vm.state.collectAsStateWithLifecycle(); OrderForm(current,vm) { id -> nav.navigate("detail/$id") { popUpTo("orders") } } }
 composable("detail/{id}") { back ->
 val current by vm.state.collectAsStateWithLifecycle()
 val order=current.orders.find { it.id==back.arguments?.getString("id")?.toLongOrNull() }
 if(order!=null) OrderDetail(order,current,vm) { target -> vm.edit(order);nav.navigate(target+"/"+order.id) }
 else Text("Orden no disponible",Modifier.padding(24.dp))
 }
 composable("visit/{id}") { val current by vm.state.collectAsStateWithLifecycle(); VisitForm(current,vm) { nav.popBackStack() } }
 composable("confirm/{id}") { val current by vm.state.collectAsStateWithLifecycle(); Confirmation(current,vm) { nav.popBackStack() } }
 }
 // Registrado después de NavHost para unificar el retroceso de Android y Volver.
 BackHandler(enabled=route!="orders" && !keyboardVisible) {nav.popBackStack()}
 }
 }
 }
}
@Composable private fun Page(title: String, subtitle: String, content: @Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
 Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
 Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
 content()
 Spacer(Modifier.height(24.dp))
 }
}
@Composable private fun Badge(text: String) { Surface(color=Sky,shape=RoundedCornerShape(8.dp)) { Text(text,Modifier.padding(horizontal=10.dp,vertical=6.dp),color=Blue,style=MaterialTheme.typography.labelMedium) } }
@Composable private fun Info(label: String,value: String) { Column(verticalArrangement=Arrangement.spacedBy(3.dp)) { Text(label,style=MaterialTheme.typography.labelMedium,color=Blue);Text(value.ifBlank { "Sin registrar" }) } }
@Composable private fun ActionIcon(resource:Int) {
 Icon(androidx.compose.ui.res.painterResource(resource),contentDescription=null,modifier=Modifier.size(22.dp))
 Spacer(Modifier.width(8.dp))
}
@Composable private fun CoordinateInfo(label:String,value:String) {
 val context=LocalContext.current
 val coordinates=coordinatesForMaps(value)
 Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
  Text(label,style=MaterialTheme.typography.labelMedium,color=Blue)
  Row(verticalAlignment=Alignment.CenterVertically) {
   Text(value.ifBlank {"Sin registrar"},modifier=Modifier.weight(1f))
   if(coordinates!=null) IconButton(onClick={
    val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Coordenadas",coordinates))
    if(Build.VERSION.SDK_INT<33) Toast.makeText(context,"Coordenadas copiadas",Toast.LENGTH_SHORT).show()
   }) {
    Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_copy),contentDescription="Copiar coordenadas",tint=Blue,modifier=Modifier.size(22.dp))
   }
  }
 }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun Field(label: String,value: String,error: String?=null,multiline: Boolean=false,onChange:(String)->Unit) {
 val requester=remember { BringIntoViewRequester() }
 var focused by remember { mutableStateOf(false) }
 val imeBottom=WindowInsets.ime.getBottom(LocalDensity.current)
 LaunchedEffect(focused,imeBottom) {
  if(focused && imeBottom>0) {
   withFrameNanos { }
   requester.bringIntoView()
  }
 }
 OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},isError=error!=null,trailingIcon={if(error!=null) Icon(androidx.compose.ui.res.painterResource(android.R.drawable.ic_dialog_alert),contentDescription="Error",tint=MaterialTheme.colorScheme.error)},supportingText={if(error!=null) Text("! $error")},singleLine=!multiline,minLines=if(multiline) 3 else 1,modifier=Modifier.fillMaxWidth().bringIntoViewRequester(requester).onFocusChanged {focused=it.isFocused},shape=ControlShape)
}
@Composable private fun Choice(label:String,options:List<String>,selected:String,onChange:(String)->Unit) {
 Column {
  Text(label,style=MaterialTheme.typography.labelLarge)
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   options.forEach { option ->
    val colors=when {
     label=="Prioridad" && option=="Alta" -> FilterChipDefaults.filterChipColors(
      selectedContainerColor=Color(0xFFFCE9D9),selectedLabelColor=Color(0xFF87451B))
     label=="Prioridad" && option=="Urgente" -> FilterChipDefaults.filterChipColors(
      selectedContainerColor=Color(0xFFFBE3E2),selectedLabelColor=Color(0xFF9B3038))
     else -> FilterChipDefaults.filterChipColors()
    }
    FilterChip(selected=option==selected,onClick={onChange(option)},label={Text(option)},shape=ControlShape,colors=colors)
   }
  }
 }
}
@Composable private fun OrderList(s: UiState,vm:AcisViewModel,onOpen:(Long)->Unit,onNew:()->Unit) {
 var query by rememberSaveable { mutableStateOf("") }
 var filter by rememberSaveable { mutableStateOf("Todas") }
 val visible=ordersByPriority(s.orders).filter { (filter=="Todas" || if(filter=="Cerradas") it.stage==Stage.CLOSED else it.stage!=Stage.CLOSED) && (it.site+it.equipment+it.technician+it.id).contains(query,true) }
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)) {
 item { Text("Órdenes de trabajo",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold) }
 item { Text("Cada visita, en un solo lugar.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
 item { Surface(color=Sky,shape=RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
 Text("Demostración local",fontWeight=FontWeight.SemiBold);Text("Elige un perfil para recorrer el trabajo. Los datos se guardan en este teléfono.",style=MaterialTheme.typography.bodySmall)
 Choice("Ver como",listOf("Técnico","Jefatura","Cliente"),s.role,vm::role)
 } } }
 item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("${s.orders.count { it.stage!=Stage.CLOSED }} pendientes",fontWeight=FontWeight.Bold);Text("${s.orders.count { it.stage==Stage.CLOSED }} cerradas",color=Blue) } }
 item { if(s.role=="Jefatura") Button(shape=ControlShape,onClick=onNew,modifier=Modifier.fillMaxWidth()) { Text("+ Crear orden") } }
 item { Field("Buscar órdenes",query,onChange={query=it});Choice("Mostrar",listOf("Todas","Pendientes","Cerradas"),filter) {filter=it} }
 if(visible.isEmpty()) item { Card(colors=CardDefaults.cardColors(containerColor=Color.White)) { Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Text(if(s.orders.isEmpty()) "Comencemos con la primera visita" else "No encontramos órdenes",fontWeight=FontWeight.Bold);Text(if(s.orders.isEmpty()) "En el perfil Jefatura puedes crear una orden para un local y asignar un técnico." else "Prueba otra búsqueda o cambia el filtro.");TextButton(shape=ControlShape,onClick=vm::retry) {Text("Actualizar")} } } }
 items(visible,key={it.id}) { o -> Card(onClick={onOpen(o.id)},colors=CardDefaults.cardColors(containerColor=Color.White),modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("OT · ${o.id.toString().padStart(4,'0')}",color=Blue,fontWeight=FontWeight.Bold);Badge(o.stage.label) };Text(o.site,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold);Text(o.equipment);Text(o.address,style=MaterialTheme.typography.bodySmall);HorizontalDivider();Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {Text(o.priority,color=priorityColor(o.priority),style=MaterialTheme.typography.labelLarge);Text("· ${o.technician}",style=MaterialTheme.typography.labelLarge)} } } }
 }
}
@Composable private fun OrderForm(s:UiState,vm:AcisViewModel,done:(Long)->Unit) {
 val o=s.draft
 Page("Nueva orden","Completa los datos de la visita. Todos los campos de texto son obligatorios.") {
 Field("Local",o.site,s.errors["site"]) {vm.change(o.copy(site=it))}
 Field("Dirección",o.address,s.errors["address"]) {vm.change(o.copy(address=it))}
 Field("Equipo / identificación",o.equipment,s.errors["equipment"]) {vm.change(o.copy(equipment=it))}
 Field("Técnico asignado",o.technician,s.errors["technician"]) {vm.change(o.copy(technician=it))}
 Choice("Prioridad",listOf("Normal","Alta","Urgente"),o.priority) {vm.change(o.copy(priority=it))}
 Choice("Tipo de mantención",listOf("Preventiva","Correctiva"),o.type) {vm.change(o.copy(type=it))}
 Field("Motivo de la visita",o.problem,s.errors["problem"],true) {vm.change(o.copy(problem=it))}
 Button(shape=ControlShape,onClick={vm.saveNew(done)},enabled=!s.busy,modifier=Modifier.fillMaxWidth()) {Text("Crear orden")}
 }
}
private fun time(value:Long)=if(value==0L) "Sin registrar" else SimpleDateFormat("dd/MM/yyyy · HH:mm",Locale.forLanguageTag("es-CL")).format(Date(value))
@Composable private fun OrderDetail(o:Order,s:UiState,vm:AcisViewModel,open:(String)->Unit) {
 val progress by animateFloatAsState((o.stage.ordinal+1)/5f,label="avance")
 Page("OT ${o.id.toString().padStart(4,'0')}",o.site) {
 Badge(o.stage.label);LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth())
 Info("Equipo",o.equipment);Info("Dirección",o.address);Info("Asignación","${o.technician} · ${o.priority} · ${o.type}");Info("Motivo",o.problem)
 HorizontalDivider()
 Info("Llegada",time(o.arrival));Info("Salida",time(o.departure))
 if(o.departure>0) Info("Tiempo de atención","${(o.departure-o.arrival)/60000} minutos")
 if(o.location.isNotBlank()) CoordinateInfo("Ubicación registrada",o.location)
 if(o.diagnosis.isNotBlank()) Info("Diagnóstico",o.diagnosis)
 if(o.work.isNotBlank()) Info("Trabajo realizado",o.work)
 if(o.materials.isNotBlank()) Info("Materiales",o.materials)
 Photos(o.evidencePhotos)
 if(o.customer.isNotBlank()) { Info("Conformidad registrada por",o.customer);Info("Observaciones",o.feedback) }
 if(s.role=="Técnico") when(o.stage) {
 Stage.ASSIGNED -> Button(shape=ControlShape,onClick={vm.advance(o,Stage.ACCEPTED)},enabled=!s.busy,modifier=Modifier.fillMaxWidth()) {Text("Aceptar orden")}
 Stage.ACCEPTED -> Button(shape=ControlShape,onClick={vm.advance(o,Stage.ACTIVE)},enabled=!s.busy,modifier=Modifier.fillMaxWidth()) {Text("Registrar llegada e iniciar")}
 Stage.ACTIVE -> Button(shape=ControlShape,onClick={open("visit")},modifier=Modifier.fillMaxWidth()) {Text("Registrar intervención")}
 else -> Unit
 }
 if(s.role=="Cliente" && o.stage==Stage.DONE) Button(shape=ControlShape,onClick={open("confirm")},modifier=Modifier.fillMaxWidth()) {Text("Revisar y confirmar trabajo")}
 }
}
@Composable private fun Photo(path:String) {
 val result by produceState<Pair<android.graphics.Bitmap?,Boolean>>(null to false,path) {
  value=null to false
  value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { loadEvidencePhoto(path) } to true
 }
 val bitmap=result.first
 Box(Modifier.fillMaxWidth().height(240.dp),contentAlignment=Alignment.Center) {
  if(bitmap!=null) Image(bitmap.asImageBitmap(),"Evidencia del equipo",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
  else if(path.isNotBlank() && result.second) Text("No se pudo abrir la fotografía.",color=MaterialTheme.colorScheme.error)
 }
}
@Composable private fun Photos(paths:List<String>,enabled:Boolean=true,onRemove:((String)->Unit)?=null) {
 if(paths.isEmpty() && onRemove==null) return
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("Fotografías (${paths.size}/${Rules.MAX_PHOTOS})",style=MaterialTheme.typography.titleMedium)
  if(paths.isEmpty()) Text("Agrega al menos una foto para finalizar la visita.",style=MaterialTheme.typography.bodySmall)
  LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   items(paths,key={it}) { path ->
    Column(Modifier.width(260.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Photo(path)
     if(onRemove!=null) Button(shape=ControlShape,onClick={onRemove(path)},enabled=enabled,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFBE3E2),contentColor=Color(0xFF9B3038))) {
      Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_delete_photo),contentDescription=null,modifier=Modifier.size(20.dp))
      Spacer(Modifier.width(8.dp))
      Text("Eliminar")
     }
    }
   }
  }
  if(paths.size>1) Text("Desliza hacia los lados para ver las fotos.",style=MaterialTheme.typography.bodySmall)
 }
}
@Composable private fun VisitForm(s:UiState,vm:AcisViewModel,done:()->Unit) {
 val o=s.draft
 val context=LocalContext.current
 var pending by rememberSaveable { mutableStateOf("") }
 var locating by remember { mutableStateOf(false) }
 val locationManager=remember {context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager}
 val handler=remember {Handler(Looper.getMainLooper())}
 var listener by remember {mutableStateOf<LocationListener?>(null)}
 DisposableEffect(Unit) { onDispose { listener?.let { locationManager.removeUpdates(it) };handler.removeCallbacksAndMessages(null) } }
 fun locate() {
  locating=true
  try {
   val provider=if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
   if(!locationManager.isProviderEnabled(provider)) {locating=false;vm.notify("Activa la ubicación del teléfono y vuelve a intentarlo.");return}
   val callback=object:LocationListener {
    override fun onLocationChanged(l:Location) { locationManager.removeUpdates(this);handler.removeCallbacksAndMessages(null);locating=false;vm.change(vm.state.value.draft.copy(location=String.format(Locale.US,"%.5f, %.5f · ±%.0f m",l.latitude,l.longitude,l.accuracy))) }
    override fun onProviderEnabled(p:String) {}
    override fun onProviderDisabled(p:String) {}
    @Deprecated("Legacy") override fun onStatusChanged(p:String,status:Int,extras:Bundle?) {}
   }
   listener=callback;locationManager.requestLocationUpdates(provider,0L,0f,callback,Looper.getMainLooper())
   handler.postDelayed({locationManager.removeUpdates(callback);locating=false;vm.notify("No fue posible obtener la ubicación. Puedes guardar y reintentar después.")},20000)
  } catch(e:Exception) {locating=false;vm.notify("No se pudo acceder a la ubicación.")}
 }
 val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted -> if(granted.values.any {it}) locate() else vm.notify("Sin permiso de ubicación. Puedes continuar registrando la visita.") }
 val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
  if(success && pending.isNotBlank()) vm.addPhoto(pending) else {if(pending.isNotBlank()) File(pending).delete();vm.notify("Captura cancelada; el formulario se conserva.")}
  pending=""
 }
 val gallery=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments(),vm::importPhotos)
 Page("Intervención","OT ${o.id} · ${o.site}") {
 Field("Diagnóstico",o.diagnosis,s.errors["diagnosis"],true) {vm.change(o.copy(diagnosis=it))}
 Field("Trabajo realizado",o.work,s.errors["work"],true) {vm.change(o.copy(work=it))}
 Field("Materiales (opcional)",o.materials,multiline=true) {vm.change(o.copy(materials=it))}
 Photos(o.evidencePhotos,enabled=!s.busy,onRemove=vm::removePhoto)
 Button(shape=ControlShape,colors=ButtonDefaults.buttonColors(containerColor=Sky,contentColor=Blue),onClick={
  try {val dir=File(context.filesDir,"evidencias").apply {mkdirs()};val f=File.createTempFile("ot_${o.id}_",".jpg",dir);pending=f.absolutePath;camera.launch(FileProvider.getUriForFile(context,context.packageName+".files",f))}
  catch(e:Exception) {vm.notify("No se pudo abrir la cámara. Verifica que haya una aplicación de cámara disponible.")}
 },enabled=!s.busy && o.evidencePhotos.size<Rules.MAX_PHOTOS,modifier=Modifier.fillMaxWidth()) {ActionIcon(R.drawable.ic_camera);Text("Tomar fotografía")}
 Button(shape=ControlShape,colors=ButtonDefaults.buttonColors(containerColor=Sky,contentColor=Blue),onClick={
  try {gallery.launch(arrayOf("image/*"))} catch(e:Exception) {vm.notify("No se pudo abrir el selector de fotografías.")}
 },enabled=!s.busy && o.evidencePhotos.size<Rules.MAX_PHOTOS,modifier=Modifier.fillMaxWidth()) {ActionIcon(R.drawable.ic_gallery);Text("Elegir del teléfono")}
 if(o.evidencePhotos.size>=Rules.MAX_PHOTOS) Text("Llegaste al máximo de 10 fotos. Quita una para agregar otra.",style=MaterialTheme.typography.bodySmall)
 AnimatedVisibility(s.errors["photo"]!=null) { Text(s.errors["photo"].orEmpty(),color=MaterialTheme.colorScheme.error) }
 CoordinateInfo("Ubicación de la visita",o.location)
 OutlinedButton(shape=ControlShape,onClick={permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))},enabled=!locating,modifier=Modifier.fillMaxWidth()) {ActionIcon(R.drawable.ic_location);Text(if(locating) "Buscando ubicación…" else "Registrar ubicación actual")}
 AnimatedVisibility(locating) {LinearProgressIndicator(Modifier.fillMaxWidth())}
 Text("Puedes guardar un avance y continuar más tarde, incluso sin internet.",style=MaterialTheme.typography.bodySmall)
 OutlinedButton(shape=ControlShape,onClick={vm.saveDraft {done()}},enabled=!s.busy,modifier=Modifier.fillMaxWidth()) {Text("Guardar avance")}
 Button(shape=ControlShape,onClick={vm.advance(o,Stage.DONE) {done()}},enabled=!s.busy,modifier=Modifier.fillMaxWidth()) {Text("Finalizar y registrar salida")}
 }
}
@Composable private fun Confirmation(s:UiState,vm:AcisViewModel,done:()->Unit) {
 val o=s.draft
 var accepted by rememberSaveable {mutableStateOf(false)}
 Page("Conformidad del cliente","Revisa el trabajo realizado antes de confirmar.") {
 Info("Local",o.site);Info("Trabajo",o.work);if(o.location.isNotBlank()) CoordinateInfo("Ubicación registrada",o.location);Photos(o.evidencePhotos)
 Field("Nombre de quien confirma",o.customer,s.errors["customer"]) {vm.change(o.copy(customer=it))}
 Field("Observaciones (opcional)",o.feedback,multiline=true) {vm.change(o.copy(feedback=it))}
 Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(checked=accepted,onCheckedChange={accepted=it});Text("Confirmo mi conformidad con el trabajo",Modifier.weight(1f))}
 Button(shape=ControlShape,onClick={vm.advance(o,Stage.CLOSED) {done()}},enabled=accepted&&!s.busy,modifier=Modifier.fillMaxWidth()) {Text("Confirmar y cerrar orden")}
 }
}
