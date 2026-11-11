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
class OrderApprovalTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val pending = Order(
        id = 1, stage = Stage.DONE, arrival = 100, departure = 200,
        location = "Ubicación de prueba", photos = listOf("evidencia.jpg")
    )

    private class Repository(var order: Order) : OrderRepository {
        var saves = 0
        override suspend fun all() = listOf(order)
        override suspend fun delete(id: Long) = error("No se utiliza en esta prueba")
        override suspend fun save(order: Order): Long {
            saves++
            this.order = order
            return order.id
        }
    }

    private fun scenario(order: Order = pending, test: (AcisViewModel, Repository) -> Unit) {
        val repository = Repository(order)
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                val vm = AcisViewModel(repository, EvidenceStore(instrumentation.targetContext))
                store.put("approval", vm)
                vm.edit(order)
                test(vm, repository)
            }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
        }
    }

    @Test fun confirmationAndClosureAreSeparateAndCannotBeRepeated() = scenario { vm, repo ->
        vm.role("Jefatura")
        vm.approveAndClose(pending.id)
        vm.advance(pending, Stage.CLOSED)
        assertEquals(0, repo.saves)

        vm.role("Cliente")
        vm.registerConfirmation(pending.id, false) { fail("Formulario inválido") }
        assertEquals(setOf("customer", "accepted"), vm.state.value.errors.keys)
        vm.change(pending.copy(customer = " Ana ", feedback = "Conforme"))
        vm.registerConfirmation(pending.id, true) {}
        val confirmed = pending.copy(customer = "Ana", feedback = "Conforme", customerConfirmed = true)
        assertEquals(confirmed, repo.order)

        vm.registerConfirmation(pending.id, true) { fail("Conformidad repetida") }
        vm.approveAndClose(pending.id)
        vm.role("Técnico")
        vm.approveAndClose(pending.id)
        assertEquals(1, repo.saves)

        vm.role("Jefatura")
        vm.approveAndClose(pending.id)
        vm.approveAndClose(pending.id)
        assertEquals(2, repo.saves)
        assertEquals(confirmed.copy(stage = Stage.CLOSED), repo.order)
    }

    @Test fun wrongStageAndWrongConfirmationRoleAreRejected() = scenario(
        pending.copy(stage = Stage.ACTIVE, customerConfirmed = true)
    ) { vm, repo ->
        vm.role("Jefatura")
        vm.approveAndClose(pending.id)
        vm.role("Cliente")
        vm.registerConfirmation(pending.id, true) { fail("Etapa incorrecta") }
        assertEquals(0, repo.saves)
    }

    @Test fun technicianCannotRegisterConfirmation() = scenario { vm, repo ->
        vm.change(pending.copy(customer = "Ana"))
        vm.registerConfirmation(pending.id, true) { fail("Perfil incorrecto") }
        assertEquals(0, repo.saves)
    }
}
