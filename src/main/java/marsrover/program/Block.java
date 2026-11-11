package marsrover.program;

import java.util.List;
import marsrover.model.Action;
import marsrover.model.PaletteEntry;
import marsrover.model.Sensor;
import marsrover.model.SolutionStep;

/** The program AST. Bodies are immutable lists; the editor builds new trees on change. */
public sealed interface Block {

    record Command(Action action) implements Block {}

    record Repeat(int count, List<Block> body) implements Block {
        public Repeat {
            body = List.copyOf(body);
        }
    }

    record While(Condition cond, List<Block> body) implements Block {
        public While {
            body = List.copyOf(body);
        }
    }

    /** {@code otherwise} is only shown and run when {@code hasElse} is true. */
    record If(Condition cond, List<Block> then, boolean hasElse, List<Block> otherwise) implements Block {
        public If {
            then = List.copyOf(then);
            otherwise = List.copyOf(otherwise);
        }
    }

    record Condition(Sensor sensor, boolean negated) {
        public String javaText() {
            return (negated ? "!" : "") + sensor.javaName() + "()";
        }
    }

    /** A new block with empty bodies, as dragged out of the palette. */
    static Block fromPalette(PaletteEntry entry) {
        return switch (entry) {
            case PaletteEntry.ActionEntry a -> new Command(a.action());
            case PaletteEntry.ForEntry f -> new Repeat(f.count(), List.of());
            case PaletteEntry.WhileEntry w -> new While(new Condition(w.sensor(), w.negated()), List.of());
            case PaletteEntry.IfEntry i ->
                    new If(new Condition(i.sensor(), i.negated()), List.of(), i.withElse(), List.of());
        };
    }

    /** A level's reference solution as a program. */
    static List<Block> fromSolution(List<SolutionStep> steps) {
        return steps.stream().map(Block::fromSolution).toList();
    }

    private static Block fromSolution(SolutionStep s) {
        List<Block> body = fromSolution(s.body());
        return switch (fromPalette(s.entry())) {
            case Command c -> c;
            case Repeat r -> new Repeat(r.count(), body);
            case While w -> new While(w.cond(), body);
            case If i -> new If(i.cond(), body, i.hasElse(), fromSolution(s.otherwise()));
        };
    }
}
