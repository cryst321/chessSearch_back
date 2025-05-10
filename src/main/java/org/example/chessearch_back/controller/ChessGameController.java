package org.example.chessearch_back.controller;

import org.example.chessearch_back.dto.ChessGameDto;
import org.example.chessearch_back.dto.GamePreviewDto;
import org.example.chessearch_back.dto.PaginatedGamePreviewsDto;
import org.example.chessearch_back.service.ChessGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for handling HTTP requests related to Chess Games
 */
@RestController
@RequestMapping("/api/game")
public class ChessGameController {

    private static final Logger log = LoggerFactory.getLogger(ChessGameController.class);

    private final ChessGameService chessGameService;

    /** @param chessGameService service responsible for chess game logic
     */
    @Autowired
    public ChessGameController(ChessGameService chessGameService) {
        this.chessGameService = chessGameService;
    }

    /**
     * Handles GET requests to retrieve a specific chess game by its ID
     * /api/game/{id}
     * @param id The ID of the game
     * @return A ResponseEntity containing the ChessGameDto and HTTP status 200 (OK) if found,
     * or empty body with HTTP status 404 (Not Found) if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChessGameDto> findGameById(@PathVariable int id) {
        log.info("Received request to find game by ID: {}", id);

        Optional<ChessGameDto> gameDtoOptional = chessGameService.getGameById(id);

        if (gameDtoOptional.isPresent()) {
            log.info("Game found for ID: {}", id);
            return ResponseEntity.ok(gameDtoOptional.get());
        } else {
            log.warn("Game not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }

    }

     /**
      * Handles GET requests to retrieve a paginated list of game previews.
      * /api/game
      * Example: /api/game?page=0&size=10
      * @param page The page number. Default 0
      * @param size The number of items per page. Default 10
      * @return A ResponseEntity containing a List of GamePreviewDto and HTTP status 200 (OK)*/
    @GetMapping
    public ResponseEntity<PaginatedGamePreviewsDto> findGamePreviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Received request for game previews: page={}, size={}", page, size);

        int effectiveSize = size;
        if (effectiveSize > 50) {
            effectiveSize = 50;
            log.warn("Requested size {} too large, setting to {}", size, effectiveSize);
        }
        if (effectiveSize <= 0) {
            effectiveSize = 10;
            log.warn("Requested size {} invalid, using default {}", size, effectiveSize);
        }
        int effectivePage = page;
        if (effectivePage < 0) {
            effectivePage = 0;
            log.warn("Requested page {} invalid, using default {}", page, effectivePage);
        }


        PaginatedGamePreviewsDto paginatedPreviews = chessGameService.getGamePreviews(effectivePage, effectiveSize);

        log.info("Returning {} game previews (total games: {}) for page={}, size={}",
                paginatedPreviews.getPreviews().size(),
                paginatedPreviews.getTotalGames(),
                effectivePage,
                effectiveSize);

        return ResponseEntity.ok(paginatedPreviews);
    }

}