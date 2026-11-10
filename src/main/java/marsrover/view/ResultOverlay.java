package marsrover.view;

import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import marsrover.model.Level;

/** Success card on top of the level: stars pop in one after another. */
final class ResultOverlay extends StackPane {

    ResultOverlay(int stars, int batteryUsed, int blocks, Level.StarThresholds targets,
                  String nextLabel, Runnable onAgain, Runnable onNext) {
        getStyleClass().add("result-overlay");

        Label title = new Label(Texts.SUCCESS);
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
        card.getStyleClass().add("result-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(760, USE_PREF_SIZE);
        getChildren().add(card);

        pop.play();
    }

    private static Label detail(String text, boolean reached) {
        Label l = new Label((reached ? "✔  " : "✘  ") + text);
        l.getStyleClass().addAll("result-detail", reached ? "reached" : "missed");
        return l;
    }
}
