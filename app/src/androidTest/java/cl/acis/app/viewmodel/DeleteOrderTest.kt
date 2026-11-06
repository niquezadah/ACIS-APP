package cl.acis.app.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.acis.app.data.EvidenceStore
import cl.acis.app.data.OrderRepository
import cl.acis.app.model.Order
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteOrderTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private class Repository : OrderRepository {
        var orders=listOf(Order(id=1,site="Orden a eliminar"),Order(id=2,site="Otra orden"))
        var deletes=0
        var fail=false
        override suspend fun all()=orders
        override suspend fun save(order:Order)=order.id
        override suspend fun delete(id:Long) {
            if(fail) error("Falla de almacenamiento")
            deletes++
            orders=orders.filterNot {it.id==id}
        }
    }
    private fun scenario(check:(AcisViewModel,Repository)->Unit) {
        val store=ViewModelStore()
        val repo=Repository()
        lateinit var vm:AcisViewModel
        try {
            instrumentation.runOnMainSync {
                vm=AcisViewModel(repo,EvidenceStore(instrumentation.targetContext))
                store.put("test",vm)
            }
            check(vm,repo)
        } finally {instrumentation.runOnMainSync {store.clear()} }
    }
    @Test fun onlyJefaturaCanDelete()=scenario { vm,repo ->
        instrumentation.runOnMainSync {
            listOf("Técnico","Cliente").forEach { role ->
                vm.role(role)
                vm.deleteOrder(1) {fail("No debe permitir eliminar")}
            }
            assertEquals(0,repo.deletes)
            assertEquals(2,vm.state.value.orders.size)
        }
    }
    @Test fun successfulDeletionRemovesOnlySelectedOrder()=scenario { vm,repo ->
        val done=CountDownLatch(1)
        instrumentation.runOnMainSync {
            vm.role("Jefatura")
            vm.deleteOrder(1) {done.countDown()}
        }
        assertTrue(done.await(5,TimeUnit.SECONDS))
        assertEquals(1,repo.deletes)
        assertEquals(listOf(2L),vm.state.value.orders.map {it.id})
    }
    @Test fun failureKeepsOrderAndDoesNotNavigate()=scenario { vm,repo ->
        instrumentation.runOnMainSync {
            vm.role("Jefatura");repo.fail=true
            vm.deleteOrder(1) {fail("No debe navegar si falla")}
            assertEquals(2,vm.state.value.orders.size)
            assertFalse(vm.state.value.busy)
            assertNotNull(vm.state.value.message)
        }
    }
}
