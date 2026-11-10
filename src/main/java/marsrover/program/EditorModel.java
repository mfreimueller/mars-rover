package marsrover.program;

import java.util.ArrayList;
import java.util.List;
import marsrover.model.Action;
import marsrover.model.PaletteEntry;
import marsrover.program.Block.Command;
import marsrover.program.Block.Condition;
import marsrover.program.Block.If;
import marsrover.program.Block.Repeat;
import marsrover.program.Block.While;

/**
 * The editable program behind the Parsons editor. Pure Java so it can be unit-tested.
 *
 * <p>The program is shown as rows (one per code line). Every body sits between a header row and
 * a closing {@code }} row, so each gap between two rows is exactly one insertion point:
 * {@link #gaps()} has {@code rows().size() + 1} entries, gap {@code i} lies above row {@code i}.
 */
public final class EditorModel {

    public enum Kind { COMMAND, REPEAT, WHILE, IF }

    public enum RowKind { STATEMENT, HEADER, ELSE, CLOSE }

    /** A block in the editor. Identity matters: two {@code vor();} blocks are different nodes. */
    public static final class Node {
        private final Kind kind;
        private final Action action;
        private final Condition cond;
        private final boolean hasElse;
        private int count;
        private final List<Node> body = new ArrayList<>();
        private final List<Node> otherwise = new ArrayList<>();

        private Node(Kind kind, Action action, Condition cond, boolean hasElse, int count) {
            this.kind = kind;
            this.action = action;
            this.cond = cond;
            this.hasElse = hasElse;
            this.count = count;
        }

        public Kind kind() {
            return kind;
        }

        public int count() {
            return count;
        }
    }

    /** One displayed line. {@code text} is identical to {@link Program#lines()}. */
    public record Row(int line, int indent, String text, Node node, RowKind kind) {}

    /** An insertion point: index in the body (or else-branch) of {@code parent}; null parent = top level. */
    public record Slot(Node parent, boolean otherwise, int index, int indent) {}

    private final List<Node> root = new ArrayList<>();
    private Program program;
    private List<Row> rows;
    private List<Slot> gaps;

    public EditorModel(List<Block> initial) {
        initial.forEach(b -> root.add(fromBlock(b)));
        rebuild();
    }

    public Program program() {
        return program;
    }

    public List<Row> rows() {
        return rows;
    }

    public List<Slot> gaps() {
        return gaps;
    }

    public int blockCount() {
        return program.blockCount();
    }

    // ---- editing ----------------------------------------------------------------------------

    /** Inserts a fresh block from the palette. */
    public Node insert(PaletteEntry entry, Slot slot) {
        Node node = fromBlock(Block.fromPalette(entry));
        listOf(slot).add(Math.clamp(slot.index(), 0, listOf(slot).size()), node);
        rebuild();
        return node;
    }

    /** Appends a fresh block at the end of the top level. */
    public Node append(PaletteEntry entry) {
        return insert(entry, new Slot(null, false, root.size(), 0));
    }

    /**
     * Moves a block (with its body) to a slot. Returns false and changes nothing if the slot is
     * inside the block itself.
     */
    public boolean move(Node node, Slot slot) {
        if (slot.parent() == node || (slot.parent() != null && contains(node, slot.parent()))) {
            return false;
        }
        List<Node> from = containing(node);
        List<Node> to = listOf(slot);
        int oldIndex = from.indexOf(node);
        int index = slot.index();
        if (from == to && index > oldIndex) {
            index--; // removing the node shifts later positions up
        }
        from.remove(oldIndex);
        to.add(Math.clamp(index, 0, to.size()), node);
        rebuild();
        return true;
    }

    /** Removes a block and its body. */
    public void remove(Node node) {
        List<Node> list = containing(node);
        if (list != null) {
            list.remove(node);
            rebuild();
        }
    }

    /** Sets a for-loop count, clamped to the allowed range. */
    public void setCount(Node node, int count) {
        if (node.kind == Kind.REPEAT) {
            node.count = Math.clamp(count, PaletteEntry.MIN_COUNT, PaletteEntry.MAX_COUNT);
            rebuild();
        }
    }

    // ---- internals --------------------------------------------------------------------------

    private List<Node> listOf(Slot slot) {
        if (slot.parent() == null) {
            return root;
        }
        return slot.otherwise() ? slot.parent().otherwise : slot.parent().body;
    }

    /** The list that directly holds {@code node}, or null. */
    private List<Node> containing(Node node) {
        return containing(root, node);
    }

    private static List<Node> containing(List<Node> list, Node node) {
        for (Node n : list) {
            if (n == node) {
                return list;
            }
            List<Node> found = containing(n.body, node);
            if (found == null) {
                found = containing(n.otherwise, node);
            }
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** True if {@code candidate} is somewhere inside {@code ancestor}'s bodies. */
    private static boolean contains(Node ancestor, Node candidate) {
        return containing(ancestor.body, candidate) != null || containing(ancestor.otherwise, candidate) != null;
    }

    private void rebuild() {
        program = new Program(toBlocks(root));
        rows = new ArrayList<>();
        gaps = new ArrayList<>();
        layout(root, null, false, 0);
        rows = List.copyOf(rows);
        gaps = List.copyOf(gaps);
    }

    private void layout(List<Node> list, Node parent, boolean otherwise, int indent) {
        for (int i = 0; i < list.size(); i++) {
            gaps.add(new Slot(parent, otherwise, i, indent));
            Node n = list.get(i);
            switch (n.kind) {
                case COMMAND -> addRow(indent, n, RowKind.STATEMENT);
                case REPEAT, WHILE -> {
                    addRow(indent, n, RowKind.HEADER);
                    layout(n.body, n, false, indent + 1);
                    addRow(indent, n, RowKind.CLOSE);
                }
                case IF -> {
                    addRow(indent, n, RowKind.HEADER);
                    layout(n.body, n, false, indent + 1);
                    if (n.hasElse) {
                        addRow(indent, n, RowKind.ELSE);
                        layout(n.otherwise, n, true, indent + 1);
                    }
                    addRow(indent, n, RowKind.CLOSE);
                }
            }
        }
        gaps.add(new Slot(parent, otherwise, list.size(), indent));
    }

    private void addRow(int indent, Node node, RowKind kind) {
        int line = rows.size() + 1;
        rows.add(new Row(line, indent, program.lineText(line), node, kind));
    }

    private static List<Block> toBlocks(List<Node> nodes) {
        List<Block> blocks = new ArrayList<>();
        for (Node n : nodes) {
            blocks.add(switch (n.kind) {
                case COMMAND -> new Command(n.action);
                case REPEAT -> new Repeat(n.count, toBlocks(n.body));
                case WHILE -> new While(n.cond, toBlocks(n.body));
                case IF -> new If(n.cond, toBlocks(n.body), n.hasElse, toBlocks(n.otherwise));
            });
        }
        return blocks;
    }

    private static Node fromBlock(Block block) {
        Node node = switch (block) {
            case Command c -> new Node(Kind.COMMAND, c.action(), null, false, 0);
            case Repeat r -> new Node(Kind.REPEAT, null, null, false, r.count());
            case While w -> new Node(Kind.WHILE, null, w.cond(), false, 0);
            case If i -> new Node(Kind.IF, null, i.cond(), i.hasElse(), 0);
        };
        switch (block) {
            case Repeat r -> r.body().forEach(b -> node.body.add(fromBlock(b)));
            case While w -> w.body().forEach(b -> node.body.add(fromBlock(b)));
            case If i -> {
                i.then().forEach(b -> node.body.add(fromBlock(b)));
                i.otherwise().forEach(b -> node.otherwise.add(fromBlock(b)));
            }
            case Command c -> { }
        }
        return node;
    }
}
