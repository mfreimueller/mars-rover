package marsrover.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import marsrover.model.LevelLoader.LevelFormatException;
import marsrover.model.PaletteEntry.ActionEntry;
import marsrover.model.PaletteEntry.ForEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LevelLoaderTest {

    // Single quotes are swapped for double quotes to keep the JSON readable.
    private static final String VALID = """
            {
              'id': '02-long-road',
              'title': 'Lange Straße',
              'intro': 'Die Basis ist weit weg.',
              'width': 10, 'height': 3,
              'tiles': ['RRRRRRRRRR', 'GGGGSGGGGB', 'RRRRRRRRRR'],
              'rover': { 'x': 0, 'y': 1, 'dir': 'E' },
              'battery': 20,
              'palette': ['vor', 'linksUm', 'for:5', 'probeNehmen', 'vor'],
              'maxBlocks': 4,
              'stars': { 'battery': 12, 'blocks': 2 },
              'program': null
            }
            """.replace('\'', '"');

    private static Level parse(String json) throws LevelFormatException {
        return LevelLoader.parse(json);
    }

    private static String failure(String json) {
        return assertThrows(LevelFormatException.class, () -> parse(json)).getMessage();
    }

    @Test
    void parsesValidLevel() throws Exception {
        Level level = parse(VALID);

        assertEquals("02-long-road", level.id());
        assertEquals("Lange Straße", level.title());
        assertEquals(10, level.width());
        assertEquals(3, level.height());
        assertEquals(new Rover(0, 1, Direction.E), level.start());
        assertEquals(20, level.battery());
        assertEquals(4, level.maxBlocks().getAsInt());
        assertEquals(new Level.StarThresholds(12, 2), level.stars());
        // duplicate "vor" is dropped, order is kept
        assertEquals(List.of(
                new ActionEntry(Action.VOR), new ActionEntry(Action.LINKS_UM),
                new ForEntry(5), new ActionEntry(Action.PROBE_NEHMEN)), level.palette());

        World world = level.createWorld();
        assertEquals(Tile.SAMPLE, world.tileAt(4, 1));
        assertEquals(Tile.BASE, world.tileAt(9, 1));
        assertEquals(1, world.count(Tile.SAMPLE));
    }

    @Test
    void optionalFieldsMayBeMissing() throws Exception {
        Level level = parse(VALID.replace("\"maxBlocks\": 4,", "").replace("\"intro\": \"Die Basis ist weit weg.\",", ""));

        assertTrue(level.maxBlocks().isEmpty());
        assertEquals("", level.intro());
    }

    @Test
    void createWorldReturnsIndependentCopies() throws Exception {
        Level level = parse(VALID);
        level.createWorld().setTile(4, 1, Tile.GROUND);

        assertEquals(Tile.SAMPLE, level.createWorld().tileAt(4, 1));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            missing id          | "id": "02-long-road",            |                                  | Feld 'id' fehlt
            missing stars       | "stars": { "battery": 12, "blocks": 2 }, |                         | Feld 'stars' fehlt
            unknown field       | "maxBlocks": 4                   | "maxblocks": 4                   | Unbekanntes Feld 'maxblocks'
            too wide            | "width": 10                      | "width": 21                      | ungültig
            row count           | "height": 3                      | "height": 4                      | 3 Zeilen, height ist 4
            row length          | "GGGGSGGGGB"                     | "GGGGSGGGG"                      | Zeile 2 hat 9 Zeichen
            spaced row          | "GGGGSGGGGB"                     | "G G G G B "                     | unbekanntes Zeichen ' '
            unknown tile        | "GGGGSGGGGB"                     | "GGGGXGGGGB"                     | unbekanntes Zeichen 'X'
            no base             | "GGGGSGGGGB"                     | "GGGGSGGGGG"                     | genau eine Basis
            two bases           | "GGGGSGGGGB"                     | "GGGGBGGGGB"                     | gefunden: 2
            bad direction       | "dir": "E"                       | "dir": "O"                       | rover.dir 'O' ungültig
            rover outside       | "x": 0, "y": 1                   | "x": 0, "y": 5                   | außerhalb
            rover on rock       | "x": 0, "y": 1                   | "x": 0, "y": 0                   | Rover startet auf ROCK
            zero battery        | "battery": 20                    | "battery": 0                     | battery muss mindestens 1 sein
            empty palette       | "palette": ["vor", "linksUm", "for:5", "probeNehmen", "vor"] | "palette": [] | palette ist leer
            unknown block       | "linksUm"                        | "linksum"                        | Unbekannter Block 'linksum'
            for count too high  | "for:5"                          | "for:21"                         | zwischen 1 und 20
            unknown sensor      | "for:5"                          | "while:vornfrei"                 | unbekannter Sensor 'vornfrei'
            zero maxBlocks      | "maxBlocks": 4                   | "maxBlocks": 0                   | maxBlocks muss mindestens 1 sein
            zero star blocks    | "blocks": 2                      | "blocks": 0                      | mindestens 1
            """)
    void rejectsInvalidLevel(String name, String find, String replace, String expectedMessagePart) {
        String json = VALID.replace(find.strip(), replace == null ? "" : replace.strip());
        assertTrue(!json.equals(VALID), "test setup: '" + find + "' not found");

        String message = failure(json);

        assertTrue(message.contains(expectedMessagePart), "message was: " + message);
    }

    @Test
    void rejectsBrokenJsonWithLineNumber() {
        String message = failure(VALID.replace("\"battery\": 20,", "\"battery\": 20"));

        assertTrue(message.startsWith("Kein gültiges JSON (Zeile "), message);
    }

    @Test
    void rejectsEmptyFile() {
        assertTrue(failure("").contains("JSON") || failure("").contains("leer"));
    }

    @Test
    void loadsFolderInFilenameOrderAndCollectsErrors(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("02-b.json"), VALID.replace("02-long-road", "b"));
        Files.writeString(dir.resolve("01-a.json"), VALID.replace("02-long-road", "a"));
        Files.writeString(dir.resolve("03-broken.json"), "{ nope");
        Files.writeString(dir.resolve("04-duplicate.json"), VALID.replace("02-long-road", "a"));
        Files.writeString(dir.resolve("notes.txt"), "ignored");

        LevelLoader.Result result = LevelLoader.loadFolder(dir);

        assertEquals(List.of("a", "b"), result.levels().stream().map(Level::id).toList());
        assertEquals(2, result.errors().size());
        assertEquals("03-broken.json", result.errors().get(0).file());
        assertEquals("04-duplicate.json", result.errors().get(1).file());
        assertTrue(result.errors().get(1).reason().contains("gibt es schon"));
    }

    @Test
    void missingFolderIsAnErrorNotAnException(@TempDir Path dir) {
        LevelLoader.Result result = LevelLoader.loadFolder(dir.resolve("nope"));

        assertTrue(result.levels().isEmpty());
        assertEquals("Ordner nicht gefunden", result.errors().getFirst().reason());
    }
}
