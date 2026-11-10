package marsrover.view;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import marsrover.session.Leaderboard;
import marsrover.session.Leaderboard.Entry;

/** Top-10 table. The row at {@code highlightRank} (1-based, or 0) is marked as "you". */
final class LeaderboardView extends VBox {

    LeaderboardView(Leaderboard board, int highlightRank) {
        super(16);
        getStyleClass().addAll("panel", "leaderboard");
        setAlignment(Pos.TOP_LEFT);
        setMinWidth(560);
        setMaxHeight(USE_PREF_SIZE);

        Label title = new Label(Texts.LEADERBOARD);
        title.getStyleClass().add("leaderboard-title");
        getChildren().add(title);

        List<Entry> top = board.top();
        if (top.isEmpty()) {
            Label empty = new Label(Texts.LEADERBOARD_EMPTY);
            empty.getStyleClass().add("leaderboard-empty");
            empty.setWrapText(true);
            getChildren().add(empty);
            return;
        }

        GridPane table = new GridPane();
        table.setHgap(28);
        table.setVgap(6);
        for (int i = 0; i < top.size(); i++) {
            Entry e = top.get(i);
            Label rank = cell((i + 1) + ".", "lb-rank");
            Label name = cell(e.name(), "lb-name");
            Label stars = cell(e.stars() + " ★", "lb-stars");
            Label time = cell(Texts.clock(e.timeMillis()), "lb-time");
            if (i + 1 == highlightRank) {
                for (Label l : List.of(rank, name, stars, time)) {
                    l.getStyleClass().add("lb-you");
                }
            }
            table.addRow(i, rank, name, stars, time);
        }
        getChildren().add(table);
    }

    private static Label cell(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("lb-cell", styleClass);
        return l;
    }
}
