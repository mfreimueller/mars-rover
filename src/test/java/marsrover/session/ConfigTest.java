package marsrover.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {

    @TempDir
    Path dir;

    @Test
    void readsValues() throws IOException {
        Path f = dir.resolve("c.properties");
        Files.writeString(f, "adminPin = 4711\ninactivitySeconds=90\n");

        assertEquals(new Config("4711", 90), Config.load(f));
    }

    @Test
    void defaultsForMissingFileAndBadValues() throws IOException {
        assertEquals(Config.DEFAULTS, Config.load(dir.resolve("nope")));

        Path f = dir.resolve("c.properties");
        Files.writeString(f, "adminPin=\ninactivitySeconds=soon\n");
        assertEquals(Config.DEFAULTS, Config.load(f));
    }

    @Test
    void timeoutHasAFloor() throws IOException {
        Path f = dir.resolve("c.properties");
        Files.writeString(f, "inactivitySeconds=1\n");

        assertEquals(10, Config.load(f).inactivitySeconds());
    }

    @Test
    void shippedConfigLoads() {
        assertEquals(60, Config.load(Path.of("data/config.properties")).inactivitySeconds());
    }
}
