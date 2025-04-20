package org.example.chessearch_back.model;


import java.time.LocalDate;

public class ChessGame {
        private int id;
        private String pgn;
        private String white;
        private String black;
        private String result;
        private String event;
        private String site;
        private LocalDate date;

    private Integer whiteElo;
    private Integer blackElo;
    private String eco;

    public ChessGame() {
    }

    public ChessGame(int id, String pgn) {
        this.id = id;
        this.pgn = pgn;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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