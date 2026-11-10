package marsrover.model;

public enum Tile {
    GROUND('G'),
    ROCK('R'),
    CRATER('C'),
    SAMPLE('S'),
    BASE('B'),
    DUST('D');

    private final char symbol;

    Tile(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    /** Safe to drive onto: everything except rocks and craters. */
    public boolean isDrivable() {
        return this != ROCK && this != CRATER;
    }

    /** Returns the tile for a level-file character, or {@code null} if unknown. */
    public static Tile fromSymbol(char c) {
        for (Tile t : values()) {
            if (t.symbol == c) {
                return t;
            }
        }
        return null;
    }
}
