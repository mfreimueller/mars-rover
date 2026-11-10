package marsrover.view;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javafx.scene.text.Font;

/** Picks an installed monospace font, because JavaFX CSS does not support fallback lists. */
public final class Fonts {

    private static final List<String> MONO = List.of("Consolas", "Menlo", "DejaVu Sans Mono", "Courier New");

    private Fonts() {
    }

    public static String monoFamily() {
        List<String> installed = Font.getFamilies();
        return MONO.stream().filter(installed::contains).findFirst().orElse("Monospaced");
    }

    /** A data-URI stylesheet that applies the mono font to all code elements. */
    public static String monoStylesheet() {
        String css = ".code-text, .line-number, .java-hint { -fx-font-family: \"" + monoFamily() + "\"; }";
        return "data:text/css," + URLEncoder.encode(css, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
