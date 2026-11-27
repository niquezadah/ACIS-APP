package cl.acis.microservicio.service;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("No existe la orden " + id);
    }
}
