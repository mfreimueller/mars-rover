package marsrover.view;

import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import marsrover.model.Direction;
import marsrover.session.NameFilter;

/** Title, name entry and (from T10) the leaderboard. */
public final class StartScreen extends BorderPane {

    private final TextField nameField = new TextField();
    private final Label error = new Label();

    /**
     * @param side      shown on the right, e.g. the leaderboard; may be null
     * @param onStart   called with the accepted name
     */
    public StartScreen(NameFilter filter, Node side, Consumer<String> onStart) {
        getStyleClass().add("start-screen");

        Label title = new Label(Texts.GAME_TITLE);
        title.getStyleClass().add("start-title");
        Label subtitle = new Label(Texts.GAME_SUBTITLE);
        subtitle.getStyleClass().add("start-subtitle");
        subtitle.setWrapText(true);

        Label prompt = new Label(Texts.NAME_PROMPT);
        prompt.getStyleClass().add("start-prompt");

        nameField.setPromptText(Texts.NAME_PLACEHOLDER);
        nameField.getStyleClass().add("name-field");
        nameField.setPrefColumnCount(NameFilter.MAX_LENGTH);
        nameField.setMaxWidth(520);
        // Typing beyond the limit is simply ignored.
        nameField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= NameFilter.MAX_LENGTH ? change : null));

        Button start = new Button(Texts.START_MISSION);
        start.getStyleClass().addAll("run", "start-button");
        start.setDefaultButton(true);

        Runnable submit = () -> {
            NameFilter.Result r = filter.check(nameField.getText());
            if (r.ok()) {
                onStart.accept(r.name());
            } else {
                error.setText(r.error());
                nameField.requestFocus();
            }
        };
        start.setOnAction(e -> submit.run());
        nameField.setOnAction(e -> submit.run());
        nameField.textProperty().addListener((obs, old, now) -> error.setText(""));

        error.getStyleClass().add("name-error");

        HBox entry = new HBox(20, nameField, start);
        entry.setAlignment(Pos.CENTER_LEFT);

        VBox main = new VBox(28,
                WorldCanvas.roverIcon(180, Direction.E), title, subtitle, prompt, entry, error);
        main.setAlignment(Pos.CENTER_LEFT);
        main.setPadding(new Insets(0, 0, 0, 140));
        main.setMaxWidth(1000);
        setCenter(main);
        BorderPane.setAlignment(main, Pos.CENTER_LEFT);

        if (side != null) {
            setRight(side);
            BorderPane.setAlignment(side, Pos.CENTER);
            BorderPane.setMargin(side, new Insets(0, 120, 0, 0));
        }
        Platform.runLater(nameField::requestFocus);
    }
}
