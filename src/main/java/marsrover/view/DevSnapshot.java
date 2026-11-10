package marsrover.view;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import javax.imageio.ImageIO;

/**
 * Dev aid: {@code -Dsnapshot=out.png [-Dsnapshot.delay=ms]} writes the scene to a PNG and exits.
 * Lets us check layouts at 1920×1080 without a full-screen window.
 */
public final class DevSnapshot {

    private DevSnapshot() {
    }

    /** Returns true if a snapshot was scheduled; the caller should then not show the stage. */
    public static boolean scheduleIfRequested(Scene scene) {
        String path = System.getProperty("snapshot");
        if (path == null) {
            return false;
        }
        Platform.setImplicitExit(false);
        long delay = Long.getLong("snapshot.delay", 1000);
        PauseTransition pause = new PauseTransition(Duration.millis(delay));
        pause.setOnFinished(e -> {
            write(scene.snapshot(null), new File(path));
            Platform.exit();
        });
        pause.play();
        return true;
    }

    private static void write(WritableImage image, File file) {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(out, "png", file);
        } catch (IOException e) {
            System.err.println("snapshot failed: " + e.getMessage());
        }
    }
}
