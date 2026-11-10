package marsrover.program;

import static marsrover.program.InterpreterTest.links;
import static marsrover.program.InterpreterTest.repeat;
import static marsrover.program.InterpreterTest.vor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import marsrover.model.PaletteEntry;
import marsrover.program.EditorModel.Node;
import marsrover.program.EditorModel.Row;
import marsrover.program.EditorModel.RowKind;
import marsrover.program.EditorModel.Slot;
import org.junit.jupiter.api.Test;

class EditorModelTest {

    private static final PaletteEntry VOR = PaletteEntry.parse("vor");
    private static final PaletteEntry LINKS = PaletteEntry.parse("linksUm");
    private static final PaletteEntry FOR3 = PaletteEntry.parse("for:3");
    private static final PaletteEntry IFELSE = PaletteEntry.parse("ifelse:vornKrater");

    private static List<String> texts(EditorModel m) {
        return m.rows().stream().map(Row::text).toList();
    }

    /** Index-aligned check: one gap more than rows, and row texts equal the program's lines. */
    private static void assertConsistent(EditorModel m) {
        assertEquals(m.rows().size() + 1, m.gaps().size());
        assertEquals(m.program().lines().stream().map(Program.CodeLine::text).toList(), texts(m));
    }

    @Test
    void emptyProgramHasOneGap() {
        EditorModel m = new EditorModel(List.of());

        assertEquals(List.of(), m.rows());
        assertEquals(List.of(new Slot(null, false, 0, 0)), m.gaps());
        assertEquals(0, m.blockCount());
    }

    @Test
    void insertIntoEmptyLoopBody() {
        EditorModel m = new EditorModel(List.of());
        Node loop = m.append(FOR3);
        // rows: "for {", "}"  -> gap 1 is inside the body
        assertEquals(new Slot(loop, false, 0, 1), m.gaps().get(1));

        m.insert(VOR, m.gaps().get(1));

        assertEquals(List.of("for (int i = 0; i < 3; i++) {", "vor();", "}"), texts(m));
        assertEquals(List.of(0, 1, 0), m.rows().stream().map(Row::indent).toList());
        assertEquals(new Program(List.of(repeat(3, vor()))).blocks(), m.program().blocks());
        assertConsistent(m);
    }

    @Test
    void gapAfterClosingBraceIsOutsideTheLoop() {
        EditorModel m = new EditorModel(List.of(repeat(3, vor())));
        // rows: for {, vor, }  -> gap 2 = end of body, gap 3 = after loop
        m.insert(LINKS, m.gaps().get(2));
        m.insert(LINKS, m.gaps().get(4));

        assertEquals(List.of("for (int i = 0; i < 3; i++) {", "vor();", "linksUm();", "}", "linksUm();"), texts(m));
        assertConsistent(m);
    }

    @Test
    void ifElseHasSeparateGapsForBothBranches() {
        EditorModel m = new EditorModel(List.of());
        Node ifNode = m.append(IFELSE);
        // rows: if {, } else {, }
        assertEquals(List.of(RowKind.HEADER, RowKind.ELSE, RowKind.CLOSE),
                m.rows().stream().map(Row::kind).toList());
        assertEquals(new Slot(ifNode, false, 0, 1), m.gaps().get(1));
        assertEquals(new Slot(ifNode, true, 0, 1), m.gaps().get(2));

        m.insert(LINKS, m.gaps().get(2)); // else branch
        m.insert(VOR, m.gaps().get(1));   // then branch

        assertEquals(List.of("if (vornKrater()) {", "vor();", "} else {", "linksUm();", "}"), texts(m));
        assertConsistent(m);
    }

    @Test
    void moveWithinSameListDownwards() {
        EditorModel m = new EditorModel(List.of(vor(), links(), links()));
        Node first = m.rows().getFirst().node();

        assertTrue(m.move(first, m.gaps().get(3))); // to the end

        assertEquals(List.of("linksUm();", "linksUm();", "vor();"), texts(m));
        assertSame(first, m.rows().get(2).node());
    }

    @Test
    void moveWithinSameListUpwards() {
        EditorModel m = new EditorModel(List.of(vor(), vor(), links()));

        m.move(m.rows().get(2).node(), m.gaps().getFirst());

        assertEquals(List.of("linksUm();", "vor();", "vor();"), texts(m));
    }

    @Test
    void moveOntoItsOwnPositionChangesNothing() {
        EditorModel m = new EditorModel(List.of(vor(), links()));

        m.move(m.rows().get(1).node(), m.gaps().get(1));
        m.move(m.rows().get(1).node(), m.gaps().get(2));

        assertEquals(List.of("vor();", "linksUm();"), texts(m));
    }

    @Test
    void moveIntoALoopAndCarryTheBody() {
        EditorModel m = new EditorModel(List.of(vor(), repeat(2, links())));
        Node loop = m.rows().get(1).node();

        m.move(m.rows().getFirst().node(), m.gaps().get(2)); // into loop, before linksUm
        assertEquals(List.of("for (int i = 0; i < 2; i++) {", "vor();", "linksUm();", "}"), texts(m));

        m.move(loop, m.gaps().getFirst()); // loop itself stays together
        assertEquals(4, m.rows().size());
        assertConsistent(m);
    }

    @Test
    void cannotMoveABlockIntoItself() {
        EditorModel m = new EditorModel(List.of(repeat(2, repeat(2, vor()))));
        Node outer = m.rows().getFirst().node();

        assertFalse(m.move(outer, m.gaps().get(1)));  // own body
        assertFalse(m.move(outer, m.gaps().get(2)));  // inner loop's body
        assertEquals(5, m.rows().size());
    }

    @Test
    void removeTakesTheBodyAlong() {
        EditorModel m = new EditorModel(List.of(vor(), repeat(2, vor(), links())));

        m.remove(m.rows().get(1).node());

        assertEquals(List.of("vor();"), texts(m));
        assertEquals(1, m.blockCount());
    }

    @Test
    void setCountIsClamped() {
        EditorModel m = new EditorModel(List.of(repeat(3, vor())));
        Node loop = m.rows().getFirst().node();

        m.setCount(loop, 25);
        assertEquals(20, loop.count());
        m.setCount(loop, 0);
        assertEquals(1, loop.count());
        assertEquals("for (int i = 0; i < 1; i++) {", m.rows().getFirst().text());
    }

    @Test
    void closingRowsBelongToTheirBlock() {
        EditorModel m = new EditorModel(List.of(repeat(2, vor())));

        assertSame(m.rows().getFirst().node(), m.rows().get(2).node());
    }
}
