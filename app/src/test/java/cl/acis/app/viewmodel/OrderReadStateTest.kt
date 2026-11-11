package cl.acis.app.viewmodel

import cl.acis.app.model.Order
import org.junit.Assert.*
import org.junit.Test

class OrderReadStateTest {
    @Test fun initialStateDoesNotClaimDatabaseIsEmpty() {
        assertFalse(UiState().emptyDatabase)
        assertFalse(UiState().completeOrderList)
    }

    @Test fun successfulEmptyReadCanShowEmptyDatabase() {
        assertTrue(UiState(ordersLoaded = true).emptyDatabase)
    }

    @Test fun failedReadRemainsVisibleWhenSnackbarIsDismissed() {
        val state = UiState(orderReadError = "Error de lectura", message = "Aviso")
        val dismissed = state.copy(message = null)
        assertEquals("Error de lectura", dismissed.orderReadError)
        assertFalse(dismissed.emptyDatabase)
    }

    @Test fun unreadableRowsNeverMeanEmptyDatabase() {
        val state = UiState(ordersLoaded = true, unreadableOrderIds = listOf(1))
        assertFalse(state.emptyDatabase)
        assertFalse(state.completeOrderList)
    }

    @Test fun failedRefreshCanKeepPreviouslyLoadedOrders() {
        val previous = UiState(ordersLoaded = true, orders = listOf(Order(id = 1)))
        val failed = previous.copy(orderReadError = "Error de lectura")
        assertEquals(previous.orders, failed.orders)
        assertFalse(failed.completeOrderList)
    }
}
