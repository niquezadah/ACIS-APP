package cl.acis.app.model

import cl.acis.app.connected.accountError
import cl.acis.app.connected.report
import org.junit.Assert.*
import org.junit.Test

class PlanningTest {
    @Test fun dateCoordinatesAndContractAreValidated() {
        val order=Order(scheduled="2028-02-29",latitude="-36.8",longitude="-73.0")
        assertTrue(planningErrors(order).isEmpty())
        assertEquals(setOf("scheduled","coordinates","sla"),planningErrors(order.copy(scheduled="2026-02-29",latitude="NaN",slaHours=0)).keys)
        assertTrue(planningErrors(order.copy(longitude="")).containsKey("coordinates"))
    }
    @Test fun reportUsesArrivalForTheDeadlineAndEscapesSpreadsheetCells(){
        val o=Order(created=1000,site="=FÓRMULA",slaHours=1)
        assertTrue(serviceDeadline(o,1001).startsWith("Dentro"))
        assertTrue(serviceDeadline(o,4000000).startsWith("Fuera"))
        assertTrue(serviceDeadline(o.copy(arrival=2000),4000000).startsWith("Cumplido"))
        assertTrue(report(listOf(o),2000).contains("'=FÓRMULA"))
    }
    @Test fun rainAndStrongWindFlagOutdoorVisits(){
        assertTrue(cl.acis.app.connected.forecastText(0.1,10.0).startsWith("Revisa"))
        assertTrue(cl.acis.app.connected.forecastText(0.0,40.0).startsWith("Revisa"))
        assertFalse(cl.acis.app.connected.forecastText(0.0,39.0).startsWith("Revisa"))
    }
    @Test fun formsRejectInvalidInput(){
        val email="ana@example.invalid";val pass="correcto1234"
        assertTrue(accountError("Ingresar","a",pass,"","").isNotEmpty())
        assertTrue(accountError("Ingresar",email,"","","").isNotEmpty())
        assertTrue(accountError("Registrarme",email,"corta","Ana","").isNotEmpty())
        assertTrue(accountError("Registrarme",email,pass,"","").isNotEmpty())
        assertTrue(accountError("Recuperar",email,pass,"","").isNotEmpty())
        assertEquals("",accountError("Registrarme",email,pass,"Ana",""))
    }
}
