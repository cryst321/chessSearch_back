package org.example.chessearch_back.controller;

import org.example.chessearch_back.service.IndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for admin tasks
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final IndexingService indexingService;

    @Autowired
    public AdminController(IndexingService indexingService) {
        this.indexingService = indexingService;
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

}