package marsrover.program;

import static marsrover.program.InterpreterTest.ifElse;
import static marsrover.program.InterpreterTest.links;
import static marsrover.program.InterpreterTest.loop;
import static marsrover.program.InterpreterTest.repeat;
import static marsrover.program.InterpreterTest.vor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import marsrover.model.PaletteEntry;
import marsrover.model.Sensor;
import marsrover.program.Program.CodeLine;
import org.junit.jupiter.api.Test;

class ProgramTest {

    @Test
    void laysOutValidIndentedJava() {
        Program p = new Program(List.of(
                repeat(2, repeat(3, vor()), links()),
                loop(Sensor.AM_ZIEL, true,
                        ifElse(Sensor.VORN_KRATER, List.of(links()), List.of(vor())))));

        assertEquals(List.of(
                new CodeLine(1, 0, "for (int i = 0; i < 2; i++) {"),
                new CodeLine(2, 1, "for (int j = 0; j < 3; j++) {"),
                new CodeLine(3, 2, "vor();"),
                new CodeLine(4, 1, "}"),
                new CodeLine(5, 1, "linksUm();"),
                new CodeLine(6, 0, "}"),
                new CodeLine(7, 0, "while (!amZiel()) {"),
                new CodeLine(8, 1, "if (vornKrater()) {"),
                new CodeLine(9, 2, "linksUm();"),
                new CodeLine(10, 1, "} else {"),
                new CodeLine(11, 2, "vor();"),
                new CodeLine(12, 1, "}"),
                new CodeLine(13, 0, "}")), p.lines());
        assertEquals("linksUm();", p.lineText(5));
        assertEquals("", p.lineText(99));
    }

    @Test
    void siblingLoopsReuseTheSameVariable() {
        Program p = new Program(List.of(repeat(2, vor()), repeat(3, vor())));

        assertEquals("for (int i = 0; i < 3; i++) {", p.lineText(4));
    }

    @Test
    void countsBlocks() {
        assertEquals(0, new Program(List.of()).blockCount());
        assertEquals(2, new Program(List.of(repeat(8, vor()))).blockCount());
        assertEquals(5, new Program(List.of(
                loop(Sensor.AM_ZIEL, true, ifElse(Sensor.VORN_KRATER, List.of(links()), List.of(vor()))),
                vor())).blockCount());
    }

    @Test
    void paletteEntriesBecomeEmptyBlocks() {
        Program p = new Program(List.of(
                Block.fromPalette(PaletteEntry.parse("for:4")),
                Block.fromPalette(PaletteEntry.parse("while:vornFrei")),
                Block.fromPalette(PaletteEntry.parse("if:!probeDa")),
                Block.fromPalette(PaletteEntry.parse("ifelse:vornKrater")),
                Block.fromPalette(PaletteEntry.parse("probeNehmen"))));

        assertEquals(List.of(
                "for (int i = 0; i < 4; i++) {", "}",
                "while (vornFrei()) {", "}",
                "if (!probeDa()) {", "}",
                "if (vornKrater()) {", "} else {", "}",
                "probeNehmen();"), p.lines().stream().map(CodeLine::text).toList());
    }
}
