package cl.acis.microservicio.dto;

import cl.acis.microservicio.model.Stage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class OrderRequest {
    @PositiveOrZero private Long id;
    @NotBlank @Pattern(regexp = "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    private String clientOrderId;
    @NotBlank @Size(max = 255) private String site;
    @NotBlank @Size(max = 255) private String address;
    @NotBlank @Size(max = 255) private String equipment;
    @NotBlank @Size(max = 255) private String technician;
    @NotBlank @Size(max = 255) private String priority = "Normal";
    @NotBlank @Size(max = 255) private String type = "Preventiva";
    @NotBlank @Size(min = 5, max = 2000) private String problem;
    @NotNull private Stage stage = Stage.ASSIGNED;
    @Size(max = 2000) private String diagnosis = "";
    @Size(max = 2000) private String work = "";
    @Size(max = 2000) private String materials = "";
    @Size(max = 500) private String location = "";
    @Min(0) private long arrival;
    @Min(0) private long departure;
    @Size(max = 200) private String customer = "";
    @Size(max = 2000) private String feedback = "";
    private boolean customerConfirmed;
    @Min(0) @Max(10) private int evidenceCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
