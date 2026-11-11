package marsrover.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import marsrover.model.Action;
import marsrover.model.Direction;
import marsrover.model.Level;
import marsrover.model.Rover;
import marsrover.model.Sensor;
import marsrover.model.Tile;
import marsrover.program.Block.Command;
import marsrover.program.Block.Condition;
import marsrover.program.Block.If;
import marsrover.program.Block.Repeat;
import marsrover.program.Block.While;
import marsrover.program.StepResult.Failure;
import marsrover.program.StepResult.Status;
import org.junit.jupiter.api.Test;

class InterpreterTest {

    // ---- tiny DSL ------------------------------------------------------------------------

    static Level level(int battery, int x, int y, Direction dir, String... rows) {
        return new Level("t", "Test", "", List.of(rows), new Rover(x, y, dir), battery,
                List.of(), OptionalInt.empty(), new Level.StarThresholds(1, 1), List.of());
    }

    static Block vor() {
        return new Command(Action.VOR);
    }

    static Block links() {
        return new Command(Action.LINKS_UM);
    }

    static Block rechts() {
        return new Command(Action.RECHTS_UM);
    }

    static Block probe() {
        return new Command(Action.PROBE_NEHMEN);
    }

    static Block repeat(int n, Block... body) {
        return new Repeat(n, List.of(body));
    }

    static Block loop(Sensor s, boolean negated, Block... body) {
        return new While(new Condition(s, negated), List.of(body));
    }

    static Block ifThen(Sensor s, Block... then) {
        return new If(new Condition(s, false), List.of(then), false, List.of());
    }

    static Block ifElse(Sensor s, List<Block> then, List<Block> otherwise) {
        return new If(new Condition(s, false), then, true, otherwise);
    }

    /** Steps until finished and returns all results. */
    static List<StepResult> runAll(Level level, Block... blocks) {
        Interpreter in = new Interpreter(level, new Program(List.of(blocks)));
        List<StepResult> results = new ArrayList<>();
        do {
            results.add(in.step());
        } while (!results.getLast().isFinished());
        return results;
    }

    static StepResult last(Level level, Block... blocks) {
        return runAll(level, blocks).getLast();
    }

    // ---- commands ------------------------------------------------------------------------

    @Test
    void sequenceReachesBaseAndReportsEachActionLine() {
        Level lvl = level(10, 0, 0, Direction.E, "GGB");

        List<StepResult> rs = runAll(lvl, vor(), links(), rechts(), vor());

        assertEquals(List.of(1, 2, 3, 4, 0), rs.stream().map(StepResult::line).toList());
        assertEquals(List.of(Status.RUNNING, Status.RUNNING, Status.RUNNING, Status.RUNNING, Status.SUCCESS),
                rs.stream().map(StepResult::status).toList());
        assertEquals(Action.VOR, rs.getFirst().action());
        assertEquals(new Rover(1, 0, Direction.E), rs.getFirst().rover());
        assertEquals(Direction.N, rs.get(1).rover().dir());
        assertEquals(new Rover(2, 0, Direction.E), rs.getLast().rover());
        assertEquals(6, rs.getLast().battery());
        assertEquals(4, rs.getLast().batteryUsed());
    }

    @Test
    void dustCostsTwo() {
        StepResult r = last(level(10, 0, 0, Direction.E, "GDB"), vor(), vor());

        assertEquals(Status.SUCCESS, r.status());
        assertEquals(3, r.batteryUsed());
    }

    @Test
    void rockStopsTheRover() {
        StepResult r = last(level(10, 0, 0, Direction.E, "GRB"), vor());

        assertEquals(Failure.ROCK, r.failure());
        assertEquals(1, r.line());
        assertEquals(new Rover(0, 0, Direction.E), r.rover());
    }

    @Test
    void mapEdgeCountsAsRock() {
        StepResult r = last(level(10, 0, 0, Direction.W, "GGB"), vor());

        assertEquals(Failure.ROCK, r.failure());
    }

    @Test
    void craterSwallowsTheRover() {
        StepResult r = last(level(10, 0, 0, Direction.E, "GCB"), vor(), vor());

        assertEquals(Failure.CRATER, r.failure());
        assertEquals(1, r.line());
        assertEquals(new Rover(1, 0, Direction.E), r.rover());
    }

    @Test
    void collectingASampleClearsTheTile() {
        List<StepResult> rs = runAll(level(10, 0, 0, Direction.E, "GSB"), vor(), probe(), vor());

        assertEquals(Tile.GROUND, rs.get(1).world().tileAt(1, 0));
        assertEquals(0, rs.get(1).samplesLeft());
        assertEquals(Tile.SAMPLE, rs.getFirst().world().tileAt(1, 0), "earlier snapshot unchanged");
        assertEquals(Status.SUCCESS, rs.getLast().status());
    }

    @Test
    void collectingWithoutSampleFails() {
        StepResult r = last(level(10, 0, 0, Direction.E, "GSB"), probe());

        assertEquals(Failure.NO_SAMPLE, r.failure());
        assertEquals(1, r.line());
    }

    @Test
    void emptyBatteryStopsBeforeTheAction() {
        StepResult r = last(level(2, 0, 0, Direction.E, "GGGB"), vor(), vor(), vor());

        assertEquals(Failure.BATTERY_EMPTY, r.failure());
        assertEquals(3, r.line());
        assertEquals(new Rover(2, 0, Direction.E), r.rover());
        assertEquals(0, r.battery());
    }

    @Test
    void dustNeedsTwoBatteryLeft() {
        StepResult r = last(level(2, 0, 0, Direction.E, "GGDB"), vor(), vor());

        assertEquals(Failure.BATTERY_EMPTY, r.failure());
        assertEquals(2, r.line());
    }

    // ---- program end ---------------------------------------------------------------------

    @Test
    void samplesLeftAtEnd() {
        StepResult r = last(level(10, 0, 0, Direction.E, "SGB"), vor(), vor());

        assertEquals(Failure.SAMPLES_LEFT, r.failure());
        assertEquals(1, r.samplesLeft());
        assertEquals(0, r.line());
    }

    @Test
    void notAtBaseAtEnd() {
        StepResult r = last(level(10, 0, 0, Direction.E, "GGB"), vor());

        assertEquals(Failure.NOT_AT_BASE, r.failure());
    }

    @Test
    void readyBeforeFirstStepAndIdempotentAfterFinish() {
        Interpreter in = new Interpreter(level(10, 0, 0, Direction.E, "GB"), new Program(List.of(vor())));

        assertEquals(Status.READY, in.current().status());
        assertNull(in.current().action());
        in.step();
        StepResult end = in.step();
        assertEquals(Status.SUCCESS, end.status());
        assertSame(end, in.step());
    }

    // ---- control structures ----------------------------------------------------------------

    @Test
    void forLoopRepeatsBodyAndHighlightsBodyLine() {
        List<StepResult> rs = runAll(level(10, 0, 0, Direction.E, "GGGB"), repeat(3, vor()));

        assertEquals(List.of(2, 2, 2, 0), rs.stream().map(StepResult::line).toList());
        assertEquals(Status.SUCCESS, rs.getLast().status());
    }

    @Test
    void nestedForLoops() {
        // 2 × (2 × vor) = 4 steps
        StepResult r = last(level(10, 0, 0, Direction.E, "GGGGB"), repeat(2, repeat(2, vor())));

        assertEquals(Status.SUCCESS, r.status());
        assertEquals(4, r.batteryUsed());
    }

    @Test
    void whileVornFreiStopsAtRock() {
        StepResult r = last(level(20, 0, 0, Direction.E, "GGGGBR"), loop(Sensor.VORN_FREI, false, vor()));

        assertEquals(Status.SUCCESS, r.status());
        assertEquals(new Rover(4, 0, Direction.E), r.rover());
    }

    @Test
    void whileVornFreiStopsBeforeCrater() {
        StepResult r = last(level(20, 0, 0, Direction.E, "GGCB"), loop(Sensor.VORN_FREI, false, vor()));

        assertEquals(Failure.NOT_AT_BASE, r.failure());
        assertEquals(new Rover(1, 0, Direction.E), r.rover());
    }

    @Test
    void whileNotAmZiel() {
        StepResult r = last(level(20, 0, 0, Direction.E, "GGGB"), loop(Sensor.AM_ZIEL, true, vor()));

        assertEquals(Status.SUCCESS, r.status());
        assertEquals(3, r.batteryUsed());
    }

    @Test
    void whileFalseAtStartSkipsBody() {
        StepResult r = last(level(20, 0, 0, Direction.E, "BR"), loop(Sensor.VORN_FREI, false, vor()));

        assertEquals(Status.SUCCESS, r.status());
        assertEquals(0, r.batteryUsed());
    }

    @Test
    void ifElseDrivesAroundCrater() {
        // Row 0: G C G B ; Row 1: G G G R  -> dodge the crater through row 1
        Level lvl = level(30, 0, 0, Direction.E, "GCGB", "GGGR");
        Block dodge = ifElse(Sensor.VORN_KRATER,
                List.of(rechts(), vor(), links(), vor(), vor(), links(), vor(), rechts()),
                List.of(vor()));

        StepResult r = last(lvl, dodge, vor());

        assertEquals(Status.SUCCESS, r.status());
        assertEquals(new Rover(3, 0, Direction.E), r.rover());
    }

    @Test
    void ifWithoutElseSkipsWhenFalse() {
        StepResult r = last(level(10, 0, 0, Direction.E, "GB"), ifThen(Sensor.VORN_KRATER, links()), vor());

        assertEquals(Status.SUCCESS, r.status());
        assertEquals(1, r.batteryUsed());
    }

    @Test
    void probeDaSensor() {
        StepResult r = last(level(10, 0, 0, Direction.E, "SGB"),
                ifThen(Sensor.PROBE_DA, probe()), vor(), ifThen(Sensor.PROBE_DA, probe()), vor());

        assertEquals(Status.SUCCESS, r.status());
    }

    // ---- endless loop guard ------------------------------------------------------------------

    @Test
    void emptyWhileBodyHitsTheGuard() {
        StepResult r = last(level(10, 0, 0, Direction.E, "GB"), loop(Sensor.AM_ZIEL, true));

        assertEquals(Failure.ENDLESS_LOOP, r.failure());
        assertEquals(1, r.line());
    }

    @Test
    void spinningForeverHitsTheGuardBeforeBattery() {
        StepResult r = last(level(100_000, 0, 0, Direction.E, "GB"), loop(Sensor.VORN_FREI, false, links(), rechts()));

        assertEquals(Failure.ENDLESS_LOOP, r.failure());
    }

    @Test
    void longButFiniteProgramIsFine() {
        // 20 × 20 = 400 turns + condition/loop bookkeeping stays under the guard
        StepResult r = last(level(1000, 0, 0, Direction.E, "B"), repeat(20, repeat(20, links())));

        assertEquals(Status.SUCCESS, r.status());
    }
}
