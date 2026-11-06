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

data class UiState(val orders: List<Order> = emptyList(), val draft: Order = Order(), val errors: Map<String,String> = emptyMap(), val busy: Boolean = false, val message: String? = null, val role: String = "Técnico")
class AcisViewModel(private val repository: OrderRepository, private val evidenceStore: EvidenceStore): ViewModel() {
 private var draftSession = 0
 private val mutable = MutableStateFlow(UiState())
 val state = mutable.asStateFlow()
 init { refresh() }
 private fun refresh() { viewModelScope.launch { mutable.value=mutable.value.copy(busy=true);try { mutable.value=mutable.value.copy(orders=repository.all()) } catch(e: Exception) { notify("No se pudieron leer las órdenes. Vuelve a intentarlo.") } finally { mutable.value=mutable.value.copy(busy=false) } } }
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
    runCatching {evidenceStore.discard(order.evidencePhotos.filterNot {it in referenced})}
    notify("Orden eliminada de este teléfono")
    done()
   } catch(e:kotlinx.coroutines.CancellationException) {throw e}
   catch(e:Exception) {notify("No se pudo eliminar la orden. Vuelve a intentarlo.")}
   finally {mutable.value=state.value.copy(busy=false)}
  }
 }
 fun saveNew(done: (Long)->Unit) {
  val o=state.value.draft; val errors=Rules.creation(o)
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
    if(session!=draftSession || draft.id!=orderId || draft.stage!=Stage.ACTIVE) {
     notify("No se pudo identificar el avance de esta visita.");return@launch
    }
    if(current.orders.find {it.id==orderId}==draft) {done();return@launch}
    val errors=Rules.photoLimit(draft)
    if(errors.isNotEmpty()) {mutable.value=state.value.copy(errors=errors);return@launch}
    mutable.value=state.value.copy(busy=true)
    try {
     repository.save(draft)
     mutable.value=state.value.copy(orders=repository.all(),errors=emptyMap())
     notify("Avance guardado automáticamente")
     done()
    } finally { mutable.value=state.value.copy(busy=false) }
   } catch(e:kotlinx.coroutines.CancellationException) {throw e}
   catch(e:Exception) {notify("No se pudo guardar el avance. Tus datos siguen aquí; vuelve a intentarlo.")}
   finally {leavingVisit=false}
  }
 }
 fun saveDraft(done: (Long)->Unit) = persist(state.value.draft,done)
 fun advance(o: Order,to: Stage,customerAccepted: Boolean = false,done: (Long)->Unit = {}) {
  if(!Rules.canMove(o.stage,to)) { notify("Esta acción no corresponde al estado actual.");return }
  val errors=if(to==Stage.DONE) Rules.closing(o) else if(to==Stage.CLOSED) Rules.confirmation(o,customerAccepted) else emptyMap()
  mutable.value=state.value.copy(errors=errors)
  if(errors.isNotEmpty()) return
  val now=System.currentTimeMillis()
  persist(o.copy(stage=to,arrival=if(to==Stage.ACTIVE) now else o.arrival,departure=if(to==Stage.DONE) now else o.departure),done)
 }
 private fun persist(o: Order,done: (Long)->Unit) {
  if(state.value.busy) return
  val errors=Rules.photoLimit(o)
  if(errors.isNotEmpty()) {mutable.value=state.value.copy(errors=errors);return}
  mutable.value=state.value.copy(busy=true)
  viewModelScope.launch {
   try { val id=repository.save(o);mutable.value=state.value.copy(orders=repository.all(),draft=o.copy(id=id),errors=emptyMap());notify("Guardado en este teléfono");done(id) }
   catch(e: Exception) { notify("No se pudo guardar. Tus datos siguen en el formulario; vuelve a intentarlo.") }
   finally { mutable.value=state.value.copy(busy=false) }
  }
 }
}
