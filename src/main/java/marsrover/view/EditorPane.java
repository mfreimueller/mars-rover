package marsrover.view;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Color;
import marsrover.model.Level;
import marsrover.model.PaletteEntry;
import marsrover.program.Block;
import marsrover.program.EditorModel;
import marsrover.program.EditorModel.Row;
import marsrover.program.EditorModel.RowKind;
import marsrover.program.EditorModel.Slot;
import marsrover.program.Program;

/**
 * Parsons editor: palette (left panel) and program (right panel).
 * Drag a palette block into the program to add it (or click it to append),
 * drag a program block to move it, drag it back to the palette to delete it.
 */
public final class EditorPane {

    static final double INDENT = 36;

    private final EditorModel model;
    private final Runnable onChange;

    private final VBox palettePanel;
    private final VBox rowsBox = new VBox(2);
    private final Region indicator = new Region();
    private final Label placeholder = new Label(Texts.DROP_HERE);
    private final VBox programPanel;
    private final List<HBox> rowNodes = new ArrayList<>();

    /** What is being dragged: a {@link PaletteEntry} or an {@link EditorModel.Node}. */
    private Object dragged;
    private boolean locked;

    public EditorPane(Level level, List<Block> initial, Runnable onChange) {
        this.model = new EditorModel(initial);
        this.onChange = onChange;
        this.palettePanel = buildPalette(level.palette());
        this.programPanel = buildProgramPanel();
        render();
    }

    public Node palettePanel() {
        return palettePanel;
    }

    public Node programPanel() {
        return programPanel;
    }

    public Program program() {
        return model.program();
    }

    /** While locked (program running), nothing can be dragged or clicked. */
    public void setLocked(boolean locked) {
        this.locked = locked;
        palettePanel.setDisable(locked);
        programPanel.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("locked"), locked);
    }

    /** Highlights a 1-based line; 0 clears. */
    public void highlight(int line) {
        for (int i = 0; i < rowNodes.size(); i++) {
            HBox row = rowNodes.get(i);
            row.getStyleClass().removeAll("current", "failed");
            if (i + 1 == line) {
                row.getStyleClass().add("current");
            }
        }
    }

    public void markFailed(int line) {
        highlight(0);
        if (line >= 1 && line <= rowNodes.size()) {
            rowNodes.get(line - 1).getStyleClass().add("failed");
        }
    }

    // ---- palette ----------------------------------------------------------------------------

    private VBox buildPalette(List<PaletteEntry> entries) {
        Label heading = new Label(Texts.PALETTE);
        heading.getStyleClass().add("panel-heading");
        VBox blocks = new VBox(14);
        for (PaletteEntry entry : entries) {
            Label chip = chip(paletteText(entry), kindClass(entry));
            chip.setOnDragDetected(e -> {
                if (!locked) {
                    startDrag(chip, entry);
                }
                e.consume();
            });
            chip.setOnMouseClicked(e -> {
                if (!locked && e.getButton() == MouseButton.PRIMARY && e.isStillSincePress()) {
                    model.append(entry);
                    changed();
                }
            });
            blocks.getChildren().add(chip);
        }
        VBox panel = new VBox(16, heading, blocks);
        panel.getStyleClass().addAll("panel", "palette");
        panel.setPrefWidth(500);
        panel.setMinWidth(500);

        // Dropping a program block back on the palette deletes it.
        panel.setOnDragOver(e -> {
            if (dragged instanceof EditorModel.Node) {
                e.acceptTransferModes(TransferMode.MOVE);
                panel.getStyleClass().add("trash");
            }
            e.consume();
        });
        panel.setOnDragExited(e -> panel.getStyleClass().remove("trash"));
        panel.setOnDragDropped(e -> {
            if (dragged instanceof EditorModel.Node node) {
                model.remove(node);
                e.setDropCompleted(true);
                changed();
            }
            e.consume();
        });
        return panel;
    }

    static String paletteText(PaletteEntry entry) {
        return switch (entry) {
            case PaletteEntry.ActionEntry a -> a.action().javaName() + "();";
            case PaletteEntry.ForEntry f -> "for (int i = 0; i < " + f.count() + "; i++) { }";
            case PaletteEntry.WhileEntry w -> "while (" + cond(w.sensor().javaName(), w.negated()) + ") { }";
            case PaletteEntry.IfEntry i -> "if (" + cond(i.sensor().javaName(), i.negated()) + ") { }"
                    + (i.withElse() ? " else { }" : "");
        };
    }

    private static String cond(String sensor, boolean negated) {
        return (negated ? "!" : "") + sensor + "()";
    }

    private static String kindClass(PaletteEntry entry) {
        return switch (entry) {
            case PaletteEntry.ActionEntry a -> "block-command";
            case PaletteEntry.ForEntry f -> "block-loop";
            case PaletteEntry.WhileEntry w -> "block-loop";
            case PaletteEntry.IfEntry i -> "block-if";
        };
    }

    private static String kindClass(EditorModel.Kind kind) {
        return switch (kind) {
            case COMMAND -> "block-command";
            case REPEAT, WHILE -> "block-loop";
            case IF -> "block-if";
        };
    }

    private static Label chip(String text, String kindClass) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("block", "code-text", kindClass);
        chip.setMinHeight(48);
        return chip;
    }

    // ---- program ----------------------------------------------------------------------------

    private VBox buildProgramPanel() {
        Label heading = new Label(Texts.PROGRAM);
        heading.getStyleClass().add("panel-heading");

        indicator.getStyleClass().add("drop-indicator");
        indicator.setManaged(false);
        indicator.setVisible(false);
        Pane overlay = new Pane(indicator);
        overlay.setMouseTransparent(true);

        placeholder.getStyleClass().add("placeholder");
        placeholder.setMaxWidth(Double.MAX_VALUE);
        placeholder.setMinHeight(120);
        placeholder.setAlignment(Pos.CENTER);

        StackPane dropArea = new StackPane(rowsBox, overlay);
        dropArea.setAlignment(Pos.TOP_LEFT);
        dropArea.setPadding(new Insets(0, 0, 200, 0)); // room to drop below the last line

        ScrollPane scroll = new ScrollPane(dropArea);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("code-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        dropArea.setOnDragOver(this::onDragOverProgram);
        dropArea.setOnDragExited(e -> indicator.setVisible(false));
        dropArea.setOnDragDropped(this::onDropOnProgram);

        VBox panel = new VBox(12, heading, scroll);
        panel.getStyleClass().addAll("panel", "program");
        panel.setPrefWidth(660);
        panel.setMinWidth(560);
        return panel;
    }

    private void render() {
        rowsBox.getChildren().clear();
        rowNodes.clear();
        if (model.rows().isEmpty()) {
            rowsBox.getChildren().add(placeholder);
            return;
        }
        for (Row row : model.rows()) {
            Label number = new Label(String.valueOf(row.line()));
            number.getStyleClass().add("line-number");
            number.setMinWidth(44);
            number.setAlignment(Pos.CENTER_RIGHT);

            Label chip = chip(row.text(), kindClass(row.node().kind()));
            if (row.kind() == RowKind.CLOSE || row.kind() == RowKind.ELSE) {
                chip.getStyleClass().add("block-brace");
            }
            HBox.setMargin(chip, new Insets(0, 0, 0, row.indent() * INDENT));
            chip.setOnDragDetected(e -> {
                if (!locked) {
                    startDrag(chip, row.node());
                }
                e.consume();
            });

            HBox line = new HBox(14, number, chip);
            line.getStyleClass().add("code-line");
            line.setAlignment(Pos.CENTER_LEFT);
            rowNodes.add(line);
            rowsBox.getChildren().add(line);
        }
    }

    private void startDrag(Label source, Object payload) {
        dragged = payload;
        Dragboard db = source.startDragAndDrop(TransferMode.COPY_OR_MOVE);
        ClipboardContent content = new ClipboardContent();
        content.putString("mars-rover-block");
        db.setContent(content);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        db.setDragView(source.snapshot(params, null));
    }

    private void onDragOverProgram(DragEvent e) {
        if (dragged != null && !locked) {
            e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            int gap = gapAt(e.getSceneX(), e.getSceneY());
            showIndicator(gap);
        }
        e.consume();
    }

    private void onDropOnProgram(DragEvent e) {
        indicator.setVisible(false);
        if (dragged == null || locked) {
            return;
        }
        Slot slot = model.gaps().get(gapAt(e.getSceneX(), e.getSceneY()));
        switch (dragged) {
            case PaletteEntry entry -> model.insert(entry, slot);
            case EditorModel.Node node -> model.move(node, slot);
            default -> { }
        }
        dragged = null;
        e.setDropCompleted(true);
        e.consume();
        changed();
    }

    /** The gap (row boundary) nearest to the mouse. */
    private int gapAt(double sceneX, double sceneY) {
        if (rowNodes.isEmpty()) {
            return 0;
        }
        double y = rowsBox.sceneToLocal(new Point2D(sceneX, sceneY)).getY();
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i <= rowNodes.size(); i++) {
            double d = Math.abs(boundaryY(i) - y);
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }

    /** y of the boundary above row {@code i} (or below the last row). */
    private double boundaryY(int i) {
        if (i == 0) {
            return rowNodes.getFirst().getBoundsInParent().getMinY();
        }
        Bounds above = rowNodes.get(i - 1).getBoundsInParent();
        return i < rowNodes.size() ? (above.getMaxY() + rowNodes.get(i).getBoundsInParent().getMinY()) / 2
                : above.getMaxY();
    }

    private void showIndicator(int gap) {
        double x = 44 + 14 + model.gaps().get(gap).indent() * INDENT;
        double y = rowNodes.isEmpty() ? 4 : boundaryY(gap);
        indicator.resizeRelocate(x, y - 3, Math.max(120, rowsBox.getWidth() - x - 8), 6);
        indicator.setVisible(true);
    }

    private void changed() {
        dragged = null;
        render();
        onChange.run();
    }
}
