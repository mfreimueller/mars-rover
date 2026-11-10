package marsrover.model;

import java.util.List;
import java.util.OptionalInt;

/** A validated, immutable level. Create via {@link LevelLoader}. */
public record Level(
        String id,
        String title,
        String intro,
        List<String> tiles,
        Rover start,
        int battery,
        List<PaletteEntry> palette,
        OptionalInt maxBlocks,
        StarThresholds stars) {

    public record StarThresholds(int battery, int blocks) {}

    public int width() {
        return tiles.getFirst().length();
    }

    public int height() {
        return tiles.size();
    }

    /** A fresh, mutable world in the level's start state. */
    public World createWorld() {
        return World.fromRows(tiles);
    }
}
