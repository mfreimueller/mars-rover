package marsrover.program;

import marsrover.model.Action;
import marsrover.model.Rover;
import marsrover.model.World;

/**
 * State after one {@link Interpreter#step()}.
 *
 * @param line        line of the action just executed, or of the block that failed; 0 if none
 * @param action      the action just executed, or {@code null}
 * @param world       a snapshot; safe to keep and draw
 * @param failure     set when {@code status == FAILED}
 * @param samplesLeft samples still on the map
 */
public record StepResult(
        Status status,
        int line,
        Action action,
        Rover rover,
        World world,
        int battery,
        int batteryUsed,
        Failure failure,
        int samplesLeft) {

    public enum Status {
        /** Not started yet. */
        READY,
        /** An action was executed; more may follow. */
        RUNNING,
        /** Program ended with all samples collected on the base. */
        SUCCESS,
        FAILED
    }

    public enum Failure {
        /** Drove into a rock or off the map. */
        ROCK,
        CRATER,
        NO_SAMPLE,
        BATTERY_EMPTY,
        ENDLESS_LOOP,
        /** Program ended, samples left on the map. */
        SAMPLES_LEFT,
        /** Program ended, all samples collected, but not on the base. */
        NOT_AT_BASE
    }

    public boolean isFinished() {
        return status == Status.SUCCESS || status == Status.FAILED;
    }
}
