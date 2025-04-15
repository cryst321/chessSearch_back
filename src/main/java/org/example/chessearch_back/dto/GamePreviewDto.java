package org.example.chessearch_back.dto;

import java.time.LocalDate;

/**
 * DTO representing a preview of a chess game
 * Includes basic game info and FEN of game's final position
 */
public class GamePreviewDto {

    private int gameId;
    private String white;
    private String black;
    private String result;
    private LocalDate date;
    private String lastFen;

    public GamePreviewDto() {
    }

    public GamePreviewDto(int gameId, String white, String black, String result, LocalDate date, String lastFen) {
        this.gameId = gameId;
        this.white = white;
        this.black = black;
        this.result = result;
        this.date = date;
        this.lastFen = lastFen;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getLastFen() {
        return lastFen;
    }

    public void setLastFen(String lastFen) {
        this.lastFen = lastFen;
    }

    @Override
    public String toString() {
        return "GamePreviewDto{" +
                "gameId=" + gameId +
                ", white='" + white + '\'' +
                ", black='" + black + '\'' +
                ", result='" + result + '\'' +
                ", date=" + date +
                ", lastFen='" + lastFen + '\'' +
                '}';
    }

}