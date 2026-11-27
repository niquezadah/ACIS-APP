package cl.acis.microservicio.model;

public enum Stage {
    ASSIGNED,
    ACCEPTED,
    ACTIVE,
    DONE,
    CLOSED;

    public boolean canMoveTo(Stage next) {
        return switch (this) {
            case ASSIGNED -> next == ACCEPTED;
            case ACCEPTED -> next == ACTIVE;
            case ACTIVE -> next == DONE;
            case DONE -> next == CLOSED;
            case CLOSED -> false;
        };
    }
}
