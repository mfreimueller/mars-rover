package marsrover.session;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Checks visitor names: 1–12 characters, letters / digits / spaces, no bad words.
 * Bad words match as substrings after normalizing case, spaces and simple leetspeak (0→o, 1→i …).
 */
public final class NameFilter {

    public static final int MAX_LENGTH = 12;

    public record Result(boolean ok, String name, String error) {
        static Result ok(String name) {
            return new Result(true, name, "");
        }

        static Result error(String message) {
            return new Result(false, "", message);
        }
    }

    private final List<String> badWords;

    public NameFilter(List<String> badWords) {
        this.badWords = badWords.stream()
                .map(NameFilter::normalize)
                .filter(w -> !w.isEmpty())
                .toList();
    }

    /** Reads one word per line; blank lines and lines starting with # are ignored. Missing file = no words. */
    public static NameFilter fromFile(Path file) {
        try {
            List<String> words = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .map(String::strip)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .toList();
            return new NameFilter(words);
        } catch (IOException e) {
            System.err.println("Wortliste nicht lesbar: " + file + " (" + e.getMessage() + ")");
            return new NameFilter(List.of());
        }
    }

    public Result check(String input) {
        String name = input == null ? "" : input.strip().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            return Result.error("Bitte gib deinen Namen ein.");
        }
        if (name.length() > MAX_LENGTH) {
            return Result.error("Höchstens " + MAX_LENGTH + " Zeichen, bitte.");
        }
        if (!name.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == ' ')) {
            return Result.error("Bitte nur Buchstaben und Zahlen.");
        }
        String normalized = normalize(name);
        if (badWords.stream().anyMatch(normalized::contains)) {
            return Result.error("Bitte wähle einen anderen Namen.");
        }
        return Result.ok(name);
    }

    static String normalize(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) {
            switch (c) {
                case ' ', '_', '-', '.' -> { }
                case '0' -> sb.append('o');
                case '1' -> sb.append('i');
                case '3' -> sb.append('e');
                case '4' -> sb.append('a');
                case '5' -> sb.append('s');
                case '7' -> sb.append('t');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
