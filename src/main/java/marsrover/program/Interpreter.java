package marsrover.program;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import marsrover.model.Action;
import marsrover.model.Level;
import marsrover.model.Rover;
import marsrover.model.Sensor;
import marsrover.model.Tile;
import marsrover.model.World;
import marsrover.program.Block.Command;
import marsrover.program.Block.Condition;
import marsrover.program.Block.If;
import marsrover.program.Block.Repeat;
import marsrover.program.Block.While;
import marsrover.program.Program.Node;
import marsrover.program.StepResult.Failure;
import marsrover.program.StepResult.Status;

/**
 * Runs a {@link Program} on a level, one action per {@link #step()}. No threads, no real Java:
 * the caller decides the pace (Run = timer, Step = key press) and can simply stop calling.
 * For a restart, create a new interpreter.
 */
public final class Interpreter {

    /** Actions, condition checks and loop iterations all count as steps. */
    public static final int MAX_STEPS = 1000;

    /** A body being executed. {@code owner} is the loop that repeats it, or null. */
    private static final class Frame {
        final List<Node> body;
        final Node owner;
        int pc;
        int remaining;

        Frame(List<Node> body, Node owner, int remaining) {
            this.body = body;
            this.owner = owner;
            this.remaining = remaining;
        }
    }

    private final World world;
    private final int startBattery;
    private final Deque<Frame> stack = new ArrayDeque<>();
    private Rover rover;
    private int battery;
    private int steps;
    private StepResult current;

    public Interpreter(Level level, Program program) {
        this.world = level.createWorld();
        this.rover = level.start();
        this.startBattery = level.battery();
        this.battery = level.battery();
        stack.push(new Frame(program.root(), null, 0));
        current = result(Status.READY, 0, null, null);
    }

    /** The latest state; {@link Status#READY} before the first step. */
    public StepResult current() {
        return current;
    }

    /** Runs until the next action has executed or the program ends. Idempotent once finished. */
    public StepResult step() {
        if (current.isFinished()) {
            return current;
        }
        current = advance();
        return current;
    }

    private StepResult advance() {
        while (true) {
            if (stack.isEmpty()) {
                return finish();
            }
            Frame frame = stack.peek();

            if (frame.pc >= frame.body.size()) {
                // End of a body: loop again or leave it.
                Node owner = frame.owner;
                if (owner != null && owner.block() instanceof Repeat && frame.remaining > 0) {
                    if (++steps > MAX_STEPS) {
                        return fail(Failure.ENDLESS_LOOP, owner.line());
                    }
                    frame.remaining--;
                    frame.pc = 0;
                } else if (owner != null && owner.block() instanceof While w) {
                    if (++steps > MAX_STEPS) {
                        return fail(Failure.ENDLESS_LOOP, owner.line());
                    }
                    if (check(w.cond())) {
                        frame.pc = 0;
                    } else {
                        stack.pop();
                    }
                } else {
                    stack.pop();
                }
                continue;
            }

            Node node = frame.body.get(frame.pc++);
            if (++steps > MAX_STEPS) {
                return fail(Failure.ENDLESS_LOOP, node.line());
            }
            switch (node.block()) {
                case Command c -> {
                    return execute(c.action(), node.line());
                }
                case Repeat r -> {
                    if (r.count() > 0) {
                        stack.push(new Frame(node.body(), node, r.count() - 1));
                    }
                }
                case While w -> {
                    if (check(w.cond())) {
                        stack.push(new Frame(node.body(), node, 0));
                    }
                }
                case If i -> {
                    if (check(i.cond())) {
                        stack.push(new Frame(node.body(), null, 0));
                    } else if (i.hasElse()) {
                        stack.push(new Frame(node.otherwise(), null, 0));
                    }
                }
            }
        }
    }

    private StepResult execute(Action action, int line) {
        int cost = 1;
        if (action == Action.VOR && frontInBounds() && world.tileAt(rover.frontX(), rover.frontY()) == Tile.DUST) {
            cost = 2;
        }
        if (cost > battery) {
            return fail(Failure.BATTERY_EMPTY, line);
        }
        battery -= cost;

        switch (action) {
            case VOR -> {
                if (!frontInBounds() || world.tileAt(rover.frontX(), rover.frontY()) == Tile.ROCK) {
                    return fail(Failure.ROCK, line, action);
                }
                rover = rover.forward();
                if (world.tileAt(rover.x(), rover.y()) == Tile.CRATER) {
                    return fail(Failure.CRATER, line, action);
                }
            }
            case LINKS_UM -> rover = rover.turnedLeft();
            case RECHTS_UM -> rover = rover.turnedRight();
            case PROBE_NEHMEN -> {
                if (world.tileAt(rover.x(), rover.y()) != Tile.SAMPLE) {
                    return fail(Failure.NO_SAMPLE, line, action);
                }
                world.setTile(rover.x(), rover.y(), Tile.GROUND);
            }
        }
        return result(Status.RUNNING, line, action, null);
    }

    private boolean check(Condition cond) {
        return sense(cond.sensor()) != cond.negated();
    }

    private boolean sense(Sensor sensor) {
        return switch (sensor) {
            case VORN_FREI -> frontInBounds() && world.tileAt(rover.frontX(), rover.frontY()).isDrivable();
            case VORN_KRATER -> frontInBounds() && world.tileAt(rover.frontX(), rover.frontY()) == Tile.CRATER;
            case PROBE_DA -> world.tileAt(rover.x(), rover.y()) == Tile.SAMPLE;
            case AM_ZIEL -> world.tileAt(rover.x(), rover.y()) == Tile.BASE;
        };
    }

    private boolean frontInBounds() {
        return world.inBounds(rover.frontX(), rover.frontY());
    }

    private StepResult finish() {
        if (world.count(Tile.SAMPLE) > 0) {
            return fail(Failure.SAMPLES_LEFT, 0);
        }
        if (world.tileAt(rover.x(), rover.y()) != Tile.BASE) {
            return fail(Failure.NOT_AT_BASE, 0);
        }
        return result(Status.SUCCESS, 0, null, null);
    }

    private StepResult fail(Failure failure, int line) {
        return fail(failure, line, null);
    }

    private StepResult fail(Failure failure, int line, Action action) {
        return result(Status.FAILED, line, action, failure);
    }

    private StepResult result(Status status, int line, Action action, Failure failure) {
        return new StepResult(status, line, action, rover, world.copy(), battery,
                startBattery - battery, failure, world.count(Tile.SAMPLE));
    }
}
