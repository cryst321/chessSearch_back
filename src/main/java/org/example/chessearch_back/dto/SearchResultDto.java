package org.example.chessearch_back.dto;

/**
 * DTO representing a single search result item, containing the game ID and the specific FEN position found by the search.
 */
public class SearchResultDto {

    private int gameId;
    private int moveNumber;
    private String positionFen;

    public SearchResultDto() {
    }

    public SearchResultDto(int gameId, int moveNumber,String positionFen) {
        this.gameId = gameId;
        this.moveNumber = moveNumber;
        this.positionFen = positionFen;
    }

    public int getGameId() {
        return gameId;
    }
    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
    public String getPositionFen() {
        return positionFen;
    }
    public void setPositionFen(String positionFen) {
        this.positionFen = positionFen;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }
}
