package marsrover.model;

/** Immutable rover pose. */
public record Rover(int x, int y, Direction dir) {

    public int frontX() {
        return x + dir.dx();
    }

    public int frontY() {
        return y + dir.dy();
    }

    public Rover forward() {
        return new Rover(frontX(), frontY(), dir);
    }

    public Rover turnedLeft() {
        return new Rover(x, y, dir.left());
    }

    public Rover turnedRight() {
        return new Rover(x, y, dir.right());
    }
}
