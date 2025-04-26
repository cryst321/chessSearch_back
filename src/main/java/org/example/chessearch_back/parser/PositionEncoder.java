package org.example.chessearch_back.parser;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGeneratorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Encodes a chess position (FEN) into a list of terms based on the approach described in Ganguly et al., SIGIR 2014.
 */
public class PositionEncoder {


    public PositionEncoder() {
    }
    public static void main(String[] args) {
        // Kh1, Qh7, kb6
        String testFen = "8/7Q/1k6/8/8/8/8/7K w - - 0 1";

        System.out.println("Testing FEN: " + testFen);

        PositionEncoder encoder = new PositionEncoder();
        try {
            List<String> terms = encoder.transformFenToDocument(testFen);

            System.out.println("\nGenerated document terms (" + terms.size() + " total):");
            String documentString = String.join(" ", terms);
            System.out.println(documentString);

        } catch (IllegalArgumentException e) {
            System.err.println("Error processing FEN: " + e.getMessage());
            e.printStackTrace();
        }
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
                    String.format("Invalid FEN: must have 6 fields. Found %d in '%s'", fenParts.length, trimmedFen)
            );
        }
        String piecePlacement = fenParts[0];
        long slashCount = piecePlacement.chars().filter(ch -> ch == '/').count();
        if (slashCount != 7) {
            throw new IllegalArgumentException(
                    String.format("Invalid FEN: piece placement must have 7 slashes. Found %d in '%s'", slashCount, piecePlacement)
            );
        }

        Board board = new Board();
        try {
            board.loadFromFen(trimmedFen);
        } catch (Exception e) {
            System.err.println("Error loading FEN: '" + trimmedFen + "' - Exception: " + e.getClass().getName() + " - Message: " + e.getMessage());
            throw new IllegalArgumentException("FEN string failed to parse: '" + trimmedFen + "'", e);
        }
        List<String> allTerms = new ArrayList<>();

        allTerms.addAll(generateTruePositionTerms(board));
        allTerms.addAll(generateReachableTerms(board));
        // allTerms.addAll(generateAttackTerms(board));       //TODO: implement
        // allTerms.addAll(generateDefenseTerms(board));      //TODO: implement
        // allTerms.addAll(generateRayAttackTerms(board));    //TODO: implement
        return allTerms;
    }


    /**
     * Helper function for parsing FEN into special notation: true positions
     * @param board position
     * @return true positions of pieces
     */
    private List<String> generateTruePositionTerms(Board board) {
        List<String> terms = new ArrayList<>();
        for (Square square : Square.values()) {
            Piece piece = board.getPiece(square);
            if (piece != null && piece != Piece.NONE) {
                String pieceNotation = getPieceNotation(piece);
                String squareNotation = square.toString().toLowerCase();
                if (pieceNotation != null) {
                    terms.add(pieceNotation + squareNotation);
                } else {
                    System.err.println("Warning: could not get notation for piece " + piece + " on square " + square);
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
            default:
                System.err.println("unknown piece type: " + type);
                return null;
        }


        if (side == Side.WHITE) {
            return notation.toUpperCase(); // white pieces
        } else {
            return notation; //black pieces
        }
    }
    /**
     * Helper method to get reachable squares weights for the board
     * @param board The current board
     * @return weights of reachable squares for all pieces
     */
    private List<String> generateReachableTerms(Board board) {
        List<String> reachableTerms = new ArrayList<>();

        generateReachableTermsForSide(board, Side.WHITE, reachableTerms);
        generateReachableTermsForSide(board, Side.BLACK, reachableTerms);

        return reachableTerms;
    }

    private void generateReachableTermsForSide(Board board, Side side, List<String> reachableTerms) {
        Side originalSideToMove = board.getSideToMove();
        board.setSideToMove(side);

        List<Move> legalMoves;

        try {

            legalMoves = board.legalMoves();
        } catch (MoveGeneratorException e) {
            System.err.println("Error generating legal moves for player " + side + " FEN: " + board.getFen() + " - " + e.getMessage());
            board.setSideToMove(originalSideToMove);
            return;
        } finally {
            board.setSideToMove(originalSideToMove);
        }

        for (Move move : legalMoves) {
            Square fromSquare = move.getFrom();
            Square toSquare = move.getTo();
            Piece movingPiece = board.getPiece(fromSquare);

            if (movingPiece == null || movingPiece.getPieceSide() != side) {
                continue;
            }

            if (board.getPiece(toSquare) == Piece.NONE) {

                int distance = calculateChebyshevDistance(fromSquare, toSquare);
                /* w = 1 - (7 * distance)/64 **/
                double weight = 1.0 - (7.0 * distance / 64.0);


                String formattedWeight = String.format(Locale.US, "%.2f", weight);
                String pieceNotation = getPieceNotation(movingPiece);
                String toSquareNotation = toSquare.toString().toLowerCase();

                if (pieceNotation != null) {
                    reachableTerms.add(pieceNotation + toSquareNotation + "|" + formattedWeight);
                }
            }
        }
    }
    private int calculateChebyshevDistance(Square s1, Square s2) {
        int file1 = s1.getFile().ordinal();
        int rank1 = s1.getRank().ordinal();
        int file2 = s2.getFile().ordinal();
        int rank2 = s2.getRank().ordinal();

        int deltaFile = Math.abs(file1 - file2);
        int deltaRank = Math.abs(rank1 - rank2);

        return Math.max(deltaFile, deltaRank);
    }



    // private List<String> generateAttackTerms(Board board) {
    //     List<String> attackTerms = new ArrayList<>();
    //     return attackTerms;
    // }

    // private List<String> generateDefenseTerms(Board board) {
    //     List<String> defenseTerms = new ArrayList<>();
    //     return defenseTerms;
    // }

    // private List<String> generateRayAttackTerms(Board board) {
    //     List<String> rayAttackTerms = new ArrayList<>();
    //     return rayAttackTerms;
    // }

}
