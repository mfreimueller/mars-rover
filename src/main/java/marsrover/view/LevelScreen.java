package marsrover.view;

import java.util.List;
import java.util.function.IntConsumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import marsrover.model.Level;
import marsrover.model.Stars;
import marsrover.model.Tile;
import marsrover.model.World;
import marsrover.program.Block;
import marsrover.program.Interpreter;
import marsrover.program.Program;
import marsrover.program.StepResult;
import marsrover.program.StepResult.Status;

/**
 * One level: map left, editor (toolbox + program) right, controls at the bottom.
 * The divider between map and editor can be dragged.
 * Run is a {@link Timeline} that calls {@link Interpreter#step()} at the chosen speed.
 * On success a {@link ResultOverlay} shows the stars.
 */
public final class LevelScreen extends StackPane {

    private static final double[] SPEED_MS = {600, 300, 100};
    /** Share of the width the map gets at the start of each level. */
    private static final double MAP_SHARE = 0.56;
    private static final Tile[] LEGEND_ORDER = {Tile.SAMPLE, Tile.BASE, Tile.CRATER, Tile.ROCK, Tile.DUST};

    private final Level level;
    private final String progress;
    private final boolean last;
    private final int totalSamples;
    private final EditorPane editor;
    private final IntConsumer onSolved;
    private final Runnable onNext;

    private final WorldCanvas worldCanvas = new WorldCanvas();
    private final ProgressBar batteryBar = new ProgressBar(1);
    private final Label batteryLabel = new Label();
    private final Label samplesLabel = new Label();
    private final Label message = new Label();
    private final Label hint = new Label();
    private final VBox banner = new VBox(4, message, hint);
    private final Slider speed = new Slider(0, 2, 1);

    private Program program;
    private Interpreter interpreter;
    private Timeline timeline;
    private StackPane overlay; // success card or the "show solution?" question
    private final Button solutionButton = button(Texts.SOLUTION, "neutral", this::askForSolution);
    /** Once the solution was shown, solving this level gives no stars. */
    private boolean solutionShown;

    /**
     * @param progress shown above the title, e.g. "Pilot:in Max · Level 2 / 4"
     * @param last     the success card then offers "Mission beenden" instead of "Nächstes Level"
     * @param onSolved called with the stars (1–3) each time the level is solved
     * @param onNext   called when the visitor presses "Nächstes Level"
     */
    public LevelScreen(Level level, List<Block> initialProgram, String progress, boolean last,
                       IntConsumer onSolved, Runnable onNext) {
        this.level = level;
        this.progress = progress;
        this.last = last;
        this.totalSamples = level.createWorld().count(Tile.SAMPLE);
        this.onSolved = onSolved;
        this.onNext = onNext;
        this.editor = new EditorPane(level, initialProgram, this::reset);
        getStyleClass().add("level-screen");

        BorderPane layout = new BorderPane();
        layout.setTop(header());
        SplitPane split = new SplitPane(worldPane(), editor.panel());
        split.getStyleClass().add("level-split");
        split.setDividerPositions(MAP_SHARE);
        BorderPane.setMargin(split, new Insets(12, 32, 12, 24));
        layout.setCenter(split);
        layout.setBottom(controls());
        getChildren().add(layout);

        addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        reset();
    }

    // ---- layout ---------------------------------------------------------------------------

    private Node header() {
        Label top = new Label(progress);
        top.getStyleClass().add("progress");
        Label title = new Label(level.title());
        title.getStyleClass().add("title");
        Label intro = new Label(level.intro());
        intro.getStyleClass().add("intro");
        intro.setWrapText(true);
        Label goal = new Label(Texts.goal(totalSamples));
        goal.getStyleClass().add("goal");
        VBox box = new VBox(4, top, title, intro, goal);
        box.setPadding(new Insets(20, 32, 8, 32));
        return box;
    }

    private Node worldPane() {
        banner.getStyleClass().add("banner");
        banner.setAlignment(Pos.CENTER);
        banner.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        banner.setMouseTransparent(true);
        message.getStyleClass().add("message");
        message.setWrapText(true);
        message.setTextAlignment(TextAlignment.CENTER);
        hint.getStyleClass().add("java-hint");
        hint.setWrapText(true);

        StackPane map = new StackPane(worldCanvas, banner);
        StackPane.setAlignment(banner, Pos.BOTTOM_CENTER);
        StackPane.setMargin(banner, new Insets(0, 8, 8, 8));
        // never wider than the map, so long messages wrap instead of being cut off
        // (16 margin + 56 banner padding)
        message.maxWidthProperty().bind(map.widthProperty().subtract(72));
        hint.maxWidthProperty().bind(map.widthProperty().subtract(72));
        VBox.setVgrow(map, Priority.ALWAYS);

        VBox pane = new VBox(12, map, legend());
        pane.setAlignment(Pos.CENTER);
        pane.setMinWidth(320);
        pane.setPadding(new Insets(0, 12, 0, 0));
        return pane;
    }

    /** Icons for the special tiles of this level. */
    private Node legend() {
        World world = level.createWorld();
        FlowPane legend = new FlowPane(22, 8);
        legend.setAlignment(Pos.CENTER);
        legend.getStyleClass().add("legend");
        for (Tile tile : LEGEND_ORDER) {
            if (world.count(tile) > 0) {
                Label label = new Label(Texts.legend(tile), WorldCanvas.icon(tile, 34));
                label.getStyleClass().add("legend-item");
                label.setMinWidth(Region.USE_PREF_SIZE);
                legend.getChildren().add(label);
            }
        }
        return legend;
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
        HBox speedBox = new HBox(10, speedLabel, new Label("🐢"), speed, new Label("🐇"));
        speedBox.setAlignment(Pos.CENTER_LEFT);

        batteryBar.setPrefWidth(220);
        batteryBar.getStyleClass().add("battery");
        batteryLabel.getStyleClass().add("control-label");
        VBox batteryBox = new VBox(4, batteryLabel, batteryBar);
        batteryBox.setAlignment(Pos.CENTER_LEFT);

        samplesLabel.getStyleClass().add("samples");
        samplesLabel.setVisible(totalSamples > 0);

        Region gap = new Region();
        gap.setMinWidth(24);
        solutionButton.setVisible(!level.solution().isEmpty());
        solutionButton.setManaged(!level.solution().isEmpty());

        HBox bar = new HBox(24, run, step, reset, solutionButton, gap, speedBox, batteryBox, samplesLabel);
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
        if (overlay != null) {
            return; // the overlay's buttons handle Enter themselves
        }
        switch (e.getCode()) {
            case F5, ENTER -> run();
            case F6 -> step();
            case R -> reset();
            case DELETE, BACK_SPACE -> editor.deleteSelected();
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
        closeOverlay();
        program = editor.program();
        interpreter = new Interpreter(level, program);
        show(interpreter.current());
    }

    /** "Lösung" button: asks first, because the solution costs the stars. */
    public void askForSolution() {
        stopTimeline();
        closeOverlay();
        overlay = ResultOverlay.confirmSolution(this::showSolution, this::closeOverlay);
        getChildren().add(overlay);
    }

    /** Loads the solution into the editor; solving the level then gives no stars. */
    public void showSolution() {
        solutionShown = true;
        solutionButton.setDisable(true);
        editor.showProgram(Block.fromSolution(level.solution())); // resets the level
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
        editor.setLocked(true);
        timeline.play();
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        editor.setLocked(false);
    }

    private void show(StepResult r) {
        worldCanvas.show(r.world(), r.rover(), r.failure() == StepResult.Failure.CRATER);
        batteryBar.setProgress(level.battery() == 0 ? 0 : (double) r.battery() / level.battery());
        batteryLabel.setText(Texts.battery(r.battery(), level.battery()));
        samplesLabel.setText(Texts.samples(totalSamples - r.samplesLeft(), totalSamples));

        banner.getStyleClass().removeAll("error", "success");
        banner.setVisible(false);
        hint.setText("");
        switch (r.status()) {
            case READY -> editor.highlight(0);
            case RUNNING -> editor.highlight(r.line());
            case SUCCESS -> {
                editor.highlight(0);
                solved(r);
            }
            case FAILED -> {
                message.setText(Texts.failure(r.failure(), r.samplesLeft()));
                hint.setText(Texts.javaHint(r.line(), program.lineText(r.line())));
                hint.setManaged(!hint.getText().isEmpty());
                banner.getStyleClass().add("error");
                banner.setVisible(true);
                editor.markFailed(r.line());
            }
        }
    }

    private void solved(StepResult r) {
        int blocks = program.blockCount();
        int stars = solutionShown ? 0 : Stars.rate(true, r.batteryUsed(), blocks, level.stars());
        onSolved.accept(stars);
        overlay = new ResultOverlay(stars, r.batteryUsed(), blocks, level.stars(), solutionShown,
                last ? Texts.FINISH : Texts.NEXT_LEVEL, this::reset, onNext);
        getChildren().add(overlay);
    }

    private void closeOverlay() {
        if (overlay != null) {
            getChildren().remove(overlay);
            overlay = null;
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
