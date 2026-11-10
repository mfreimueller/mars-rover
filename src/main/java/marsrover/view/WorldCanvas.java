package marsrover.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.transform.Affine;
import marsrover.model.Rover;
import marsrover.model.Tile;
import marsrover.model.World;

/** Draws the map and the rover, scaled to fit and centered. Plain shapes, no image assets. */
public final class WorldCanvas extends Region {

    private static final double MAX_TILE = 110;

    private static final Color GROUND = Color.web("#c8693a");
    private static final Color GROUND_DARK = Color.web("#b65c30");
    private static final Color DUST = Color.web("#e3a56f");
    private static final Color ROCK = Color.web("#5e3426");
    private static final Color ROCK_LIGHT = Color.web("#7a4635");
    private static final Color CRATER = Color.web("#2a120b");
    private static final Color CRATER_RIM = Color.web("#8e4426");
    private static final Color SAMPLE = Color.web("#5ef0ff");
    private static final Color BASE = Color.web("#e9eef2");
    private static final Color BASE_PAD = Color.web("#5a6470");

    private final Canvas canvas = new Canvas();
    private World world;
    private Rover rover;
    private boolean crashed;

    public WorldCanvas() {
        getChildren().add(canvas);
        setMinSize(200, 200);
    }

    public void show(World world, Rover rover, boolean crashed) {
        this.world = world;
        this.rover = rover;
        this.crashed = crashed;
        draw();
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        draw();
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setTransform(new Affine());
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (world == null) {
            return;
        }
        double size = Math.floor(Math.min(MAX_TILE,
                Math.min(canvas.getWidth() / world.width(), canvas.getHeight() / world.height())));
        double ox = Math.floor((canvas.getWidth() - size * world.width()) / 2);
        double oy = Math.floor((canvas.getHeight() - size * world.height()) / 2);

        for (int y = 0; y < world.height(); y++) {
            for (int x = 0; x < world.width(); x++) {
                drawTile(g, world.tileAt(x, y), ox + x * size, oy + y * size, size, (x + y) % 2 == 0);
            }
        }
        if (rover != null) {
            drawRover(g, ox + rover.x() * size, oy + rover.y() * size, size);
        }
    }

    private static void drawTile(GraphicsContext g, Tile tile, double x, double y, double s, boolean even) {
        g.setFill(tile == Tile.DUST ? DUST : even ? GROUND : GROUND_DARK);
        g.fillRect(x, y, s, s);
        switch (tile) {
            case ROCK -> {
                g.setFill(ROCK);
                g.fillPolygon(
                        new double[] {x + s * .12, x + s * .35, x + s * .62, x + s * .9, x + s * .82, x + s * .15},
                        new double[] {y + s * .78, y + s * .22, y + s * .14, y + s * .45, y + s * .86, y + s * .9},
                        6);
                g.setFill(ROCK_LIGHT);
                g.fillPolygon(
                        new double[] {x + s * .35, x + s * .62, x + s * .55, x + s * .3},
                        new double[] {y + s * .22, y + s * .14, y + s * .4, y + s * .45},
                        4);
            }
            case CRATER -> {
                g.setFill(CRATER_RIM);
                g.fillOval(x + s * .06, y + s * .1, s * .88, s * .8);
                g.setFill(CRATER);
                g.fillOval(x + s * .14, y + s * .2, s * .72, s * .6);
            }
            case SAMPLE -> {
                g.setFill(SAMPLE.deriveColor(0, 1, 1, .25));
                g.fillOval(x + s * .2, y + s * .2, s * .6, s * .6);
                g.setFill(SAMPLE);
                g.fillPolygon(
                        new double[] {x + s * .5, x + s * .64, x + s * .5, x + s * .36},
                        new double[] {y + s * .24, y + s * .5, y + s * .76, y + s * .5},
                        4);
            }
            case BASE -> {
                g.setFill(BASE_PAD);
                g.fillOval(x + s * .08, y + s * .08, s * .84, s * .84);
                g.setFill(BASE);
                g.fillArc(x + s * .2, y + s * .25, s * .6, s * .6, 0, 180, ArcType.ROUND);
                g.setFill(Color.web("#ffcc00"));
                g.fillRect(x + s * .47, y + s * .12, s * .06, s * .2);
            }
            case GROUND, DUST -> {
                if (tile == Tile.DUST) {
                    g.setFill(GROUND.deriveColor(0, 1, 1, .5));
                    for (int i = 0; i < 5; i++) {
                        g.fillOval(x + s * (.15 + .16 * i), y + s * (.3 + .1 * (i % 3)), s * .07, s * .07);
                    }
                }
            }
        }
        g.setStroke(Color.color(0, 0, 0, .15));
        g.setLineWidth(1);
        g.strokeRect(x + .5, y + .5, s - 1, s - 1);
    }

    /** Rover drawn facing east, rotated to its direction. */
    private void drawRover(GraphicsContext g, double x, double y, double s) {
        g.save();
        g.translate(x + s / 2, y + s / 2);
        g.rotate(switch (rover.dir()) {
            case E -> 0;
            case S -> 90;
            case W -> 180;
            case N -> 270;
        });
        if (crashed) {
            g.scale(.6, .6);
            g.setGlobalAlpha(.6);
        }
        double h = s / 2;
        // wheels
        g.setFill(Color.web("#222"));
        for (double wx : new double[] {-.32, .0, .3}) {
            g.fillRoundRect(h * (wx - .12), -h * .78, h * .24, h * .22, 4, 4);
            g.fillRoundRect(h * (wx - .12), h * .56, h * .24, h * .22, 4, 4);
        }
        // body
        g.setFill(Color.web("#f2f2f2"));
        g.fillRoundRect(-h * .55, -h * .58, h * 1.1, h * 1.16, h * .3, h * .3);
        // solar panel
        g.setFill(Color.web("#2f5fa8"));
        g.fillRect(-h * .45, -h * .4, h * .5, h * .8);
        // camera head shows the driving direction
        g.setFill(Color.web("#ffcc00"));
        g.fillPolygon(new double[] {h * .3, h * .8, h * .3}, new double[] {-h * .3, 0, h * .3}, 3);
        g.restore();
    }
}
