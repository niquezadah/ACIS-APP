package cl.acis.microservicio.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StageTest {
    @Test
    void permitsOnlyTheNextAcisStage() {
        assertTrue(Stage.ASSIGNED.canMoveTo(Stage.ACCEPTED));
        assertTrue(Stage.ACCEPTED.canMoveTo(Stage.ACTIVE));
        assertTrue(Stage.ACTIVE.canMoveTo(Stage.DONE));
        assertTrue(Stage.DONE.canMoveTo(Stage.CLOSED));
        assertFalse(Stage.ASSIGNED.canMoveTo(Stage.ACTIVE));
        assertFalse(Stage.DONE.canMoveTo(Stage.ACTIVE));
        assertFalse(Stage.CLOSED.canMoveTo(Stage.CLOSED));
    }
}
