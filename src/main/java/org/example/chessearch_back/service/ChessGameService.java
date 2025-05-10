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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Retrieves a paginated list of game previews
     * @param pageNumber page number
     * @param pageSize items per page
     * @return A List of {@link GamePreviewDto} or an empty list
     */
    @Transactional(readOnly = true)
    public PaginatedGamePreviewsDto getGamePreviews(int pageNumber, int pageSize) {
        int offset = pageNumber * pageSize;
        log.debug("Fetching game previews with limit={}, offset={}", pageSize, offset);

        try {
            List<GamePreviewDto> previews = chessGameRepository.findGamePreviews(pageSize, offset);
            long totalGames = chessGameRepository.countTotalGames();

            return new PaginatedGamePreviewsDto(previews, totalGames, pageNumber, pageSize);

        } catch (Exception e) {
            log.error("Error fetching game previews: page={}, size={}, error={}",
                    pageNumber, pageSize, e.getMessage(), e);
            return new PaginatedGamePreviewsDto(Collections.emptyList(), 0, pageNumber, pageSize);
        }
    }
}
