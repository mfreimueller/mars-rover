package marsrover.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Loads and validates level JSON files. Never throws to the caller for bad content:
 * {@link #loadFolder} collects problems as {@link LoadError}s so the admin can see them.
 * Messages are German because students read them on the day.
 */
public final class LevelLoader {

    public static final int MAX_WIDTH = 20;
    public static final int MAX_HEIGHT = 12;

    public record LoadError(String file, String reason) {}

    public record Result(List<Level> levels, List<LoadError> errors) {}

    /** Thrown by {@link #parse} for invalid level content. */
    public static final class LevelFormatException extends Exception {
        public LevelFormatException(String message) {
            super(message);
        }
    }

    // Wire format. Boxed types so missing fields can be detected.
    private record RoverJson(Integer x, Integer y, String dir) {}

    private record StarsJson(Integer battery, Integer blocks) {}

    private record LevelJson(
            String id, String title, String intro,
            Integer width, Integer height, List<String> tiles,
            RoverJson rover, Integer battery, List<String> palette,
            Integer maxBlocks, StarsJson stars,
            Object program /* reserved for fix-the-bug levels, ignored for now */) {}

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    private LevelLoader() {
    }

    /** Loads all {@code *.json} files of a folder in filename order. Duplicate ids are rejected. */
    public static Result loadFolder(Path folder) {
        List<Level> levels = new ArrayList<>();
        List<LoadError> errors = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            errors.add(new LoadError(folder.toString(), "Ordner nicht gefunden"));
            return new Result(levels, errors);
        }
        List<Path> files;
        try (Stream<Path> s = Files.list(folder)) {
            files = s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            errors.add(new LoadError(folder.toString(), "Ordner nicht lesbar: " + e.getMessage()));
            return new Result(levels, errors);
        }
        Set<String> ids = new HashSet<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            try {
                Level level = parse(Files.readString(file, StandardCharsets.UTF_8));
                if (!ids.add(level.id())) {
                    errors.add(new LoadError(name, "id '" + level.id() + "' gibt es schon"));
                } else {
                    levels.add(level);
                }
            } catch (LevelFormatException e) {
                errors.add(new LoadError(name, e.getMessage()));
            } catch (IOException | RuntimeException e) {
                errors.add(new LoadError(name, "Datei nicht lesbar: " + e.getMessage()));
            }
        }
        return new Result(List.copyOf(levels), List.copyOf(errors));
    }

    /** Parses and validates one level. */
    public static Level parse(String json) throws LevelFormatException {
        LevelJson j;
        try {
            j = MAPPER.readValue(json, LevelJson.class);
        } catch (UnrecognizedPropertyException e) {
            throw new LevelFormatException("Unbekanntes Feld '" + e.getPropertyName() + "'");
        } catch (JsonProcessingException e) {
            String where = e.getLocation() == null ? "" : " (Zeile " + e.getLocation().getLineNr() + ")";
            throw new LevelFormatException("Kein gültiges JSON" + where + ": " + e.getOriginalMessage());
        }
        if (j == null) {
            throw new LevelFormatException("Datei ist leer");
        }

        String id = require(j.id(), "id");
        String title = require(j.title(), "title");
        int width = require(j.width(), "width");
        int height = require(j.height(), "height");
        if (width < 1 || width > MAX_WIDTH || height < 1 || height > MAX_HEIGHT) {
            throw new LevelFormatException(
                    "Größe " + width + "×" + height + " ungültig (max. " + MAX_WIDTH + "×" + MAX_HEIGHT + ")");
        }

        List<String> tiles = validateTiles(require(j.tiles(), "tiles"), width, height);
        Rover start = validateRover(require(j.rover(), "rover"), tiles);

        int battery = require(j.battery(), "battery");
        if (battery < 1) {
            throw new LevelFormatException("battery muss mindestens 1 sein");
        }

        List<PaletteEntry> palette = validatePalette(require(j.palette(), "palette"));

        OptionalInt maxBlocks = OptionalInt.empty();
        if (j.maxBlocks() != null) {
            if (j.maxBlocks() < 1) {
                throw new LevelFormatException("maxBlocks muss mindestens 1 sein");
            }
            maxBlocks = OptionalInt.of(j.maxBlocks());
        }

        StarsJson s = require(j.stars(), "stars");
        int starBattery = require(s.battery(), "stars.battery");
        int starBlocks = require(s.blocks(), "stars.blocks");
        if (starBattery < 1 || starBlocks < 1) {
            throw new LevelFormatException("stars.battery und stars.blocks müssen mindestens 1 sein");
        }

        String intro = j.intro() == null ? "" : j.intro();
        return new Level(id, title, intro, tiles, start, battery, palette, maxBlocks,
                new Level.StarThresholds(starBattery, starBlocks));
    }

    private static List<String> validateTiles(List<String> rows, int width, int height)
            throws LevelFormatException {
        if (rows.size() != height) {
            throw new LevelFormatException("tiles hat " + rows.size() + " Zeilen, height ist " + height);
        }
        int bases = 0;
        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            if (row == null || row.length() != width) {
                int len = row == null ? 0 : row.length();
                throw new LevelFormatException(
                        "tiles Zeile " + (y + 1) + " hat " + len + " Zeichen, width ist " + width);
            }
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                Tile t = Tile.fromSymbol(c);
                if (t == null) {
                    throw new LevelFormatException("tiles Zeile " + (y + 1) + ": unbekanntes Zeichen '" + c
                            + "' (erlaubt: G R C S B D)");
                }
                if (t == Tile.BASE) {
                    bases++;
                }
            }
        }
        if (bases != 1) {
            throw new LevelFormatException("Es muss genau eine Basis (B) geben, gefunden: " + bases);
        }
        return List.copyOf(rows);
    }

    private static Rover validateRover(RoverJson r, List<String> tiles) throws LevelFormatException {
        int x = require(r.x(), "rover.x");
        int y = require(r.y(), "rover.y");
        String dirText = require(r.dir(), "rover.dir");
        Direction dir;
        try {
            dir = Direction.valueOf(dirText);
        } catch (IllegalArgumentException e) {
            throw new LevelFormatException("rover.dir '" + dirText + "' ungültig (erlaubt: N E S W)");
        }
        if (y < 0 || y >= tiles.size() || x < 0 || x >= tiles.getFirst().length()) {
            throw new LevelFormatException("Rover-Start (" + x + "," + y + ") liegt außerhalb des Felds");
        }
        Tile startTile = Tile.fromSymbol(tiles.get(y).charAt(x));
        if (!startTile.isDrivable()) {
            throw new LevelFormatException("Rover startet auf " + startTile + " bei (" + x + "," + y + ")");
        }
        return new Rover(x, y, dir);
    }

    private static List<PaletteEntry> validatePalette(List<String> entries) throws LevelFormatException {
        if (entries.isEmpty()) {
            throw new LevelFormatException("palette ist leer");
        }
        List<PaletteEntry> result = new ArrayList<>();
        for (String text : entries) {
            try {
                PaletteEntry entry = PaletteEntry.parse(text);
                if (!result.contains(entry)) {
                    result.add(entry);
                }
            } catch (IllegalArgumentException e) {
                throw new LevelFormatException("palette: " + e.getMessage());
            }
        }
        return List.copyOf(result);
    }

    private static <T> T require(T value, String field) throws LevelFormatException {
        if (value == null) {
            throw new LevelFormatException("Feld '" + field + "' fehlt");
        }
        return value;
    }
}
