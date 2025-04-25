package org.example.chessearch_back.parser;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Encodes a chess position (FEN) into a list of terms based on the approach described in Ganguly et al., SIGIR 2014.
 */
public class PositionEncoder {


    public PositionEncoder() {
    }

    /**
     * Transforms a FEN string into a list of terms representing the position for future indexing
     * @param fen The FEN string representing the board position
     * @return A List of String terms representing the positions as document.
     * @throws IllegalArgumentException if the FEN string is invalid.
     */
    public List<String> transformFenToDocument(String fen) throws IllegalArgumentException {
        Objects.requireNonNull(fen, "FEN string is null");
        String trimmedFen = fen.trim();
        if (trimmedFen.isEmpty()) {
            throw new IllegalArgumentException("FEN string is empty");
        }

        String[] fenParts = trimmedFen.split("\\s+");
        if (fenParts.length != 6) {
            throw new IllegalArgumentException(
                    String.format("Invalid FEN: must have 6 space-separated fields. Found %d in '%s'", fenParts.length, trimmedFen)
            );
        }
        String piecePlacement = fenParts[0];
        // Count the slashes in the piece placement part
        long slashCount = piecePlacement.chars().filter(ch -> ch == '/').count();
        if (slashCount != 7) {
            throw new IllegalArgumentException(
                    String.format("Invalid FEN: must have 7 slashes. Found %d in '%s'", slashCount, piecePlacement)
            );
        }
        Board board = new Board();
        try {
            board.loadFromFen(fen);
        } catch (Exception e) {
            System.err.println("Error loading FEN: " + fen + ", " + e.getMessage());
            throw new IllegalArgumentException("Invalid FEN string provided: " + fen, e);
        }

        List<String> terms = new ArrayList<>();

        for (Square square : Square.values()) {
            Piece piece = board.getPiece(square);

            if (piece != null && piece != Piece.NONE) {
/**generate TRUE POSITION*/
                String pieceNotation = getPieceNotation(piece);
                String squareNotation = square.toString().toLowerCase();

                if (pieceNotation != null) {
                    terms.add(pieceNotation + squareNotation);
                } else {
                    System.err.println("Warning: Could not get notation for piece " + piece + " on square " + square);
                }
            }
        }

        return terms;
    }

    /**
     * Helper method to get the single character notation for a piece
     * @param piece The chesslib Piece object.
     * @return The single character string representation
     */
    private String getPieceNotation(Piece piece) {
        if (piece == null || piece == Piece.NONE) {
            return null;
        }

        PieceType type = piece.getPieceType();
        Side side = piece.getPieceSide();
        String notation = "";

        switch (type) {
            case PAWN:   notation = "p"; break;
            case KNIGHT: notation = "n"; break;
            case BISHOP: notation = "b"; break;
            case ROOK:   notation = "r"; break;
            case QUEEN:  notation = "q"; break;
            case KING:   notation = "k"; break;
            default:     return null;
        }

        if (side == Side.WHITE) {
            return notation.toUpperCase(); // white pieces
        } else {
            return notation; //black pieces
        }
    }


}
