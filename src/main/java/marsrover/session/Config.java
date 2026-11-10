package marsrover.session;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Settings from {@code data/config.properties}. Missing file or bad values fall back to defaults. */
public record Config(String adminPin, int inactivitySeconds) {

    public static final Config DEFAULTS = new Config("1234", 60);

    public static Config load(Path file) {
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException e) {
                System.err.println("Config nicht lesbar, nutze Standardwerte: " + e.getMessage());
            }
        }
        String pin = p.getProperty("adminPin", DEFAULTS.adminPin()).strip();
        int seconds = DEFAULTS.inactivitySeconds();
        try {
            seconds = Integer.parseInt(p.getProperty("inactivitySeconds", String.valueOf(seconds)).strip());
        } catch (NumberFormatException e) {
            System.err.println("inactivitySeconds ungültig, nutze " + seconds);
        }
        return new Config(pin.isEmpty() ? DEFAULTS.adminPin() : pin, Math.max(10, seconds));
    }
}
