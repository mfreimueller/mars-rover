package marsrover.program;

import static marsrover.program.InterpreterTest.links;
import static marsrover.program.InterpreterTest.loop;
import static marsrover.program.InterpreterTest.probe;
import static marsrover.program.InterpreterTest.rechts;
import static marsrover.program.InterpreterTest.repeat;
import static marsrover.program.InterpreterTest.vor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import marsrover.model.Level;
import marsrover.model.LevelLoader;
import marsrover.model.PaletteEntry;
import marsrover.model.Sensor;
import marsrover.model.Stars;
import marsrover.program.Block.Command;
import marsrover.program.Block.Condition;
import marsrover.program.Block.If;
import marsrover.program.Block.Repeat;
import marsrover.program.Block.While;
import marsrover.program.StepResult.Status;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The shipped levels in {@code levels/} load and can be solved for 3 stars with their own palette. */
class DefaultLevelsTest {

    private static Map<String, Level> levels;

    @BeforeAll
    static void load() {
        LevelLoader.Result result = LevelLoader.loadFolder(Path.of("levels"));
        assertEquals(List.of(), result.errors());
        assertEquals(List.of("01-landing", "02-long-road", "03-crater-field", "04-unknown-distance"),
                result.levels().stream().map(Level::id).toList());
        levels = result.levels().stream()
                .collect(java.util.stream.Collectors.toMap(Level::id, l -> l));
    }

    private static Block ifKrater(Block... then) {
        return new If(new Condition(Sensor.VORN_KRATER, false), List.of(then), false, List.of());
    }

    private static final Map<String, List<Block>> SOLUTIONS = Map.of(
            "01-landing", List.of(vor(), vor(), probe(), links(), vor(), vor()),
            "02-long-road", List.of(repeat(9, vor())),
            "03-crater-field", List.of(repeat(6, ifKrater(links(), vor(), rechts()), vor())),
            "04-unknown-distance", List.of(
                    loop(Sensor.VORN_FREI, false, vor()), probe(), rechts(), loop(Sensor.VORN_FREI, false, vor())));

    @Test
    void everyLevelHasAThreeStarSolutionFromItsPalette() {
        for (var e : SOLUTIONS.entrySet()) {
            Level level = levels.get(e.getKey());
            Program program = new Program(e.getValue());
            assertUsesOnlyPalette(level, program.blocks());

            StepResult r = run(level, program);

            assertEquals(Status.SUCCESS, r.status(), level.id() + " failed: " + r.failure());
            assertEquals(3, Stars.rate(true, r.batteryUsed(), program.blockCount(), level.stars()),
                    level.id() + ": battery " + r.batteryUsed() + ", blocks " + program.blockCount());
            level.maxBlocks().ifPresent(max -> assertTrue(program.blockCount() <= max, level.id()));
        }
    }

    @Test
    void longRoadNeedsALoop() {
        Level level = levels.get("02-long-road");
        // 9 × vor() solves the map but does not fit into maxBlocks
        assertTrue(9 > level.maxBlocks().getAsInt());
    }

    @Test
    void craterFieldNeedsLoopAndIf() {
        Level level = levels.get("03-crater-field");
        List<Block> straight = List.of(
                vor(), links(), vor(), rechts(), vor(), vor(), links(), vor(), rechts(),
                vor(), vor(), links(), vor(), rechts(), vor());
        assertEquals(Status.SUCCESS, run(level, new Program(straight)).status(), "map is solvable at all");
        assertTrue(straight.size() > level.maxBlocks().getAsInt(), "but not without a loop");
        assertTrue(Collections.disjoint(List.of(new PaletteEntry.WhileEntry(Sensor.VORN_FREI, false)), level.palette()));
    }

    @Test
    void unknownDistanceOffersNoForLoop() {
        assertTrue(levels.get("04-unknown-distance").palette().stream()
                .noneMatch(p -> p instanceof PaletteEntry.ForEntry));
    }

    @Test
    void walkingIntoTheCraterFails() {
        StepResult r = run(levels.get("03-crater-field"), new Program(List.of(repeat(6, vor()))));

        assertEquals(StepResult.Failure.CRATER, r.failure());
    }

    private static StepResult run(Level level, Program program) {
        Interpreter in = new Interpreter(level, program);
        StepResult r;
        do {
            r = in.step();
        } while (!r.isFinished());
        return r;
    }

    /** Every block kind must be offered by the palette; for-loop counts are editable, so any count is fine. */
    private static void assertUsesOnlyPalette(Level level, List<Block> blocks) {
        for (Block b : blocks) {
            PaletteEntry needed = switch (b) {
                case Command c -> new PaletteEntry.ActionEntry(c.action());
                case Repeat r -> null;
                case While w -> new PaletteEntry.WhileEntry(w.cond().sensor(), w.cond().negated());
                case If i -> new PaletteEntry.IfEntry(i.cond().sensor(), i.cond().negated(), i.hasElse());
            };
            if (needed == null) {
                assertTrue(level.palette().stream().anyMatch(p -> p instanceof PaletteEntry.ForEntry),
                        level.id() + " has no for in its palette");
            } else {
                assertTrue(level.palette().contains(needed), level.id() + " palette lacks " + needed);
            }
            switch (b) {
                case Repeat r -> assertUsesOnlyPalette(level, r.body());
                case While w -> assertUsesOnlyPalette(level, w.body());
                case If i -> {
                    assertUsesOnlyPalette(level, i.then());
                    assertUsesOnlyPalette(level, i.otherwise());
                }
                case Command c -> { }
            }
        }
    }
}
