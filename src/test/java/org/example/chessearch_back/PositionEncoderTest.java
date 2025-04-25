package org.example.chessearch_back;

import org.example.chessearch_back.parser.PositionEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class PositionEncoderTest {

    private PositionEncoder positionEncoder;

    private static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @BeforeEach
    void setUp() {
        positionEncoder = new PositionEncoder();
    }

    @Test
    @DisplayName("Should generate correct true position terms for starting FEN")
    void testTransformFenToDocument_StartPosition_TruePositionTerms() {
        List<String> expectedTerms = Arrays.asList(
                "ra8", "nb8", "bc8", "qd8", "ke8", "bf8", "ng8", "rh8",
                "pa7", "pb7", "pc7", "pd7", "pe7", "pf7", "pg7", "ph7",
                "Pa2", "Pb2", "Pc2", "Pd2", "Pe2", "Pf2", "Pg2", "Ph2",
                "Ra1", "Nb1", "Bc1", "Qd1", "Ke1", "Bf1", "Ng1", "Rh1"
        );

        List<String> actualTerms = positionEncoder.transformFenToDocument(STARTING_FEN);

        System.out.println("--- Generated Document (True Positions) for Starting FEN ---");
        String documentString = actualTerms.stream().collect(Collectors.joining(" "));
        System.out.println(documentString);
        System.out.println("------------------------------------------------------------");
        assertNotNull(actualTerms, "Returned list should not be null");

        assertEquals(
                new HashSet<>(expectedTerms),
                new HashSet<>(actualTerms),
                "The set of generated terms should match the expected terms."
        );

        assertEquals(expectedTerms.size(), actualTerms.size(), "Number of terms should match number of pieces");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null FEN")
    void testTransformFenToDocument_NullFen() {
        assertThrows(NullPointerException.class, () -> {
            positionEncoder.transformFenToDocument(null);
        }, "Should throw NullPointerException for null FEN input");
    }
    @Test
    @DisplayName("Should throw IllegalArgumentException for empty FEN")
    void testTransformFenToDocument_EmptyFen() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionEncoder.transformFenToDocument("");
        }, "Should throw IllegalArgumentException for empty FEN input");
        assertThrows(IllegalArgumentException.class, () -> {
            positionEncoder.transformFenToDocument("   ");
        }, "Should throw IllegalArgumentException for blank FEN input");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid FEN")
    void testTransformFenToDocument_InvalidFen() {
        /**extra slash in the end**/
        String invalidFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR/ w KQkq - 0 1";
        assertThrows(IllegalArgumentException.class, () -> {
            positionEncoder.transformFenToDocument(invalidFen);
        }, "Should throw IllegalArgumentException for invalid FEN structure");

        String invalidFenFields = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -";
        assertThrows(IllegalArgumentException.class, () -> {
            positionEncoder.transformFenToDocument(invalidFenFields);
        }, "Should throw IllegalArgumentException for wrong number of FEN fields");
    }


}
