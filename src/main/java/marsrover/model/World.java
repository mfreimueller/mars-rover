package marsrover.model;

import java.util.List;

/** Mutable tile grid. Use {@link #copy()} to hand out snapshots. */
public final class World {

    private final Tile[][] tiles; // [y][x]

    private World(Tile[][] tiles) {
        this.tiles = tiles;
    }

    /** Builds a world from validated level rows (see {@link LevelLoader}). */
    public static World fromRows(List<String> rows) {
        Tile[][] grid = new Tile[rows.size()][];
        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            grid[y] = new Tile[row.length()];
            for (int x = 0; x < row.length(); x++) {
                grid[y][x] = Tile.fromSymbol(row.charAt(x));
            }
        }
        return new World(grid);
    }

    public int width() {
        return tiles[0].length;
    }

    public int height() {
        return tiles.length;
    }

    public boolean inBounds(int x, int y) {
        return y >= 0 && y < height() && x >= 0 && x < width();
    }

    public Tile tileAt(int x, int y) {
        return tiles[y][x];
    }

    public void setTile(int x, int y, Tile tile) {
        tiles[y][x] = tile;
    }

    public int count(Tile tile) {
        int n = 0;
        for (Tile[] row : tiles) {
            for (Tile t : row) {
                if (t == tile) {
                    n++;
                }
            }
        }
        return n;
    }

    public World copy() {
        Tile[][] grid = new Tile[tiles.length][];
        for (int y = 0; y < tiles.length; y++) {
            grid[y] = tiles[y].clone();
        }
        return new World(grid);
    }
}
