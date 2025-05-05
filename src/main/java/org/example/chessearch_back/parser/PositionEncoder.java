package org.example.chessearch_back.parser;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGeneratorException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Encodes a chess position (FEN) into a list of terms based on the approach described in Ganguly et al., SIGIR 2014.
 */
@Component
public class PositionEncoder {
    private static final int[][] ROOK_OFFSETS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
    private static final int[][] BISHOP_OFFSETS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] QUEEN_OFFSETS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};



    public PositionEncoder() {
    }
    public static void main(String[] args) {
        // Kh1, Qh7, kb6
       // String testFen = "8/7Q/1k6/8/8/8/8/7K w - - 0 1";

        // Kh1, Nc6, bf3, ph2, kb6
     //  String testFen = "8/8/1kN5/8/8/5b2/7p/7K w - -

        // Nf2 ph2 Nd3 bf4 Pc5 ka6 Kh8
        // String testFen = "7K/8/k7/2P5/5b2/3N4/5N1p/8 w - - 0 1";
      //  String testFen = "8/8/1kp5/1p6/5N2/3b4/8/2RR3K w - - 0 1";

        // Qc1 Rf1 Pd2 Nf2 ph2 Nd3 bf4 Pc5 ka6 Kh8
       // String testFen = "7K/8/k7/2P2p2/5b2/3N4/3P1N1p/2Q2R2 w - - 0 1";

        // weird castling
       // String testFen = "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1";

        //Ra1 Re1 Kg1 Pe2 qd3 ke8 - ray attack king, rooks defend each other
       // String testFen = "4k3/8/8/8/8/3q4/4P3/R3R1K1 w Q - 0 1";

        //debut position pinned knight
        //String testFen = "rnb1kbnr/pp1p1ppp/8/q1p1p3/3PPP2/2N5/PPP3PP/R1BQKBNR w KQkq - 0 1";

        //pawn promotion and two kings
        //String testFen = "k7/4P3/8/8/8/8/8/K7 w - - 0 1";

        //complex crowded position
        //String testFen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

        //mutual ray attacks, defenses of pawns
        //String testFen = "1k1r4/pp1b1pbp/2p1p1p1/4P3/2P2P2/1P4P1/PB4BP/3R2K1 w - - 0 1";

        //stalemate
        String testFen = "7k/5Q2/8/8/8/8/6P1/7K b - - 0 1";

//        System.out.println("Testing FEN: " + testFen);

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

        List<String> truePositionTerms = generateTruePositionTerms(board);
        List<String> reachableTerms = generateReachableTerms(board);
        List<String> attackTerms = generateAttackTerms(board);
        List<String> defenseTerms = generateDefenseTerms(board);
        List<String> rayAttackTerms = generateRayAttackTerms(board);

        allTerms.addAll(truePositionTerms);
        allTerms.addAll(reachableTerms);
        allTerms.addAll(attackTerms);
        allTerms.addAll(defenseTerms);
        allTerms.addAll(rayAttackTerms);
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
        String notation;

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


    /**
     * Helper method to get attack terms
     * @param board The current board
     * @return attack terms
     */
    private List<String> generateAttackTerms(Board board) {
        List<String> attackTerms = new ArrayList<>();
        generateAttackTermsForSide(board, Side.WHITE, attackTerms);
        generateAttackTermsForSide(board, Side.BLACK, attackTerms);
        return attackTerms;
    }

    private void generateAttackTermsForSide(Board board, Side attackingSide, List<String> attackTerms) {
        Side originalSideToMove = board.getSideToMove();
        board.setSideToMove(attackingSide);

        List<Move> legalMoves;
        try {
            legalMoves = board.legalMoves();
        } catch (MoveGeneratorException e) {
            System.err.println("Error generating legal moves for attack check (player " + attackingSide + ") FEN: " + board.getFen() + " - " + e.getMessage());
            board.setSideToMove(originalSideToMove);
            return;
        } finally {
            board.setSideToMove(originalSideToMove);
        }

        for (Move move : legalMoves) {
            Square fromSquare = move.getFrom();
            Square toSquare = move.getTo();
            Piece attackerPiece = board.getPiece(fromSquare);
            Piece attackedPiece = board.getPiece(toSquare);

            if (attackedPiece != null && attackedPiece != Piece.NONE && attackedPiece.getPieceSide() != attackingSide) {

                String attackerNotation = getPieceNotation(attackerPiece);
                String attackedNotation = getPieceNotation(attackedPiece);
                String targetSquareNotation = toSquare.toString().toLowerCase();

                if (attackerNotation != null && attackedNotation != null) {
                    attackTerms.add(attackerNotation + ">" + attackedNotation + targetSquareNotation);
                }
            }
        }
    }
    /**
     * Helper method to get defense terms
     * @param board The current board
     * @return defense terms
     */
    private List<String> generateDefenseTerms(Board board) {
        List<String> defenseTerms = new ArrayList<>();
        for (Square targetSquare : Square.values()) {
            Piece defendedPiece = board.getPiece(targetSquare);

            //blank squares and kings don't count
            if (defendedPiece != null && defendedPiece != Piece.NONE && defendedPiece != Piece.BLACK_KING  && defendedPiece != Piece.WHITE_KING) {
                Side defendingSide = defendedPiece.getPieceSide();
                Side opponentSide = defendingSide.flip();
                PieceType defendedType = defendedPiece.getPieceType();

                //System.out.println("----------DEBUG: Checking defense for " + getPieceNotation(defendedPiece) + " on " + targetSquare.toString().toLowerCase());

                Piece tempOpponentPiece = Piece.make(opponentSide, defendedType);
                if (tempOpponentPiece == null) {
                    System.err.println("ERROR: Could not create temporary opponent piece for type " + defendedType);
                    continue;
                }
               // else System.out.println(tempOpponentPiece);

                Side originalSideToMove = board.getSideToMove();
                board.unsetPiece(defendedPiece,targetSquare);
                board.setPiece(tempOpponentPiece, targetSquare);
                board.setSideToMove(defendingSide);
               // System.out.println("DEBUG: Board state for defense check:\n" + board);
             //   System.out.println(board.getFen());
                List<Move> opponentMoves;
                try {
                    opponentMoves = board.legalMoves();
                   // System.out.println("DEBUG: Found " + opponentMoves.size() + " legal moves for " + defendingSide);
                    //List<Move> pseudo = board.pseudoLegalMoves();
                    //System.out.println("DEBUG: Found " + pseudo.size() + " pseudo moves for " + defendingSide);


                } catch (Exception e) {
                    System.err.println("Error generating moves for defense check (opponent " + opponentSide + ") FEN: " + board.getFen() + " - " + e.getMessage());
                    opponentMoves = new ArrayList<>();
                }

                for (Move move : opponentMoves) {
                  // System.out.print(move + "=> ");
                   // System.out.println(board.getPiece(move.getTo()) + " target square: " + targetSquare + ", ");
                    if (move.getTo().toString().equals(targetSquare.toString())) {
                      //  System.out.println(board.getPiece(targetSquare));
                        Square defenderSquare = move.getFrom();
                        Piece defenderPiece = board.getPiece(defenderSquare);
                       // System.out.println(defenderPiece + " defends " + defendedPiece);
                        //System.out.println("DEBUG: Potential defender found: " + getPieceNotation(defenderPiece) + " on " + defenderSquare.toString().toLowerCase() + " targeting " + targetSquare.toString().toLowerCase());

                        if (defenderPiece != null && defenderPiece != Piece.NONE && defenderPiece.getPieceSide() == defendingSide) {
                            String defenderNotation = getPieceNotation(defenderPiece);
                            String defendedNotation = getPieceNotation(defendedPiece);
                            String targetSquareNotation = targetSquare.toString().toLowerCase();

                            if (defenderNotation != null && defendedNotation != null) {
                                String defenseTerm = defenderNotation + "<" + defendedNotation + targetSquareNotation;
                                //System.out.println("DEBUG: Adding defense term: " + defenseTerm);
                                defenseTerms.add(defenseTerm);
                            }
                        } else {
                           // System.out.println("DEBUG: Defender piece check failed for piece on " + defenderSquare + " (Piece: " + defenderPiece + ", Expected Side: " + defendingSide + ")");
                        }
                    }
                }
                board.unsetPiece(tempOpponentPiece,targetSquare);
                board.setPiece(defendedPiece, targetSquare);
                board.setSideToMove(originalSideToMove);
            }

        }
        //System.out.println("DEBUG: Finished generateDefenseTerms. Found " + defenseTerms.size() + " terms.");
        return defenseTerms;
    }

    /**
     * Helper method to get ray attack terms
     * @param board The current board
     * @return ray attack terms
     */
    private List<String> generateRayAttackTerms(Board board) {
        List<String> rayAttackTerms = new ArrayList<>();

        for (Square fromSquare : Square.values()) {
            Piece attackerPiece = board.getPiece(fromSquare);

            if (attackerPiece != null && attackerPiece != Piece.NONE &&
                    (attackerPiece.getPieceType() == PieceType.ROOK ||
                            attackerPiece.getPieceType() == PieceType.BISHOP ||
                            attackerPiece.getPieceType() == PieceType.QUEEN)) {

                int[][] offsets;
                if (attackerPiece.getPieceType() == PieceType.ROOK) {
                    offsets = ROOK_OFFSETS;
                } else if (attackerPiece.getPieceType() == PieceType.BISHOP) {
                    offsets = BISHOP_OFFSETS;
                } else {
                    offsets = QUEEN_OFFSETS;
                }

                String attackerNotation = getPieceNotation(attackerPiece);
                Side attackerSide = attackerPiece.getPieceSide();

                for (int[] offset : offsets) {
                    int fileOffset = offset[0];
                    int rankOffset = offset[1];

                    boolean foundIntermediatePiece = false;

                    for (int i = 1; i < 8; i++) {
                        int currentFile = fromSquare.getFile().ordinal() + i * fileOffset;
                        int currentRank = fromSquare.getRank().ordinal() + i * rankOffset;

                        //check for end of the board**/
                        if (currentFile < 0 || currentFile > 7 || currentRank < 0 || currentRank > 7) {
                            break;
                        }

                        Square raySquare = Square.squareAt(currentFile + currentRank * 8);
                        Piece pieceOnRay = board.getPiece(raySquare);

                        if (pieceOnRay != null && pieceOnRay != Piece.NONE) {
                            //ray attack occurs if there is an opponent piece on path, and intervening piece exists
                            if (pieceOnRay.getPieceSide() != attackerSide && foundIntermediatePiece) {
                                String attackedNotation = getPieceNotation(pieceOnRay);
                                String targetSquareNotation = raySquare.toString().toLowerCase();

                                if (attackerNotation != null && attackedNotation != null) {
                                    //R=qd8
                                    rayAttackTerms.add(attackerNotation + "=" + attackedNotation + targetSquareNotation);
                                }
                            }

                            foundIntermediatePiece = true;

                        }
                    }
                }
            }
        }
        return rayAttackTerms;
    }

}
