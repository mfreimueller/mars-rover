package marsrover.view;

import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import marsrover.model.Level;

/**
 * Success card on top of the level: stars pop in one after another.
 * After the solution was shown there are no stars, just a friendly note.
 */
final class ResultOverlay extends StackPane {

    ResultOverlay(int stars, int batteryUsed, int blocks, Level.StarThresholds targets, boolean withSolution,
                  String nextLabel, Runnable onAgain, Runnable onNext) {
        getStyleClass().add("result-overlay");

        Label title = new Label(withSolution ? Texts.SUCCESS_WITH_SOLUTION : Texts.SUCCESS);
        title.getStyleClass().add("result-title");

        HBox starRow = new HBox(24);
        starRow.setAlignment(Pos.CENTER);
        SequentialTransition pop = new SequentialTransition();
        for (int i = 0; i < 3; i++) {
            Label star = new Label("★");
            star.getStyleClass().addAll("star", i < stars ? "star-on" : "star-off");
            star.setScaleX(0);
            star.setScaleY(0);
            ScaleTransition t = new ScaleTransition(Duration.millis(280), star);
            t.setFromX(0);
            t.setFromY(0);
            t.setToX(1);
            t.setToY(1);
            pop.getChildren().add(t);
            starRow.getChildren().add(star);
        }

        Label battery = detail(Texts.batteryTarget(batteryUsed, targets.battery()), batteryUsed <= targets.battery());
        Label blockLine = detail(Texts.blocksTarget(blocks, targets.blocks()), blocks <= targets.blocks());

        Button again = new Button(Texts.AGAIN);
        again.getStyleClass().add("reset");
        again.setOnAction(e -> onAgain.run());
        Button next = new Button(nextLabel);
        next.getStyleClass().add("run");
        next.setOnAction(e -> onNext.run());
        next.setDefaultButton(true);
        HBox buttons = new HBox(20, again, next);
        buttons.setAlignment(Pos.CENTER);

        VBox card = new VBox(22, title, starRow, battery, blockLine, buttons);
        if (withSolution) {
            Label note = new Label(Texts.NO_STARS_WITH_SOLUTION);
            note.getStyleClass().add("result-detail");
            note.setWrapText(true);
            note.setTextAlignment(TextAlignment.CENTER);
            card.getChildren().setAll(title, note, buttons);
        }
        card.getStyleClass().add("result-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(760, USE_PREF_SIZE);
        getChildren().add(card);

        if (!withSolution) {
            pop.play();
        }
    }

    /** Asks before showing the solution: it costs the stars of this level. */
    static StackPane confirmSolution(Runnable onShow, Runnable onCancel) {
        Label title = new Label(Texts.SOLUTION_QUESTION);
        title.getStyleClass().add("result-title");
        title.setStyle("-fx-text-fill: #ffcc00;");
        Label note = new Label(Texts.SOLUTION_WARNING);
        note.getStyleClass().add("result-detail");
        note.setWrapText(true);
        note.setTextAlignment(TextAlignment.CENTER);

        Button show = new Button(Texts.SOLUTION_SHOW);
        show.getStyleClass().add("neutral");
        show.setOnAction(e -> onShow.run());
        Button cancel = new Button(Texts.SOLUTION_CANCEL);
        cancel.getStyleClass().add("run");
        cancel.setDefaultButton(true); // Enter = keep trying, the safe choice
        cancel.setOnAction(e -> onCancel.run());
        HBox buttons = new HBox(20, show, cancel);
        buttons.setAlignment(Pos.CENTER);

        VBox card = new VBox(22, title, note, buttons);
        card.getStyleClass().add("result-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(760, USE_PREF_SIZE);
        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("result-overlay");
        return overlay;
    }

    private static Label detail(String text, boolean reached) {
        Label l = new Label((reached ? "✔  " : "✘  ") + text);
        l.getStyleClass().addAll("result-detail", reached ? "reached" : "missed");
        return l;
    }
}
