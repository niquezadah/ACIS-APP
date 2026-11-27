package cl.acis.microservicio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ordenes")
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null is allowed only for legacy rows created before client idempotency existed.
    @Column(name = "client_order_id", length = 36, unique = true)
    private String clientOrderId;

    @Column(nullable = false)
    private String site;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private String equipment;
    @Column(nullable = false)
    private String technician;
    @Column(nullable = false)
    private String priority;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false, length = 2000)
    private String problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stage stage;

    @Column(nullable = false, length = 2000)
    private String diagnosis = "";
    @Column(nullable = false, length = 2000)
    private String work = "";
    @Column(nullable = false, length = 2000)
    private String materials = "";
    @Column(nullable = false, length = 500)
    private String location = "";
    @Column(nullable = false)
    private long arrival;
    @Column(nullable = false)
    private long departure;
    @Column(nullable = false, length = 200)
    private String customer = "";
    @Column(nullable = false, length = 2000)
    private String feedback = "";
    @Column(nullable = false)
    private boolean customerConfirmed;

    // Solo conserva el número declarado de evidencias; nunca rutas ni archivos de imagen.
    @Column(nullable = false)
    private int evidenceCount;

    public WorkOrder() {
    }

    public Long getId() { return id; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    public String getTechnician() { return technician; }
    public void setTechnician(String technician) { this.technician = technician; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }
    public Stage getStage() { return stage; }
    public void setStage(Stage stage) { this.stage = stage; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getWork() { return work; }
    public void setWork(String work) { this.work = work; }
    public String getMaterials() { return materials; }
    public void setMaterials(String materials) { this.materials = materials; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public long getArrival() { return arrival; }
    public void setArrival(long arrival) { this.arrival = arrival; }
    public long getDeparture() { return departure; }
    public void setDeparture(long departure) { this.departure = departure; }
    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public boolean isCustomerConfirmed() { return customerConfirmed; }
    public void setCustomerConfirmed(boolean customerConfirmed) { this.customerConfirmed = customerConfirmed; }
    public int getEvidenceCount() { return evidenceCount; }
    public void setEvidenceCount(int evidenceCount) { this.evidenceCount = evidenceCount; }
}
