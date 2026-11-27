package cl.acis.microservicio.service;

public class OrderRuleException extends RuntimeException {
    public OrderRuleException(String message) {
        super(message);
    }
}
