package marsrover.model;

/** Compass direction. y grows downwards (row 0 is the top row of the level file). */
public enum Direction {
    N(0, -1),
    E(1, 0),
    S(0, 1),
    W(-1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public Direction left() {
        return values()[(ordinal() + 3) % 4];
    }

    public Direction right() {
        return values()[(ordinal() + 1) % 4];
    }
}
