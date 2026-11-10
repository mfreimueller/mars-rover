package marsrover;

/**
 * Entry point of the shaded jar. It must not extend {@code Application}, otherwise the
 * JavaFX launcher refuses to start from the classpath ("JavaFX runtime components are missing").
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
