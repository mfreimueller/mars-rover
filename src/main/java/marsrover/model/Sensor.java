package marsrover.model;

/** Rover sensors as named in the visitor-facing Java API. */
public enum Sensor {
    VORN_FREI("vornFrei"),
    VORN_KRATER("vornKrater"),
    PROBE_DA("probeDa"),
    AM_ZIEL("amZiel");

    private final String javaName;

    Sensor(String javaName) {
        this.javaName = javaName;
    }

    public String javaName() {
        return javaName;
    }

    public static Sensor fromJavaName(String name) {
        for (Sensor s : values()) {
            if (s.javaName.equals(name)) {
                return s;
            }
        }
        return null;
    }
}
