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
    public String getWhite() {
        return white;
    }

    public void setWhite(String white) {
        this.white = white;
    }

    public String getBlack() {
        return black;
    }

    public void setBlack(String black) {
        this.black = black;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getWhiteElo() {
        return whiteElo;
    }

    public void setWhiteElo(Integer whiteElo) {
        this.whiteElo = whiteElo;
    }

    public Integer getBlackElo() {
        return blackElo;
    }

    public void setBlackElo(Integer blackElo) {
        this.blackElo = blackElo;
    }

    public String getEco() {
        return eco;
    }

    public void setEco(String eco) {
        this.eco = eco;
    }

    @Override
    public String toString() {
        return "ChessGame{" +
                "id=" + id +
                ", pgn='" + pgn + '\'' +
                '}';
    }
}