package org.example.chessearch_back.dto;

import org.example.chessearch_back.model.FenPosition;

import java.time.LocalDate;
import java.util.List;
public class ChessGameDto {
    private String pgn;
    private String white;
    private String black;
    private String result;
    private String event;
    private String site;
    private LocalDate date;
    private List<FenPosition> positions;

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

    public String getPgn() {
        return pgn;
    }

    public void setPgn(String pgn) {
        this.pgn = pgn;
    }

    public List<FenPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<FenPosition> positions) {
        this.positions = positions;
    }
}