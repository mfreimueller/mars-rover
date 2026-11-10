package marsrover.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class NameFilterTest {

    private final NameFilter filter = new NameFilter(List.of("arsch", "Doof"));

    @Test
    void acceptsAndTidiesNormalNames() {
        assertEquals("Max", filter.check("  Max ").name());
        assertEquals("Anna Lena", filter.check("Anna   Lena").name());
        assertTrue(filter.check("Jürgen2010").ok());
        assertTrue(filter.check("Øyvind ßöä").ok());
        assertTrue(filter.check("abcdefghijkl").ok(), "12 chars is fine");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abcdefghijklm", "Max!", "<script>", "Max_1", "😀"})
    void rejectsInvalidNames(String name) {
        NameFilter.Result r = filter.check(name);
        assertFalse(r.ok());
        assertFalse(r.error().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Arsch", "derARSCH", "a r s c h", "4rsch", "D00f"})
    void rejectsBadWordsAlsoDisguised(String name) {
        assertEquals("Bitte wähle einen anderen Namen.", filter.check(name).error());
    }

    @Test
    void nullIsEmpty() {
        assertFalse(filter.check(null).ok());
    }

    @Test
    void shippedWordListLoadsAndDoesNotBlockCommonNames() {
        NameFilter shipped = NameFilter.fromFile(Path.of("data/badwords.txt"));

        assertFalse(shipped.check("Fuck").ok());
        for (String name : List.of("Max", "Anna", "Lukas", "Sophie", "Elias", "Emilia", "Paul", "Hannah",
                "Leon", "Mia", "Jonas", "Lea", "Felix", "Marie", "Noah", "Sebastian", "Alexander", "Benedikt")) {
            assertTrue(shipped.check(name).ok(), name);
        }
    }

    @Test
    void missingWordListMeansNoFilter() {
        assertTrue(NameFilter.fromFile(Path.of("does/not/exist.txt")).check("Arsch").ok());
    }
}
