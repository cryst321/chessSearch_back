package org.example.chessearch_back.service;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.pgn.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for business logic related to parsing PGN
 */
@Service
public class PgnParserService {

    public List<String> parsePgnToFens(String pgn) throws Exception {
        try {
            if (!isValidPgn(pgn)) {
                throw new IllegalArgumentException("Invalid or incomplete PGN data: missing required tags or moves.");
            }

            File tempFile = File.createTempFile("temp", ".pgn");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(pgn);
            }

            PgnHolder holder = new PgnHolder(tempFile.getAbsolutePath());
            holder.loadPgn();

            List<String> fens = new ArrayList<>();

            for (Game game : holder.getGames()) {
                game.loadMoveText();
                Board board = new Board();
                fens.add(board.getFen());

                for (Move move : game.getHalfMoves()) {
                    board.doMove(move);
                    String currentFen = board.getFen();
                    System.out.println("After move " + move.toString() + ": " + currentFen);
                    fens.add(board.getFen());
                }
            }
            tempFile.delete();

            if (fens.isEmpty()) {
                throw new IllegalArgumentException("No valid moves found in PGN data.");
            }

            return fens;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid PGN format: " + e.getMessage());
        }
    }

    private boolean isValidPgn(String pgn) {
        if (pgn == null || pgn.trim().isEmpty()) {
            return false;
        }

        String[] requiredTags = {"Event", "White", "Black", "Result"};
        for (String tag : requiredTags) {
            if (!pgn.contains("[" + tag + " ")) {
                return false;
            }
        }

        if (!pgn.contains("1.")) {
            return false;
        }

        String[] validResults = {"1-0", "0-1", "1/2-1/2", "*"};
        boolean hasValidResult = false;
        for (String result : validResults) {
            if (pgn.trim().endsWith(result)) {
                hasValidResult = true;
                break;
            }
        }
        if (!hasValidResult) {
            return false;
        }

        int bracketCount = 0;
        boolean inComment = false;
        for (char c : pgn.toCharArray()) {
            if (c == '{') {
                inComment = true;
                bracketCount++;
            } else if (c == '}') {
                inComment = false;
                bracketCount--;
            }
            if (bracketCount < 0) {
                throw new IllegalArgumentException("Invalid PGN format: unmatched closing bracket '}' found.");
            }
        }
        if (bracketCount != 0) {
            throw new IllegalArgumentException("Invalid PGN format: unmatched opening bracket '{' found.");
        }
        if (inComment) {
            throw new IllegalArgumentException("Invalid PGN format: unclosed comment found.");
        }

        return true;
    }
}