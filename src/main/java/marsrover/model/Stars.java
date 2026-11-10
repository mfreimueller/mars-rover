package marsrover.model;

/** Star rating for one solved level (SPEC §5). */
public final class Stars {

    private Stars() {
    }

    /**
     * 0 = not solved, 1 = solved, 2 = also within the battery target,
     * 3 = also within the block target.
     */
    public static int rate(boolean solved, int batteryUsed, int blockCount, Level.StarThresholds t) {
        if (!solved) {
            return 0;
        }
        if (batteryUsed > t.battery()) {
            return 1;
        }
        return blockCount <= t.blocks() ? 3 : 2;
    }
}
