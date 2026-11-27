package cl.acis.microservicio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.acis.microservicio.dto.OrderRequest;
import cl.acis.microservicio.model.Stage;
import cl.acis.microservicio.model.WorkOrder;
import cl.acis.microservicio.repository.WorkOrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private WorkOrderRepository repository;
    @InjectMocks private OrderService service;

    @Test
    void rejectsSkippedStageWithoutSaving() {
        WorkOrder order = orderAt(Stage.ASSIGNED, false);
        when(repository.findById(7L)).thenReturn(Optional.of(order));
        OrderRequest request = requestAt(Stage.ACTIVE);

        assertThrows(OrderRuleException.class, () -> service.update(7L, request));
    }

    @Test
    void requiresPreviouslySavedCustomerConfirmationToClose() {
        WorkOrder order = orderAt(Stage.DONE, false);
        when(repository.findById(7L)).thenReturn(Optional.of(order));
        OrderRequest request = requestAt(Stage.CLOSED);
        request.setCustomer("Ana");
        request.setCustomerConfirmed(true);
        request.setDiagnosis("Falla identificada");
        request.setWork("Equipo reparado");
        request.setEvidenceCount(1);

        assertThrows(OrderRuleException.class, () -> service.update(7L, request));
    }

    @Test
    void requiresCustomerNameWhenSavingConfirmation() {
        WorkOrder order = orderAt(Stage.DONE, false);
        when(repository.findById(7L)).thenReturn(Optional.of(order));
        OrderRequest request = requestAt(Stage.DONE);
        request.setCustomerConfirmed(true);

        assertThrows(OrderRuleException.class, () -> service.update(7L, request));
    }

    @Test
    void requiresEvidenceAndWorkDetailsForDoneStage() {
        WorkOrder order = orderAt(Stage.ACTIVE, false);
        when(repository.findById(7L)).thenReturn(Optional.of(order));
        OrderRequest request = requestAt(Stage.DONE);
        request.setDiagnosis("Causa determinada");
        request.setWork("Trabajo realizado");
        request.setEvidenceCount(0);

        assertThrows(OrderRuleException.class, () -> service.update(7L, request));
    }

    @Test
    void rejectsChangesToClosedOrders() {
        WorkOrder order = orderAt(Stage.CLOSED, true);
        when(repository.findById(7L)).thenReturn(Optional.of(order));

        assertThrows(OrderRuleException.class, () -> service.update(7L, requestAt(Stage.CLOSED)));
        assertEquals(Stage.CLOSED, order.getStage());
    }

    private static WorkOrder orderAt(Stage stage, boolean confirmed) {
        WorkOrder order = new WorkOrder();
        order.setClientOrderId(CLIENT_ORDER_ID);
        order.setSite("Local ACIS");
        order.setAddress("Av. Central 100");
        order.setEquipment("Equipo 1");
        order.setTechnician("Técnico 1");
        order.setPriority("Normal");
        order.setType("Preventiva");
        order.setProblem("Mantención preventiva");
        order.setStage(stage);
        order.setDiagnosis("Causa determinada");
        order.setWork("Trabajo realizado");
        order.setEvidenceCount(1);
        order.setCustomer("Ana");
        order.setCustomerConfirmed(confirmed);
        return order;
    }

    private static OrderRequest requestAt(Stage stage) {
        OrderRequest request = new OrderRequest();
        request.setClientOrderId(CLIENT_ORDER_ID);
        request.setSite("Local ACIS");
        request.setAddress("Av. Central 100");
        request.setEquipment("Equipo 1");
        request.setTechnician("Técnico 1");
        request.setPriority("Normal");
        request.setType("Preventiva");
        request.setProblem("Mantención preventiva");
        request.setStage(stage);
        request.setDiagnosis("Causa determinada");
        request.setWork("Trabajo realizado");
        request.setEvidenceCount(1);
        return request;
    }

    private static final String CLIENT_ORDER_ID = "a5796a8e-8497-4cc2-bdbf-44f1ab736c71";
}
