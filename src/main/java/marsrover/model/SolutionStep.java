package marsrover.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One block of a level's reference solution. In the level JSON a solution is written with the
 * palette tokens, and {@code "end"} closes a loop or if ({@code "else"} splits an ifelse), e.g.
 * {@code ["for:6", "if:vornKrater", "linksUm", "vor", "rechtsUm", "end", "vor", "end"]}.
 */
public record SolutionStep(PaletteEntry entry, List<SolutionStep> body, List<SolutionStep> otherwise) {

    public SolutionStep {
        body = List.copyOf(body);
        otherwise = List.copyOf(otherwise);
    }

    /** Parses solution tokens. Throws {@link IllegalArgumentException} with a German message. */
    public static List<SolutionStep> parse(List<String> tokens) {
        Parser p = new Parser(tokens);
        List<SolutionStep> steps = p.body();
        if (p.i < tokens.size()) {
            throw new IllegalArgumentException("'" + tokens.get(p.i) + "' ohne passenden Block");
        }
        return steps;
    }

    private static final class Parser {
        private final List<String> tokens;
        private int i;

        Parser(List<String> tokens) {
            this.tokens = tokens;
        }

        /** Reads blocks until "end", "else" or the end of the list. */
        List<SolutionStep> body() {
            List<SolutionStep> steps = new ArrayList<>();
            while (i < tokens.size() && !"end".equals(tokens.get(i)) && !"else".equals(tokens.get(i))) {
                String token = tokens.get(i++);
                PaletteEntry entry = PaletteEntry.parse(token);
                if (entry instanceof PaletteEntry.ActionEntry) {
                    steps.add(new SolutionStep(entry, List.of(), List.of()));
                    continue;
                }
                List<SolutionStep> body = body();
                List<SolutionStep> otherwise = List.of();
                if (entry instanceof PaletteEntry.IfEntry f && f.withElse()) {
                    expect("else", token);
                    otherwise = body();
                }
                expect("end", token);
                steps.add(new SolutionStep(entry, body, otherwise));
            }
            return steps;
        }

        private void expect(String word, String opener) {
            if (i >= tokens.size() || !word.equals(tokens.get(i))) {
                throw new IllegalArgumentException("zu '" + opener + "' fehlt \"" + word + "\"");
            }
            i++;
        }
    }
}
