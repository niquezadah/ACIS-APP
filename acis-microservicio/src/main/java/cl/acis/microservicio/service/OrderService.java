package cl.acis.microservicio.service;

import cl.acis.microservicio.dto.OrderRequest;
import cl.acis.microservicio.dto.OrderResponse;
import cl.acis.microservicio.model.Stage;
import cl.acis.microservicio.model.WorkOrder;
import cl.acis.microservicio.repository.WorkOrderRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {
    private final WorkOrderRepository repository;

    public OrderService(WorkOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(OrderService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return toResponse(requireOrder(id));
    }

    public OrderResponse create(OrderRequest request) {
        if (request.getId() != null && request.getId() != 0L) {
            throw new OrderRuleException("El identificador de una orden nueva lo asigna el servidor");
        }
        if (request.getStage() != Stage.ASSIGNED) {
            throw new OrderRuleException("Una orden nueva debe comenzar en estado ASSIGNED");
        }
        if (request.isCustomerConfirmed() || request.getEvidenceCount() != 0) {
            throw new OrderRuleException("Una orden nueva no puede incluir conformidad ni evidencias");
        }
        validateBaseText(request);

        WorkOrder existing = repository.findByClientOrderId(request.getClientOrderId()).orElse(null);
        if (existing != null) {
            if (matchesSnapshot(existing, request)) {
                return toResponse(existing);
            }
            throw new OrderRuleException("La clave clientOrderId ya fue usada con otros datos");
        }

        WorkOrder order = new WorkOrder();
        order.setClientOrderId(request.getClientOrderId());
        apply(request, order);
        order.setStage(Stage.ASSIGNED);
        order.setCustomerConfirmed(false);
        order.setEvidenceCount(0);
        return toResponse(repository.save(order));
    }

    public OrderResponse update(Long id, OrderRequest request) {
        if (request.getId() != null && !request.getId().equals(id)) {
            throw new OrderRuleException("El identificador del cuerpo debe coincidir con la ruta");
        }
        WorkOrder order = requireOrder(id);
        if (order.getClientOrderId() == null
                || !order.getClientOrderId().equals(request.getClientOrderId())) {
            throw new OrderRuleException("clientOrderId no coincide con la orden remota");
        }
        Stage current = order.getStage();
        Stage requested = request.getStage();

        if (current == Stage.CLOSED) {
            if (requested == Stage.CLOSED && matchesSnapshot(order, request)) {
                return toResponse(order);
            }
            throw new OrderRuleException("Una orden CLOSED es terminal y no se puede modificar");
        }
        if (requested != current && !current.canMoveTo(requested)) {
            throw new OrderRuleException("Transición no permitida: " + current + " -> " + requested);
        }
        validateBaseText(request);
        validateStageRequirements(request);

        if (request.isCustomerConfirmed()
                && requested != Stage.DONE && requested != Stage.CLOSED) {
            throw new OrderRuleException("La conformidad solo se puede registrar cuando la orden está por confirmar");
        }
        if (current != Stage.DONE && requested == Stage.DONE && request.isCustomerConfirmed()) {
            throw new OrderRuleException("Registra la conformidad en una actualización posterior a la visita");
        }
        if (requested == Stage.DONE && request.isCustomerConfirmed()
                && valueOrEmpty(request.getCustomer()).isBlank()) {
            throw new OrderRuleException("Se requiere el nombre de quien confirma");
        }

        if (order.isCustomerConfirmed() && !request.isCustomerConfirmed()) {
            throw new OrderRuleException("La conformidad registrada no se puede retirar");
        }
        if (requested == Stage.CLOSED) {
            if (!order.isCustomerConfirmed()) {
                throw new OrderRuleException("La orden debe tener conformidad guardada antes del cierre");
            }
            if (!request.isCustomerConfirmed() || valueOrEmpty(request.getCustomer()).isBlank()) {
                throw new OrderRuleException("Se requiere la conformidad del cliente para cerrar");
            }
        }

        apply(request, order);
        return toResponse(repository.save(order));
    }

    private WorkOrder requireOrder(Long id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private static void validateBaseText(OrderRequest request) {
        if (valueOrEmpty(request.getProblem()).trim().length() < 5) {
            throw new OrderRuleException("El motivo debe tener al menos 5 caracteres");
        }
    }

    private static void validateStageRequirements(OrderRequest request) {
        if (request.getStage() == Stage.DONE || request.getStage() == Stage.CLOSED) {
            if (valueOrEmpty(request.getDiagnosis()).trim().length() < 5) {
                throw new OrderRuleException("El diagnóstico debe tener al menos 5 caracteres");
            }
            if (valueOrEmpty(request.getWork()).trim().length() < 5) {
                throw new OrderRuleException("El trabajo realizado debe tener al menos 5 caracteres");
            }
            if (request.getEvidenceCount() < 1 || request.getEvidenceCount() > 10) {
                throw new OrderRuleException("Se requiere declarar entre 1 y 10 evidencias fotográficas");
            }
        }
        if (request.getStage() == Stage.CLOSED && valueOrEmpty(request.getCustomer()).isBlank()) {
            throw new OrderRuleException("Se requiere el nombre de quien confirma");
        }
    }

    private static void apply(OrderRequest request, WorkOrder order) {
        order.setSite(request.getSite().trim());
        order.setAddress(request.getAddress().trim());
        order.setEquipment(request.getEquipment().trim());
        order.setTechnician(request.getTechnician().trim());
        order.setPriority(request.getPriority().trim());
        order.setType(request.getType().trim());
        order.setProblem(request.getProblem().trim());
        order.setStage(request.getStage());
        order.setDiagnosis(valueOrEmpty(request.getDiagnosis()));
        order.setWork(valueOrEmpty(request.getWork()));
        order.setMaterials(valueOrEmpty(request.getMaterials()));
        order.setLocation(valueOrEmpty(request.getLocation()));
        order.setArrival(request.getArrival());
        order.setDeparture(request.getDeparture());
        order.setCustomer(valueOrEmpty(request.getCustomer()));
        order.setFeedback(valueOrEmpty(request.getFeedback()));
        order.setCustomerConfirmed(request.isCustomerConfirmed());
        order.setEvidenceCount(request.getEvidenceCount());
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static OrderResponse toResponse(WorkOrder order) {
        return new OrderResponse(
                order.getId(), order.getClientOrderId(), order.getSite(), order.getAddress(), order.getEquipment(),
                order.getTechnician(), order.getPriority(), order.getType(), order.getProblem(),
                order.getStage(), order.getDiagnosis(), order.getWork(), order.getMaterials(),
                order.getLocation(), order.getArrival(), order.getDeparture(), order.getCustomer(),
                order.getFeedback(), order.isCustomerConfirmed(), order.getEvidenceCount());
    }

    private static boolean matchesSnapshot(WorkOrder order, OrderRequest request) {
        return (request.getId() == null || request.getId() == 0L || request.getId().equals(order.getId()))
                && order.getClientOrderId().equals(request.getClientOrderId())
                && order.getSite().equals(valueOrEmpty(request.getSite()))
                && order.getAddress().equals(valueOrEmpty(request.getAddress()))
                && order.getEquipment().equals(valueOrEmpty(request.getEquipment()))
                && order.getTechnician().equals(valueOrEmpty(request.getTechnician()))
                && order.getPriority().equals(valueOrEmpty(request.getPriority()))
                && order.getType().equals(valueOrEmpty(request.getType()))
                && order.getProblem().equals(valueOrEmpty(request.getProblem()))
                && order.getStage() == request.getStage()
                && order.getDiagnosis().equals(valueOrEmpty(request.getDiagnosis()))
                && order.getWork().equals(valueOrEmpty(request.getWork()))
                && order.getMaterials().equals(valueOrEmpty(request.getMaterials()))
                && order.getLocation().equals(valueOrEmpty(request.getLocation()))
                && order.getArrival() == request.getArrival()
                && order.getDeparture() == request.getDeparture()
                && order.getCustomer().equals(valueOrEmpty(request.getCustomer()))
                && order.getFeedback().equals(valueOrEmpty(request.getFeedback()))
                && order.isCustomerConfirmed() == request.isCustomerConfirmed()
                && order.getEvidenceCount() == request.getEvidenceCount();
    }
}
