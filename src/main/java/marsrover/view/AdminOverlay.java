package marsrover.view;

import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import marsrover.model.LevelLoader;

/**
 * Hidden admin panel (Ctrl+Shift+A). First a PIN prompt, then: reload levels, clear the
 * leaderboard, exit, close. Three wrong PINs lock the prompt for 30 seconds.
 */
final class AdminOverlay extends StackPane {

    interface Actions {
        /** Reloads levels and returns what happened. */
        LevelLoader.Result reloadLevels();

        void clearLeaderboard();

        void exit();

        void close();
    }

    private static int wrongAttempts;
    private static long lockedUntil;

    private final String pin;
    private final Actions actions;
    private final VBox card = new VBox(20);

    AdminOverlay(String pin, Actions actions) {
        this.pin = pin;
        this.actions = actions;
        getStyleClass().add("result-overlay");
        card.getStyleClass().addAll("result-card", "admin-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxSize(820, USE_PREF_SIZE);
        getChildren().add(card);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                actions.close();
            }
        });
        showPinPrompt();
    }

    /** Dev only (snapshots): skip the PIN. */
    void unlockForDev() {
        showMenu("4 Levels geladen: Landung, Lange Straße, Kraterfeld, Unbekannte Entfernung."
                + "\n⚠ 05-student.json: tiles Zeile 2 hat 9 Zeichen, width ist 10");
    }

    private void showPinPrompt() {
        Label title = title("Admin");
        PasswordField field = new PasswordField();
        field.setPromptText("PIN");
        field.getStyleClass().add("name-field");
        field.setMaxWidth(300);
        Label error = new Label();
        error.getStyleClass().add("name-error");

        Runnable submit = () -> {
            long now = System.currentTimeMillis();
            if (now < lockedUntil) {
                error.setText("Gesperrt – bitte " + (lockedUntil - now + 999) / 1000 + " s warten.");
            } else if (field.getText().equals(pin)) {
                wrongAttempts = 0;
                showMenu("");
            } else {
                wrongAttempts++;
                if (wrongAttempts >= 3) {
                    wrongAttempts = 0;
                    lockedUntil = now + 30_000;
                    error.setText("Falscher PIN – 30 s gesperrt.");
                } else {
                    error.setText("Falscher PIN.");
                }
                field.clear();
            }
        };
        field.setOnAction(e -> submit.run());
        Button ok = button("OK", "run", submit);
        Button cancel = button("Abbrechen", "reset", actions::close);

        card.getChildren().setAll(title, new HBox(16, field, ok, cancel), error);
        Platform.runLater(field::requestFocus);
    }

    private void showMenu(String status) {
        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().add("admin-status");
        statusLabel.setWrapText(true);

        Button reload = button("Levels neu laden", "run", () -> {
            LevelLoader.Result r = actions.reloadLevels();
            showMenu(describe(r));
        });

        Button clear = button("Bestenliste löschen", "reset", null);
        clear.setOnAction(e -> {
            if ("confirm".equals(clear.getUserData())) {
                actions.clearLeaderboard();
                showMenu("Bestenliste gelöscht.");
            } else {
                clear.setUserData("confirm");
                clear.setText("Wirklich löschen?");
                clear.getStyleClass().add("danger");
                PauseTransition undo = new PauseTransition(Duration.seconds(4));
                undo.setOnFinished(ev -> {
                    clear.setUserData(null);
                    clear.setText("Bestenliste löschen");
                    clear.getStyleClass().remove("danger");
                });
                undo.play();
            }
        });

        Button exit = button("Programm beenden", "reset", actions::exit);
        exit.getStyleClass().add("danger");
        Button close = button("Schließen", "neutral", actions::close);

        VBox buttons = new VBox(14, reload, clear, exit, close);
        card.getChildren().setAll(title("Admin"), buttons, statusLabel);
    }

    private static String describe(LevelLoader.Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.levels().size()).append(r.levels().size() == 1 ? " Level geladen" : " Levels geladen");
        if (!r.levels().isEmpty()) {
            sb.append(": ").append(String.join(", ", r.levels().stream().map(l -> l.title()).toList()));
        }
        sb.append('.');
        List<LevelLoader.LoadError> errors = r.errors();
        for (LevelLoader.LoadError e : errors) {
            sb.append("\n⚠ ").append(e.file()).append(": ").append(e.reason());
        }
        return sb.toString();
    }

    private static Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("leaderboard-title");
        return l;
    }

    private static Button button(String text, String styleClass, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        b.setMinWidth(360);
        if (action != null) {
            b.setOnAction(e -> action.run());
        }
        return b;
    }
}
