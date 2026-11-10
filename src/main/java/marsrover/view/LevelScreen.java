package marsrover.view;

import java.util.function.Supplier;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import marsrover.model.Level;
import marsrover.program.Interpreter;
import marsrover.program.Program;
import marsrover.program.StepResult;
import marsrover.program.StepResult.Status;

/**
 * One level: map in the middle, program on the right, controls at the bottom.
 * Run is a {@link Timeline} that calls {@link Interpreter#step()} at the chosen speed.
 */
public final class LevelScreen extends BorderPane {

    private static final double[] SPEED_MS = {600, 300, 100};

    private final Level level;
    private final Supplier<Program> programSource;

    private final WorldCanvas worldCanvas = new WorldCanvas();
    private final CodeView codeView = new CodeView();
    private final ProgressBar batteryBar = new ProgressBar(1);
    private final Label batteryLabel = new Label();
    private final Label message = new Label();
    private final Label hint = new Label();
    private final Slider speed = new Slider(0, 2, 1);

    private Program program;
    private Interpreter interpreter;
    private Timeline timeline;

    public LevelScreen(Level level, Supplier<Program> programSource) {
        this.level = level;
        this.programSource = programSource;
        getStyleClass().add("level-screen");

        setTop(header());
        setCenter(worldPane());
        setRight(programPane());
        setBottom(controls());

        addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        reset();
    }

    // ---- layout ---------------------------------------------------------------------------

    private Node header() {
        Label title = new Label(level.title());
        title.getStyleClass().add("title");
        Label intro = new Label(level.intro());
        intro.getStyleClass().add("intro");
        intro.setWrapText(true);
        VBox box = new VBox(6, title, intro);
        box.setPadding(new Insets(24, 32, 12, 32));
        return box;
    }

    private Node worldPane() {
        BorderPane.setMargin(worldCanvas, new Insets(12, 16, 12, 32));
        return worldCanvas;
    }

    private Node programPane() {
        Label heading = new Label(Texts.PROGRAM);
        heading.getStyleClass().add("panel-heading");
        ScrollPane scroll = new ScrollPane(codeView);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("code-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox panel = new VBox(12, heading, scroll);
        panel.getStyleClass().add("panel");
        panel.setPrefWidth(720);
        BorderPane.setMargin(panel, new Insets(12, 32, 12, 16));
        return panel;
    }

    private Node controls() {
        Button run = button(Texts.RUN, "run", this::run);
        Button step = button(Texts.STEP, "step", this::step);
        Button reset = button(Texts.RESET, "reset", this::reset);

        speed.setMajorTickUnit(1);
        speed.setMinorTickCount(0);
        speed.setSnapToTicks(true);
        speed.setShowTickMarks(true);
        speed.setPrefWidth(200);
        speed.valueProperty().addListener((obs, old, now) -> {
            if (timeline != null && timeline.getStatus() == Timeline.Status.RUNNING) {
                startTimeline(); // restart with the new pace
            }
        });
        Label speedLabel = new Label(Texts.SPEED);
        speedLabel.getStyleClass().add("control-label");
        Label slow = new Label("🐢");
        Label fast = new Label("🐇");
        HBox speedBox = new HBox(10, speedLabel, slow, speed, fast);
        speedBox.setAlignment(Pos.CENTER_LEFT);

        batteryBar.setPrefWidth(220);
        batteryBar.getStyleClass().add("battery");
        batteryLabel.getStyleClass().add("control-label");
        VBox batteryBox = new VBox(4, batteryLabel, batteryBar);
        batteryBox.setAlignment(Pos.CENTER_LEFT);

        message.getStyleClass().add("message");
        hint.getStyleClass().add("java-hint");
        VBox messageBox = new VBox(2, message, hint);
        messageBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(messageBox, Priority.ALWAYS);

        Region gap = new Region();
        gap.setMinWidth(24);
        HBox bar = new HBox(16, run, step, reset, gap, speedBox, batteryBox, messageBox);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 32, 28, 32));
        return bar;
    }

    private static Button button(String text, String styleClass, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        return b;
    }

    // ---- execution ------------------------------------------------------------------------

    private void onKey(KeyEvent e) {
        switch (e.getCode()) {
            case F5, ENTER -> run();
            case F6 -> step();
            case R -> reset();
            default -> {
                return;
            }
        }
        e.consume();
    }

    /** Starts (or continues after Step) the animated run. After a finished run, starts over. */
    public void run() {
        if (interpreter.current().isFinished()) {
            reset();
        }
        startTimeline();
    }

    /** Executes one action; pauses a running animation. */
    public void step() {
        stopTimeline();
        if (interpreter.current().isFinished()) {
            reset();
        }
        show(interpreter.step());
    }

    /** Back to the start position; the program is kept. */
    public void reset() {
        stopTimeline();
        program = programSource.get();
        codeView.show(program);
        interpreter = new Interpreter(level, program);
        show(interpreter.current());
    }

    private void startTimeline() {
        stopTimeline();
        Duration pace = Duration.millis(SPEED_MS[(int) Math.round(speed.getValue())]);
        timeline = new Timeline(new KeyFrame(pace, e -> {
            StepResult r = interpreter.step();
            show(r);
            if (r.isFinished()) {
                stopTimeline();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void show(StepResult r) {
        worldCanvas.show(r.world(), r.rover(), r.failure() == StepResult.Failure.CRATER);
        batteryBar.setProgress(level.battery() == 0 ? 0 : (double) r.battery() / level.battery());
        batteryLabel.setText(Texts.battery(r.battery(), level.battery()));

        message.getStyleClass().removeAll("error", "success");
        hint.setText("");
        switch (r.status()) {
            case READY -> {
                message.setText("");
                codeView.highlight(0);
            }
            case RUNNING -> {
                message.setText("");
                codeView.highlight(r.line());
            }
            case SUCCESS -> {
                message.setText(Texts.SUCCESS);
                message.getStyleClass().add("success");
                codeView.highlight(0);
            }
            case FAILED -> {
                message.setText(Texts.failure(r.failure(), r.samplesLeft()));
                message.getStyleClass().add("error");
                hint.setText(Texts.javaHint(r.line(), program.lineText(r.line())));
                codeView.markFailed(r.line());
            }
        }
    }

    /** For tests and the dev snapshot. */
    public boolean isRunning() {
        return timeline != null;
    }

    public Status status() {
        return interpreter.current().status();
    }
}
