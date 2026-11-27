package cl.acis.microservicio.dto;

import cl.acis.microservicio.model.Stage;

public record OrderResponse(
        Long id,
        String clientOrderId,
        String site,
        String address,
        String equipment,
        String technician,
        String priority,
        String type,
        String problem,
        Stage stage,
        String diagnosis,
        String work,
        String materials,
        String location,
        long arrival,
        long departure,
        String customer,
        String feedback,
        boolean customerConfirmed,
        int evidenceCount
) {
}
