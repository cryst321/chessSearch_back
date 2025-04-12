package org.example.chessearch_back;

import org.example.chessearch_back.service.PgnParserService;

import java.util.List;

public class PgnParserTest {
    public static void main(String[] args) throws Exception {

        String samplePgn = """
                    [Event "Rated classical game"]
                            [Site "https://lichess.org/GfHJot5M"]
                            [Date "2020.07.24"]
                            [White "Hamid8785"]
                            [Black "xXcrystallixXx"]
                            [Result "0-1"]
                            [GameId "GfHJot5M"]
                            [UTCDate "2020.07.24"]
                            [UTCTime "12:43:52"]
                            [WhiteElo "1511"]
                            [BlackElo "1500"]
                            [WhiteRatingDiff "-16"]
                            [BlackRatingDiff "+185"]
                            [Variant "Standard"]
                            [TimeControl "1800+20"]
                            [ECO "D02"]
                            [Opening "Queen's Pawn Game: Chigorin Variation"]
                            [Termination "Time forfeit"]
                            [Annotator "lichess.org"]
                            
                            1. d4 d5 2. Nf3 Nc6 3. Bg5 f6 4. Bf4 Bf5 5. e3 e5 6. dxe5 fxe5 { White left the game. } 0-1
                """;

        PgnParserService service = new PgnParserService();
        List<String> fens = service.parsePgnToFens(samplePgn);

        System.out.println("Parsed FENs:");
        for (String fen : fens) {
            System.out.println(fen);
        }


    }
}
