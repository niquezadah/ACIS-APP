package cl.acis.app.viewmodel
import android.net.Uri
import cl.acis.app.data.EvidenceStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.acis.app.data.OrderRepository
import cl.acis.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
 val orders: List<Order> = emptyList(),
 val draft: Order = Order(),
 val errors: Map<String,String> = emptyMap(),
 val busy: Boolean = false,
 val message: String? = null,
 val role: String = "Técnico",
 val ordersLoaded: Boolean = false,
 val orderReadError: String? = null,
 val unreadableOrderIds: List<Long> = emptyList()
) {
 val completeOrderList: Boolean
  get() = ordersLoaded && orderReadError == null && unreadableOrderIds.isEmpty()
 val emptyDatabase: Boolean
  get() = completeOrderList && orders.isEmpty()
}
class AcisViewModel(private val repository: OrderRepository, private val evidenceStore: EvidenceStore): ViewModel() {
 private var draftSession = 0
 private val mutable = MutableStateFlow(UiState())
 val state = mutable.asStateFlow()
 init { refresh() }
 private fun refresh() {
  if(state.value.busy) return
  mutable.value=state.value.copy(busy=true)
  viewModelScope.launch {
   try { readOrders() }
   finally { mutable.value=state.value.copy(busy=false) }
  }
 }
 private suspend fun readOrders() {
  try {
   val result=repository.read()
   mutable.value=state.value.copy(
    orders=result.orders,ordersLoaded=true,orderReadError=null,
    unreadableOrderIds=result.unreadableOrderIds
   )
  } catch(e:kotlinx.coroutines.CancellationException) {throw e}
  catch(e:Exception) {
   mutable.value=state.value.copy(orderReadError=
    "No se pudieron leer las órdenes. Reintenta la lectura. Si el problema continúa, conserva la instalación y solicita una revisión.")
  }
 }
 fun retry() = refresh()
 fun role(value: String) { mutable.value=mutable.value.copy(role=value) }
 fun edit(o: Order) { draftSession++; mutable.value=mutable.value.copy(draft=o,errors=emptyMap()) }
 fun change(o: Order) { mutable.value=mutable.value.copy(draft=o,errors=emptyMap()) }
 fun addPhoto(path: String) {
  val o=state.value.draft
  if(o.evidencePhotos.size>=Rules.MAX_PHOTOS) {notify("Puedes agregar hasta 10 fotografías por orden");return}
  change(o.withPhotos(o.evidencePhotos+path))
 }
 fun removePhoto(path: String) {
  if(state.value.busy) return
  change(state.value.draft.withPhotos(state.value.draft.evidencePhotos.filterNot {it==path}))
 }
 fun importPhotos(uris: List<Uri>) {
  if(uris.isEmpty() || state.value.busy) return
  val available=Rules.MAX_PHOTOS-state.value.draft.evidencePhotos.size
  if(available<=0) {notify("Ya agregaste las 10 fotografías permitidas");return}
  val selected=uris.distinct().take(available)
  val session=draftSession
  mutable.value=state.value.copy(busy=true)
  viewModelScope.launch {
   try {
    val imported=evidenceStore.importPhotos(selected)
    if(session!=draftSession) {evidenceStore.discard(imported);return@launch}
    change(state.value.draft.withPhotos(state.value.draft.evidencePhotos+imported))
    notify(when {
     imported.size<selected.size -> "Se agregaron ${imported.size} fotos. Algunas no se pudieron abrir."
     uris.distinct().size>available -> "Se agregaron ${imported.size} fotos para completar el máximo de 10."
     else -> "${imported.size} fotografías agregadas"
    })
   } catch(e:kotlinx.coroutines.CancellationException) {throw e}
   catch(e:Exception) {notify("No se pudieron importar las fotografías. Vuelve a intentarlo.")}
   finally {mutable.value=state.value.copy(busy=false)}
  }
 }
 fun notify(text: String?) { mutable.value=mutable.value.copy(message=text) }
 fun deleteOrder(id:Long,done:()->Unit) {
  if(state.value.role!="Jefatura") {notify("Solo Jefatura puede eliminar una orden.");return}
  if(state.value.busy) return
  val order=state.value.orders.find {it.id==id} ?: return
  mutable.value=state.value.copy(busy=true)
  viewModelScope.launch {
   try {
    repository.delete(id)
    val remaining=state.value.orders.filterNot {it.id==id}
    mutable.value=state.value.copy(orders=remaining,draft=if(state.value.draft.id==id) Order() else state.value.draft,errors=emptyMap())
    val referenced=remaining.flatMap {it.evidencePhotos}.toSet()
    // Una fila ilegible también puede referenciar estas fotografías.
    if(state.value.completeOrderList) {
     runCatching {evidenceStore.discard(order.evidencePhotos.filterNot {it in referenced})}
    }
    notify("Orden eliminada de este teléfono")
    done()
   } catch(e:kotlinx.coroutines.CancellationException) {throw e}
   catch(e:Exception) {notify("No se pudo eliminar la orden. Vuelve a intentarlo.")}
   finally {mutable.value=state.value.copy(busy=false)}
  }
 }
 fun saveNew(done: (Long)->Unit) {
  val o=state.value.draft; val errors=Rules.creation(o)
  if(o.id!=0L || o.stage!=Stage.ASSIGNED) return
  mutable.value=state.value.copy(errors=errors)
  if(errors.isEmpty()) persist(o,done)
 }
 private var leavingVisit=false
 fun saveVisitBeforeLeaving(orderId:Long,done:()->Unit) {
  if(leavingVisit) return
  leavingVisit=true
  val session=draftSession
  viewModelScope.launch {
   try {
    state.first { !it.busy }
    val current=state.value
    val draft=current.draft
    val order=current.orders.find {it.id==orderId}
    if(session!=draftSession || draft.id!=orderId || draft.stage!=Stage.ACTIVE || order?.stage!=Stage.ACTIVE) {
     notify("No se pudo identificar el avance de esta visita.");return@launch
    }
    if(order==draft) {done();return@launch}
    val errors=Rules.photoLimit(draft)
    if(errors.isNotEmpty()) {mutable.value=state.value.copy(errors=errors);return@launch}
    mutable.value=state.value.copy(busy=true)
    try {
     repository.save(draft)
     readOrders()
     mutable.value=state.value.copy(errors=emptyMap())
     notify("Avance guardado automáticamente")
     done()
    } finally { mutable.value=state.value.copy(busy=false) }
   } catch(e:kotlinx.coroutines.CancellationException) {throw e}
   catch(e:Exception) {notify("No se pudo guardar el avance. Tus datos siguen aquí; vuelve a intentarlo.")}
   finally {leavingVisit=false}
  }
 }
 fun saveDraft(done: (Long)->Unit) {
  val draft=state.value.draft
  if(draft.stage!=Stage.ACTIVE || state.value.orders.find {it.id==draft.id}?.stage!=Stage.ACTIVE) return
  persist(draft,done)
 }
 fun advance(o: Order,to: Stage,done: (Long)->Unit = {}) {
  val current=state.value
  if(current.busy) return
  val order=current.orders.find {it.id==o.id}
  if(to==Stage.CLOSED || order?.stage!=o.stage || !Rules.canMove(o.stage,to)) {
   notify("Esta acción no corresponde al estado actual.");return
  }
  val errors=if(to==Stage.DONE) Rules.closing(o) else emptyMap()
  mutable.value=state.value.copy(errors=errors)
  if(errors.isNotEmpty()) return
  val now=System.currentTimeMillis()
  persist(o.copy(stage=to,arrival=if(to==Stage.ACTIVE) now else o.arrival,departure=if(to==Stage.DONE) now else o.departure),done)
 }
 fun registerConfirmation(id: Long,accepted: Boolean,done: (Long)->Unit) {
  val current=state.value
  if(current.busy) return
  if(current.role!="Cliente") {notify("Solo Cliente puede registrar la conformidad.");return}
  val order=current.orders.find {it.id==id}
  val draft=current.draft
  if(order==null || order.stage!=Stage.DONE || order.customerConfirmed || draft.id!=id) {
   notify("La orden no está pendiente de conformidad.");return
  }
  val confirmed=order.copy(customer=draft.customer.trim(),feedback=draft.feedback,customerConfirmed=true)
  val errors=Rules.confirmation(confirmed,accepted)
  mutable.value=current.copy(errors=errors)
  if(errors.isEmpty()) persist(confirmed,done)
 }
 fun approveAndClose(id: Long) {
  val current=state.value
  if(current.busy) return
  if(current.role!="Jefatura") {notify("Solo Jefatura puede aprobar y cerrar la orden.");return}
  val order=current.orders.find {it.id==id}
  if(order==null || !Rules.canMove(order.stage,Stage.CLOSED) || !order.customerConfirmed) {
   notify("La orden debe estar por confirmar y tener conformidad registrada.");return
  }
  persist(order.copy(stage=Stage.CLOSED)) {}
 }
 private fun persist(o: Order,done: (Long)->Unit) {
  if(state.value.busy) return
  val errors=Rules.photoLimit(o)
  if(errors.isNotEmpty()) {mutable.value=state.value.copy(errors=errors);return}
  mutable.value=state.value.copy(busy=true)
  viewModelScope.launch {
   try {
    val id=repository.save(o)
    val saved=o.copy(id=id)
    val orders=state.value.orders
    val updated=if(o.id==0L) listOf(saved)+orders else orders.map {if(it.id==id) saved else it}
    mutable.value=state.value.copy(orders=updated,draft=saved,errors=emptyMap())
    notify("Guardado en este teléfono")
    done(id)
   }
   catch(e: Exception) { notify("No se pudo guardar. Tus datos siguen en el formulario; vuelve a intentarlo.") }
   finally { mutable.value=state.value.copy(busy=false) }
  }
 }
}
