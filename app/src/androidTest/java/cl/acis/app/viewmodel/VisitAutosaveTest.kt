package cl.acis.app.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.acis.app.data.EvidenceStore
import cl.acis.app.data.OrderRepository
import cl.acis.app.model.Order
import cl.acis.app.model.Stage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisitAutosaveTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private class Repository : OrderRepository {
        var order = Order(id=1,stage=Stage.ACTIVE,site="Local de prueba")
        var saves = 0
        var fail = false
        override suspend fun delete(id:Long) {error("No se utiliza en esta prueba")}
        override suspend fun all() = listOf(order)
        override suspend fun save(order: Order): Long {
            if(fail) error("Sin espacio")
            saves++
            this.order=order
            return order.id
        }
    }
    private fun checkVisit(action:(AcisViewModel,Repository)->Unit) {
        val repository=Repository()
        val store=ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                val vm=AcisViewModel(repository,EvidenceStore(instrumentation.targetContext))
                store.put("test",vm)
                vm.edit(repository.order)
                action(vm,repository)
            }
        } finally {instrumentation.runOnMainSync {store.clear()} }
    }
    @Test fun backSavesIncompleteVisitWithoutFinishingIt() = checkVisit { vm,repo ->
        vm.change(vm.state.value.draft.copy(diagnosis="Diagnóstico sin terminar",materials="Material de prueba"))
        var returned=false
        vm.saveVisitBeforeLeaving(1) {returned=true}
        assertTrue(returned)
        assertEquals(1,repo.saves)
        assertEquals(Stage.ACTIVE,repo.order.stage)
        assertEquals("Diagnóstico sin terminar",repo.order.diagnosis)
        assertEquals("Material de prueba",repo.order.materials)
        assertEquals(0L,repo.order.departure)
    }
    @Test fun saveFailureKeepsFormAndDoesNotNavigate() = checkVisit { vm,repo ->
        repo.fail=true
        vm.change(vm.state.value.draft.copy(work="Trabajo pendiente de guardar"))
        var returned=false
        vm.saveVisitBeforeLeaving(1) {returned=true}
        assertFalse(returned)
        assertEquals("Trabajo pendiente de guardar",vm.state.value.draft.work)
        assertFalse(vm.state.value.busy)
        assertNotNull(vm.state.value.message)
    }
    @Test fun unchangedVisitReturnsWithoutAnotherWrite() = checkVisit { vm,repo ->
        var returned=false
        vm.saveVisitBeforeLeaving(1) {returned=true}
        assertTrue(returned)
        assertEquals(0,repo.saves)
    }
}
