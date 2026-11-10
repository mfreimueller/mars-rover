package marsrover.view;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import marsrover.program.Program;
import marsrover.program.Program.CodeLine;

/** Read-only listing of a program with line numbers and a highlighted current line. */
public final class CodeView extends VBox {

    static final double INDENT = 36;

    private final List<HBox> rows = new ArrayList<>();

    public CodeView() {
        getStyleClass().add("code-view");
    }

    public void show(Program program) {
        getChildren().clear();
        rows.clear();
        for (CodeLine line : program.lines()) {
            Label number = new Label(String.valueOf(line.number()));
            number.getStyleClass().add("line-number");
            number.setMinWidth(44);
            number.setAlignment(Pos.CENTER_RIGHT);

            Label text = new Label(line.text());
            text.getStyleClass().add("code-text");
            text.setPadding(new Insets(0, 0, 0, line.indent() * INDENT));

            HBox row = new HBox(16, number, text);
            row.getStyleClass().add("code-line");
            row.setAlignment(Pos.CENTER_LEFT);
            rows.add(row);
        }
        getChildren().addAll(rows);
    }

    /** Highlights a 1-based line; 0 clears the highlight. */
    public void highlight(int line) {
        for (int i = 0; i < rows.size(); i++) {
            HBox row = rows.get(i);
            row.getStyleClass().remove("current");
            row.getStyleClass().remove("failed");
            if (i + 1 == line) {
                row.getStyleClass().add("current");
            }
        }
    }

    /** Marks a 1-based line as the cause of a failure. */
    public void markFailed(int line) {
        highlight(0);
        if (line >= 1 && line <= rows.size()) {
            rows.get(line - 1).getStyleClass().add("failed");
        }
    }
}
