package marsrover.view;

import marsrover.program.StepResult.Failure;

/** All visitor-facing German texts in one place. */
public final class Texts {

    public static final String RUN = "▶  Start";
    public static final String STEP = "⏭  Schritt";
    public static final String RESET = "↺  Zurück";
    public static final String SPEED = "Tempo";
    public static final String[] SPEEDS = {"langsam", "normal", "schnell"};
    public static final String BATTERY = "Batterie";
    public static final String PROGRAM = "Dein Programm";
    public static final String PALETTE = "Befehle";
    public static final String DROP_HERE = "Zieh Befehle hierher";
    public static final String SUCCESS = "Mission geschafft!";

    private Texts() {
    }

    public static String failure(Failure failure, int samplesLeft) {
        return switch (failure) {
            case ROCK -> "Der Rover ist gegen einen Felsen gefahren!";
            case CRATER -> "Der Rover ist in einen Krater gefallen!";
            case NO_SAMPLE -> "Hier liegt keine Probe!";
            case BATTERY_EMPTY -> "Batterie leer!";
            case ENDLESS_LOOP -> "Der Rover dreht sich im Kreis – Endlosschleife?";
            case SAMPLES_LEFT -> samplesLeft == 1
                    ? "Mission nicht geschafft: Es liegt noch 1 Probe herum."
                    : "Mission nicht geschafft: Es liegen noch " + samplesLeft + " Proben herum.";
            case NOT_AT_BASE -> "Mission nicht geschafft: Der Rover ist nicht an der Basis.";
        };
    }

    /** Small Java hint below an error, e.g. {@code Zeile 4: vor();}. */
    public static String javaHint(int line, String code) {
        return line > 0 ? "Zeile " + line + ":  " + code : "";
    }

    public static String battery(int left, int total) {
        return BATTERY + ": " + left + " / " + total;
    }
}
