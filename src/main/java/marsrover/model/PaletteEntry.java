package marsrover.model;

/**
 * One block offered in a level's palette, parsed from strings like
 * {@code "vor"}, {@code "for:3"}, {@code "while:!amZiel"}, {@code "ifelse:vornKrater"}.
 */
public sealed interface PaletteEntry {

    int MIN_COUNT = 1;
    int MAX_COUNT = 20;

    record ActionEntry(Action action) implements PaletteEntry {}

    record ForEntry(int count) implements PaletteEntry {}

    record WhileEntry(Sensor sensor, boolean negated) implements PaletteEntry {}

    record IfEntry(Sensor sensor, boolean negated, boolean withElse) implements PaletteEntry {}

    /** Parses one palette string. Throws {@link IllegalArgumentException} with a German message. */
    static PaletteEntry parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Leerer Palette-Eintrag");
        }
        int colon = text.indexOf(':');
        if (colon < 0) {
            Action action = Action.fromJavaName(text);
            if (action == null) {
                throw new IllegalArgumentException("Unbekannter Block '" + text + "'");
            }
            return new ActionEntry(action);
        }
        String kind = text.substring(0, colon);
        String arg = text.substring(colon + 1);
        return switch (kind) {
            case "for" -> new ForEntry(parseCount(text, arg));
            case "while" -> {
                boolean neg = arg.startsWith("!");
                yield new WhileEntry(parseSensor(text, neg ? arg.substring(1) : arg), neg);
            }
            case "if", "ifelse" -> {
                boolean neg = arg.startsWith("!");
                yield new IfEntry(parseSensor(text, neg ? arg.substring(1) : arg), neg, kind.equals("ifelse"));
            }
            default -> throw new IllegalArgumentException("Unbekannter Block '" + text + "'");
        };
    }

    private static int parseCount(String text, String arg) {
        try {
            int n = Integer.parseInt(arg);
            if (n >= MIN_COUNT && n <= MAX_COUNT) {
                return n;
            }
        } catch (NumberFormatException ignored) {
            // fall through to the error below
        }
        throw new IllegalArgumentException(
                "'" + text + "': Zahl muss zwischen " + MIN_COUNT + " und " + MAX_COUNT + " liegen");
    }

    private static Sensor parseSensor(String text, String name) {
        Sensor sensor = Sensor.fromJavaName(name);
        if (sensor == null) {
            throw new IllegalArgumentException("'" + text + "': unbekannter Sensor '" + name + "'");
        }
        return sensor;
    }
}
