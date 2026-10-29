package cl.acis.app.model

import org.junit.Assert.*
import org.junit.Test

class OrderDisplayTest {
    @Test fun closedUrgentOrdersStayAfterAllOpenOrders() {
        val orders = listOf(
            Order(id=1, priority="Normal"),
            Order(id=2, priority="Urgente", stage=Stage.CLOSED),
            Order(id=3, priority="Alta"),
            Order(id=4, priority="Urgente", stage=Stage.DONE),
            Order(id=5, priority="Normal", stage=Stage.CLOSED)
        )
        assertEquals(listOf(4L,3L,1L,5L,2L),ordersByPriority(orders).map {it.id})
    }
    @Test fun equalPriorityShowsNewestFirst() {
        assertEquals(listOf(3L,2L,1L),ordersByPriority((1L..3L).map {Order(id=it,priority="Alta")}).map {it.id})
    }
    @Test fun mapsCoordinatesExcludeAccuracy() {
        assertEquals("-36.85298, -73.04904",coordinatesForMaps("-36.85298, -73.04904 · ±8 m"))
        assertEquals("0, 0",coordinatesForMaps("0, 0"))
    }
    @Test fun invalidCoordinatesCannotBeCopied() {
        listOf("", "Sin registrar", "91, 0", "0, -181", "NaN, 0", "0, 0, 0").forEach {
            assertNull(coordinatesForMaps(it))
        }
    }
}
