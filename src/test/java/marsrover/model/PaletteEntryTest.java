package marsrover.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import marsrover.model.PaletteEntry.ActionEntry;
import marsrover.model.PaletteEntry.ForEntry;
import marsrover.model.PaletteEntry.IfEntry;
import marsrover.model.PaletteEntry.WhileEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PaletteEntryTest {

    @Test
    void parsesAllKinds() {
        assertEquals(new ActionEntry(Action.VOR), PaletteEntry.parse("vor"));
        assertEquals(new ActionEntry(Action.RECHTS_UM), PaletteEntry.parse("rechtsUm"));
        assertEquals(new ForEntry(3), PaletteEntry.parse("for:3"));
        assertEquals(new WhileEntry(Sensor.VORN_FREI, false), PaletteEntry.parse("while:vornFrei"));
        assertEquals(new WhileEntry(Sensor.AM_ZIEL, true), PaletteEntry.parse("while:!amZiel"));
        assertEquals(new IfEntry(Sensor.VORN_KRATER, false, false), PaletteEntry.parse("if:vornKrater"));
        assertEquals(new IfEntry(Sensor.PROBE_DA, true, true), PaletteEntry.parse("ifelse:!probeDa"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Vor", "vor()", "for:0", "for:x", "for:", "while:", "while:!", "loop:3", "if:vor"})
    void rejectsInvalidEntries(String text) {
        assertThrows(IllegalArgumentException.class, () -> PaletteEntry.parse(text));
    }

    @Test
    void directionsTurnAround() {
        assertEquals(Direction.W, Direction.N.left());
        assertEquals(Direction.E, Direction.N.right());
        assertEquals(Direction.N, Direction.W.right());
        assertEquals(Direction.S, Direction.W.left());
    }
}
