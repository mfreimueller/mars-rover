package marsrover.program;

import java.util.ArrayList;
import java.util.List;
import marsrover.program.Block.Command;
import marsrover.program.Block.If;
import marsrover.program.Block.Repeat;
import marsrover.program.Block.While;

/**
 * A program laid out as Java source lines. Line numbers start at 1 and are shared by the
 * code view (highlighting) and the {@link Interpreter} (which line just ran / failed).
 */
public final class Program {

    public record CodeLine(int number, int indent, String text) {}

    /** A block plus the line of its header; bodies are already laid out. */
    record Node(Block block, int line, List<Node> body, List<Node> otherwise) {}

    private final List<Block> blocks;
    private final List<CodeLine> lines = new ArrayList<>();
    private final List<Node> root;

    public Program(List<Block> blocks) {
        this.blocks = List.copyOf(blocks);
        this.root = layout(this.blocks, 0, 0);
    }

    public List<Block> blocks() {
        return blocks;
    }

    public List<CodeLine> lines() {
        return List.copyOf(lines);
    }

    /** Text of a line, e.g. {@code "vor();"}, or empty for an unknown line. */
    public String lineText(int number) {
        return number >= 1 && number <= lines.size() ? lines.get(number - 1).text() : "";
    }

    /** Number of blocks; a control block counts 1 and its body counts separately. */
    public int blockCount() {
        return count(blocks);
    }

    List<Node> root() {
        return root;
    }

    private static int count(List<Block> blocks) {
        int n = 0;
        for (Block b : blocks) {
            n += 1 + switch (b) {
                case Command c -> 0;
                case Repeat r -> count(r.body());
                case While w -> count(w.body());
                case If i -> count(i.then()) + (i.hasElse() ? count(i.otherwise()) : 0);
            };
        }
        return n;
    }

    private List<Node> layout(List<Block> blocks, int indent, int forDepth) {
        List<Node> nodes = new ArrayList<>();
        for (Block b : blocks) {
            nodes.add(switch (b) {
                case Command c -> new Node(b, add(indent, c.action().javaName() + "();"), List.of(), List.of());
                case Repeat r -> {
                    String v = loopVariable(forDepth);
                    int line = add(indent, "for (int " + v + " = 0; " + v + " < " + r.count() + "; " + v + "++) {");
                    List<Node> body = layout(r.body(), indent + 1, forDepth + 1);
                    add(indent, "}");
                    yield new Node(b, line, body, List.of());
                }
                case While w -> {
                    int line = add(indent, "while (" + w.cond().javaText() + ") {");
                    List<Node> body = layout(w.body(), indent + 1, forDepth);
                    add(indent, "}");
                    yield new Node(b, line, body, List.of());
                }
                case If i -> {
                    int line = add(indent, "if (" + i.cond().javaText() + ") {");
                    List<Node> then = layout(i.then(), indent + 1, forDepth);
                    List<Node> otherwise = List.of();
                    if (i.hasElse()) {
                        add(indent, "} else {");
                        otherwise = layout(i.otherwise(), indent + 1, forDepth);
                    }
                    add(indent, "}");
                    yield new Node(b, line, then, otherwise);
                }
            });
        }
        return nodes;
    }

    /** Nested for loops need distinct variables to be valid Java: i, j, k, i3, i4 … */
    private static String loopVariable(int depth) {
        return depth < 3 ? String.valueOf("ijk".charAt(depth)) : "i" + depth;
    }

    private int add(int indent, String text) {
        int number = lines.size() + 1;
        lines.add(new CodeLine(number, indent, text));
        return number;
    }
}
