package org.example.chessearch_back.service;

import org.example.chessearch_back.dto.ChessGameDto;
import org.example.chessearch_back.dto.GamePreviewDto;
import org.example.chessearch_back.dto.PaginatedGamePreviewsDto;
import org.example.chessearch_back.model.FenPosition;
import org.example.chessearch_back.repository.ChessGameRepository;
import org.example.chessearch_back.repository.FenPositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;

/**
 * Service layer for business logic related to Chess Games
 */
@Service
public class ChessGameService {

    private static final Logger log = LoggerFactory.getLogger(ChessGameService.class);

    private final ChessGameRepository chessGameRepository;
    private final FenPositionRepository fenPositionRepository;

    /**
     * Constructor injection for repositories
     * @param chessGameRepository Repository for accessing ChessGame data
     * @param fenPositionRepository Repository for accessing FenPosition data
     */
    @Autowired
    public ChessGameService(ChessGameRepository chessGameRepository, FenPositionRepository fenPositionRepository) {
        this.chessGameRepository = chessGameRepository;
        this.fenPositionRepository = fenPositionRepository;
    }

    /**
     * Retrieves a ChessGame by its ID, including associated FEN positions
     * @param id of the chess game
     * @return An Optional containing {@link ChessGameDto} if found.
     */
    @Transactional(readOnly = true)
    public Optional<ChessGameDto> getGameById(int id) {
        log.debug("Attempting to retrieve game with ID: {}", id);
        try {
            ChessGameDto gameDto = chessGameRepository.findById(id);
            log.debug("Found game details for ID: {}", id);

            List<FenPosition> positions = fenPositionRepository.getFensByGameId(id);
            log.debug("Found {} FEN positions for game ID: {}", positions.size(), id);

            gameDto.setPositions(positions);
            return Optional.of(gameDto);

        } catch (EmptyResultDataAccessException e) {

            log.warn("Chess game with ID {} not found.", id);
            return Optional.empty();
        } catch (Exception e) {
            log.error("An unexpected error occurred while retrieving game with ID {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }


    /**
     * Retrieves a paginated list of game previews along with total count, applying filters.
     * @param pageNumber page number (0-based)
     * @param pageSize items per page
     * @param eco ECO code filter
     * @param dateFromString Date from filter (YYYY-MM-DD)
     * @param dateToString Date to filter (YYYY-MM-DD)
     * @param result Result filter
     * @param minElo Min elo filter
     * @param maxElo Max elo filter
     * @param playerName Player name filter
     * @return PaginatedGamePreviewsDto object
     */
    @Transactional(readOnly = true)
    public PaginatedGamePreviewsDto getGamePreviews(int pageNumber, int pageSize,
                                                      String eco, String dateFromString, String dateToString, String result,
                                                      Integer minElo, Integer maxElo, String playerName) {
        int offset = pageNumber * pageSize;
        log.debug("Fetching game previews with limit={}, offset={}, eco={}, dateFrom={}, dateTo={}, result={}, minElo={}, maxElo={}, player={}",
                pageSize, offset, eco, dateFromString, dateToString, result, minElo, maxElo, playerName);
        LocalDate dateFrom = null;
        LocalDate dateTo = null;
        try {
            if (StringUtils.hasText(dateFromString)) dateFrom = LocalDate.parse(dateFromString);
            if (StringUtils.hasText(dateToString)) dateTo = LocalDate.parse(dateToString);
        } catch (DateTimeParseException e) {log.warn("Invalid date format for filtering: {}", e.getMessage());}
        try {
            List<GamePreviewDto> previews = chessGameRepository.findGamePreviews(
                    pageSize, offset, eco, dateFrom, dateTo, result, minElo, maxElo, playerName);
            long totalGames = chessGameRepository.countTotalGames(
                    eco, dateFrom, dateTo, result, minElo, maxElo, playerName);

            return new PaginatedGamePreviewsDto(previews, totalGames, pageNumber, pageSize);

        } catch (Exception e) {
            log.error("Error fetching game previews with filters: error={}", e.getMessage(), e);
            return new PaginatedGamePreviewsDto(Collections.emptyList(), 0, pageNumber, pageSize);
        }
    }
}
