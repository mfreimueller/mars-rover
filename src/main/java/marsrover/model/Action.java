package marsrover.model;

/** Rover actions as named in the visitor-facing Java API. */
public enum Action {
    VOR("vor"),
    LINKS_UM("linksUm"),
    RECHTS_UM("rechtsUm"),
    PROBE_NEHMEN("probeNehmen");

    private final String javaName;

    Action(String javaName) {
        this.javaName = javaName;
    }

    public String javaName() {
        return javaName;
    }

    public static Action fromJavaName(String name) {
        for (Action a : values()) {
            if (a.javaName.equals(name)) {
                return a;
            }
        }
        return null;
    }
}
