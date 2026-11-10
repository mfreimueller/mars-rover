package marsrover.view;

import marsrover.model.Tile;
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

    public static final String NEXT_LEVEL = "Nächstes Level  ▶";
    public static final String AGAIN = "↺  Nochmal";

        public static final String FINISH = "Mission beenden  ▶";
    public static final String GAME_TITLE = "Mars Rover Mission";
    public static final String GAME_SUBTITLE =
            "Steuere einen echten Rover über den Mars – mit echtem Java-Code. Bau dein Programm aus Blöcken!";
    public static final String NAME_PROMPT = "Wie heißt du, Pilot:in?";
    public static final String NAME_PLACEHOLDER = "Dein Name";
    public static final String START_MISSION = "Mission starten  ▶";
    public static final String DONE = "Fertig";
    public static final String LEADERBOARD = "🏆 Bestenliste";
    public static final String LEADERBOARD_EMPTY = "Noch niemand hier – sei die Erste oder der Erste!";

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

    /** Block counter, e.g. "Blöcke: 3 / 4" or "Blöcke: 3" without a limit. */
    public static String blocks(int used, java.util.OptionalInt max) {
        return "Blöcke: " + used + (max.isPresent() ? " / " + max.getAsInt() : "");
    }

    /** The mission goal, always visible under the intro. */
    public static String goal(int samples) {
        if (samples == 0) {
            return "🎯 Ziel: Fahr zur Basis.";
        }
        return "🎯 Ziel: Sammle " + (samples == 1 ? "die Probe" : "alle " + samples + " Proben")
                + " ein und fahr dann zur Basis.";
    }

    public static String samples(int collected, int total) {
        return "Proben: " + collected + " / " + total;
    }

    public static String legend(Tile tile) {
        return switch (tile) {
            case SAMPLE -> "Probe";
            case BASE -> "Basis (Ziel)";
            case CRATER -> "Krater";
            case ROCK -> "Fels";
            case DUST -> "Staub (2× Batterie)";
            case GROUND -> "Boden";
        };
    }

    public static String batteryTarget(int used, int target) {
        return "Batterie verbraucht: " + used + "  (für ★★ höchstens " + target + ")";
    }

    public static String blocksTarget(int used, int target) {
        return "Blöcke: " + used + "  (für ★★★ höchstens " + target + ")";
    }

    public static String progress(String name, int level, int levels) {
        return "Pilot:in " + name + "  ·  Level " + level + " / " + levels;
    }

    public static String missionComplete(String name) {
        return "Mission beendet, " + name + "!";
    }

    /** e.g. "★★☆" */
    public static String stars(int n) {
        return "★".repeat(n) + "☆".repeat(Math.max(0, 3 - n));
    }

    public static String totalStars(int stars, int max) {
        return "Gesamt: " + stars + " / " + max + " ★";
    }

    /** e.g. "Zeit: 3:07" */
    public static String time(long millis) {
        return "Zeit: " + clock(millis);
    }

    /** e.g. "3:07" */
    public static String clock(long millis) {
        long seconds = millis / 1000;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    public static String rank(int rank) {
        return rank == 1 ? "🥇 Du bist auf Platz 1!" : "Du bist auf Platz " + rank + "!";
    }

    public static String battery(int left, int total) {
        return BATTERY + ": " + left + " / " + total;
    }
}
