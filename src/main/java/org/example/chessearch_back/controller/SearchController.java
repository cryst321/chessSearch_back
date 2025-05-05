package org.example.chessearch_back.controller;

import org.example.chessearch_back.dto.SearchResultDto;
import org.example.chessearch_back.service.PositionSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST Controller for handling search requests for similar chess positions
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final PositionSearchService positionSearchService;
    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int MAX_SEARCH_LIMIT = 50;

    @Autowired
    public SearchController(PositionSearchService positionSearchService) {
        this.positionSearchService = positionSearchService;
    }

    /**
     * Handles GET requests to search for similar positions based on a FEN string.
     * /api/search?fen=...&limit=20
     * @param fen FEN string representing the query position
     * @param limit The maximum number of unique game results to return (default 10)
     * @return A ResponseEntity containing a List of SearchResultDto or an error response
     */
    @GetMapping
    public ResponseEntity<?> performSearch(
            @RequestParam(name = "fen", required = true) String fen,
            @RequestParam(name = "limit", required = false, defaultValue = "" + DEFAULT_SEARCH_LIMIT) int limit) {

        log.info("Received search request for FEN: '{}', limit: {}", fen, limit);

        if (fen == null || fen.trim().isEmpty()) {
            log.warn("Search request received with empty FEN parameter.");
            return ResponseEntity.badRequest().body("FEN parameter cannot be empty.");
        }
        String trimmedFen = fen.trim();

        int effectiveLimit = limit;
        if (effectiveLimit <= 0) {
            log.warn("Invalid limit '{}' requested, using default {}.", limit, DEFAULT_SEARCH_LIMIT);
            effectiveLimit = DEFAULT_SEARCH_LIMIT;
        } else if (effectiveLimit > MAX_SEARCH_LIMIT) {
            log.warn("Requested limit {} exceeds maximum {}, using {}.", limit, MAX_SEARCH_LIMIT, MAX_SEARCH_LIMIT);
            effectiveLimit = MAX_SEARCH_LIMIT;
        }

        try {
            List<SearchResultDto> results = positionSearchService.searchSimilar(trimmedFen, effectiveLimit);

            if (results.isEmpty()) {
                log.info("No similar positions found for FEN: '{}'", trimmedFen);
                return ResponseEntity.ok(Collections.emptyList());
            } else {
                log.info("Returning {} search results for FEN: '{}'", results.size(), trimmedFen);
                return ResponseEntity.ok(results);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid FEN format provided for search: '{}' - {}", trimmedFen, e.getMessage());
            return ResponseEntity.badRequest().body("Invalid FEN format: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during search for FEN '{}': {}", trimmedFen, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred during search.");
        }
    }
}