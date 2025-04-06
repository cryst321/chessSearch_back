package org.example.chessearch_back.model;


public class ChessGame {
    private Long id;
    private String pgn;

    public ChessGame() {
    }

    public ChessGame(Long id, String pgn) {
        this.id = id;
        this.pgn = pgn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPgn() {
        return pgn;
    }

    public void setPgn(String pgn) {
        this.pgn = pgn;
    }

    @Override
    public String toString() {
        return "ChessGame{" +
                "id=" + id +
                ", pgn='" + pgn + '\'' +
                '}';
    }
}