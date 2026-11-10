package marsrover.view;

import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import marsrover.model.Level;
import marsrover.session.Session;

/** Session summary: stars per level, total and time. */
public final class EndScreen extends BorderPane {

    public EndScreen(Session session, Session.Result result, List<Level> levels, Node side, Runnable onDone) {
        getStyleClass().add("end-screen");

        Label title = new Label(Texts.missionComplete(session.name()));
        title.getStyleClass().add("start-title");

        GridPane table = new GridPane();
        table.setHgap(40);
        table.setVgap(10);
        for (int i = 0; i < levels.size(); i++) {
            Level level = levels.get(i);
            Label name = new Label((i + 1) + ".  " + level.title());
            name.getStyleClass().add("end-level");
            Label stars = new Label(Texts.stars(session.stars(level.id())));
            stars.getStyleClass().add("end-stars");
            table.addRow(i, name, stars);
        }

        Label total = new Label(Texts.totalStars(result.stars(), levels.size() * 3));
        total.getStyleClass().add("end-total");
        Label time = new Label(Texts.time(result.timeMillis()));
        time.getStyleClass().add("end-time");

        Button done = new Button(Texts.DONE);
        done.getStyleClass().add("run");
        done.setDefaultButton(true);
        done.setOnAction(e -> onDone.run());

        VBox main = new VBox(28, title, table, total, time, done);
        main.setAlignment(Pos.CENTER_LEFT);
        main.setPadding(new Insets(0, 0, 0, 140));
        setCenter(main);

        if (side != null) {
            setRight(side);
            BorderPane.setAlignment(side, Pos.CENTER);
            BorderPane.setMargin(side, new Insets(0, 120, 0, 0));
        }
    }
}
