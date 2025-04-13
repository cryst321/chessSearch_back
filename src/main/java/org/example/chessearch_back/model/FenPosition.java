package org.example.chessearch_back.model;

public class FenPosition {
    private int id;
    private int gameId;
    private int moveNumber;
    private String fen;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    @Override
    public String toString() {
        return "FenPosition{" +
                "id=" + id +
                ", gameId=" + gameId +
                ", moveNumber=" + moveNumber +
                ", fen='" + fen + '\'' +
                '}';
    }
}
