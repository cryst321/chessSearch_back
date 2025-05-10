package org.example.chessearch_back.dto;

import java.util.List;

/**
 * DTO to hold paginated list of game previews + total number of games
 */
public class PaginatedGamePreviewsDto {

    private List<GamePreviewDto> previews;
    private long totalGames;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    public PaginatedGamePreviewsDto() {
    }

    public PaginatedGamePreviewsDto(List<GamePreviewDto> previews, long totalGames, int currentPage, int pageSize) {
        this.previews = previews;
        this.totalGames = totalGames;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = (pageSize > 0) ? (int) Math.ceil((double) totalGames / pageSize) : 0;
    }

    public List<GamePreviewDto> getPreviews() {
        return previews;
    }

    public void setPreviews(List<GamePreviewDto> previews) {
        this.previews = previews;
    }

    public long getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(long totalGames) {
        this.totalGames = totalGames;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}