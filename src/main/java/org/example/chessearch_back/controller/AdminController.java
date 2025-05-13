package org.example.chessearch_back.controller;

import org.example.chessearch_back.service.GameManagementService;
import org.example.chessearch_back.service.IndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for admin tasks
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final IndexingService indexingService;
    private final GameManagementService gameManagementService;

    @Autowired
    public AdminController(IndexingService indexingService, GameManagementService gameManagementService) {
        this.indexingService = indexingService;
        this.gameManagementService = gameManagementService;
    }

    /**
     * Handles POST requests to trigger a full rebuild of the Lucene index
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rebuildIndex() {
        log.warn("Received request to rebuild Lucene index");
        try {
            indexingService.buildIndex();

            String message = "Lucene index rebuild initiated";
            log.info(message);
            return ResponseEntity.ok(message);

        } catch (Exception e) {
            log.error("Error during manual index rebuild trigger: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to rebuild index: " + e.getMessage());
        }
    }

    /**
     * Handles POST requests to clear the Lucene index
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/clear")
    public ResponseEntity<String> clearIndex() {
        log.warn("Received request to clear Lucene index.");
        try {
            indexingService.clearIndex();
            String message = "Lucene index cleared successfully.";
            log.info(message);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Error during manual index clear trigger: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to clear index: " + e.getMessage());
        }
    }

    /**
     * Handles POST requests to upload a PGN file, process it, save games to the database, and update Lucene index.
     * @param pgnFile The PGN file uploaded by the admin
     * @return ResponseEntity
     */
    @PostMapping("/upload-pgn-file")
    public ResponseEntity<String> uploadAndProcessPgnFile(@RequestParam("pgnFile") MultipartFile pgnFile) {
        if (pgnFile.isEmpty()) {
            log.warn("Upload request received with an empty file.");
            return ResponseEntity.badRequest().body("Please select a PGN file to upload.");
        }

        String originalFilename = pgnFile.getOriginalFilename();
        log.info("Received PGN file upload: {}", originalFilename);

        try {
            List<Integer> newGameIds = gameManagementService.processAndSavePgn(pgnFile);

            if (newGameIds == null || newGameIds.isEmpty()) {
                log.warn("PGN file {} processed, but no new games were added.", originalFilename);
                return ResponseEntity.ok("File processed, but no new games were added.");
            }
            log.info("Successfully saved {} games from PGN file {} to database.", newGameIds.size(), originalFilename);

            indexingService.indexNewGames(newGameIds);
            log.info("Successfully submitted {} new games for Lucene indexing.", newGameIds.size());

            return ResponseEntity.ok(String.format("Successfully processed PGN file '%s'. Added %d games to database and index.",
                    originalFilename, newGameIds.size()));

        } catch (IllegalArgumentException e) {
            log.error("Invalid PGN file format or content in {}: {}", originalFilename, e.getMessage());
            return ResponseEntity.badRequest().body("Invalid PGN file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process PGN file {}: {}", originalFilename, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not process PGN file: " + e.getMessage());
        }
    }

    /**
     * Handles POST requests to upload PGN data as a raw string, process it, save games to the database, and update Lucene index.
     * @param pgnStringData The raw PGN string data from the request body
     * @return ResponseEntity
     */
    @PostMapping("/upload-pgn-string")
    public ResponseEntity<String> uploadAndProcessPgnString(@RequestBody String pgnStringData) {
        if (pgnStringData == null || pgnStringData.trim().isEmpty()) {
            log.warn("Upload PGN string request received with empty data.");
            return ResponseEntity.badRequest().body("Please provide PGN data in the request body.");
        }

        log.info("Received PGN string data for processing (length: {} chars).", pgnStringData.length());

        try {
            List<Integer> newGameIds = gameManagementService.processAndSavePgnString(pgnStringData);

            if (newGameIds == null || newGameIds.isEmpty()) {
                log.warn("PGN string data processed, but no new games were added to the database.");
                return ResponseEntity.ok("PGN data processed, but no new games were added.");
            }
            log.info("Successfully saved {} games from PGN string data to database.", newGameIds.size());
            indexingService.indexNewGames(newGameIds);
            log.info("Successfully submitted {} new games for Lucene indexing.", newGameIds.size());
            return ResponseEntity.ok(String.format("Successfully processed PGN string data. Added %d games to database and index.",
                    newGameIds.size()));

        } catch (IllegalArgumentException e) {
            log.error("Invalid PGN string format or content: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid PGN data: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process PGN string data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not process PGN string data: " + e.getMessage());
        }
    }
}

