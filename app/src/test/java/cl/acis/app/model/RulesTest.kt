package cl.acis.app.model
import org.junit.Assert.*
import org.junit.Test
class RulesTest {
 @Test fun tenPhotosCanFinishButElevenCannot() {
  val order=Order(diagnosis="Filtro obstruido",work="Limpieza realizada").withPhotos((1..10).map {"foto_$it.jpg"})
  assertTrue(Rules.closing(order).isEmpty())
  assertTrue(Rules.closing(order.withPhotos(order.evidencePhotos+"foto_11.jpg")).containsKey("photo"))
 }
 @Test fun removingAllPhotosDoesNotRestoreLegacyPhoto() {
  val order=Order(photo="antigua.jpg").withPhotos(emptyList())
  assertTrue(order.evidencePhotos.isEmpty())
  assertTrue(Rules.closing(order).containsKey("photo"))
 }
 @Test fun oldOrderKeepsItsSinglePhoto() { assertEquals(listOf("antigua.jpg"),Order(photo="antigua.jpg").evidencePhotos) }
 @Test fun emptyOrderIsRejected() { assertEquals(setOf("site","address","equipment","technician","problem"),Rules.creation(Order()).keys) }
 @Test fun completeOrderIsAccepted() { assertTrue(Rules.creation(Order(site="Local",address="Calle 123",equipment="Equipo 1",technician="Ana",problem="Mantención mensual")).isEmpty()) }
 @Test fun whitespaceDoesNotCountAsDiagnosis() { assertTrue(Rules.closing(Order(diagnosis="     ",work="Revisión completa",photo="foto.jpg")).containsKey("diagnosis")) }
 @Test fun cannotFinishWithoutEvidence() { assertTrue(Rules.closing(Order(diagnosis="Filtro obstruido",work="Limpieza realizada")).containsKey("photo")) }
 @Test fun completeInterventionCanFinish() { assertTrue(Rules.closing(Order(diagnosis="Filtro obstruido",work="Limpieza realizada",photo="foto.jpg")).isEmpty()) }
 @Test fun cannotSkipVisitOrReopenClosedOrder() { assertFalse(Rules.canMove(Stage.ASSIGNED,Stage.DONE));assertFalse(Rules.canMove(Stage.CLOSED,Stage.ACTIVE)) }
 @Test fun onlySequentialTransitionsAreAllowed() { Stage.entries.forEach { from -> Stage.entries.forEach { to -> assertEquals(to.ordinal==from.ordinal+1,Rules.canMove(from,to)) } } }
}
