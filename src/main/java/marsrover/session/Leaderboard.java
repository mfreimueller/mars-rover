package marsrover.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * All results of the day, best first: more stars wins, then less time, then whoever was first.
 * Stored as a JSON array. A missing or broken file means an empty board, never a crash.
 */
public final class Leaderboard {

    public static final int TOP = 10;

    public record Entry(String name, int stars, long timeMillis) {}

    private static final Comparator<Entry> ORDER =
            Comparator.comparingInt(Entry::stars).reversed().thenComparingLong(Entry::timeMillis);

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Path file;
    private final List<Entry> entries = new ArrayList<>();

    private Leaderboard(Path file) {
        this.file = file;
    }

    public static Leaderboard load(Path file) {
        Leaderboard board = new Leaderboard(file);
        if (Files.exists(file)) {
            try {
                List<Entry> read = MAPPER.readValue(file.toFile(), new TypeReference<List<Entry>>() { });
                read.stream().filter(e -> e != null && e.name() != null).forEach(board.entries::add);
                board.entries.sort(ORDER);
            } catch (IOException | RuntimeException e) {
                System.err.println("Bestenliste nicht lesbar, starte leer: " + e.getMessage());
            }
        }
        return board;
    }

    /** Adds a result, saves, and returns the new entry's 1-based rank. */
    public int add(Session.Result result) {
        Entry entry = new Entry(result.name(), result.stars(), result.timeMillis());
        int index = 0;
        while (index < entries.size() && ORDER.compare(entries.get(index), entry) <= 0) {
            index++; // equal results: the earlier one stays ahead
        }
        entries.add(index, entry);
        save();
        return index + 1;
    }

    public List<Entry> top() {
        return List.copyOf(entries.subList(0, Math.min(TOP, entries.size())));
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
        save();
    }

    /** Writes to a temp file first so a crash mid-write can't destroy the board. */
    private void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            MAPPER.writeValue(tmp.toFile(), entries);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("Bestenliste nicht gespeichert: " + e.getMessage());
        }
    }
}
