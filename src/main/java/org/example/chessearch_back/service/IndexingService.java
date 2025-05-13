package org.example.chessearch_back.service;

import org.example.chessearch_back.model.FenPosition;
import org.example.chessearch_back.parser.PositionEncoder;
import org.example.chessearch_back.repository.ChessGameRepository;
import org.example.chessearch_back.repository.FenPositionRepository;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private static final int NUM_SKIP_MOVES = 24;

    public static final String FIELD_TERMS = "terms";
    public static final String FIELD_FEN_ID = "fen_id";
    public static final String FIELD_GAME_ID = "game_id";
    public static final String FIELD_MOVE_NUMBER = "move_number";
    public static final String FIELD_FEN_STRING = "fen_string";


    private final FenPositionRepository fenPositionRepository;
    private final ChessGameRepository chessGameRepository;
    private final PositionEncoder positionEncoder;
    private final IndexWriter indexWriter;

    @Autowired
    public IndexingService(FenPositionRepository fenPositionRepository,
                           ChessGameRepository chessGameRepository,
                           PositionEncoder positionEncoder,
                           IndexWriter indexWriter) {
        this.fenPositionRepository = fenPositionRepository;
        this.chessGameRepository = chessGameRepository;
        this.positionEncoder = positionEncoder;
        this.indexWriter = indexWriter;
    }

    /**
     * Builds or rebuilds the entire Lucene index from the FEN positions in the database
     */
    public void buildIndex() {
        log.info("Starting Lucene index build process...");
        long startTime = System.currentTimeMillis();
        long totalDocumentsIndexed = 0;
        long totalDocumentsSkipped = 0;
        long totalGamesProcessed = 0;

        clearIndex();

        List<Integer> allGameIds = chessGameRepository.findAllGameIds();
        log.info("Found {} games to potentially index.", allGameIds.size());

        for (Integer gameId : allGameIds) {
            long indexedInGame = 0;
            long skippedInGame = 0;
            log.debug("Processing game ID: {}", gameId);
            try {
                List<FenPosition> positionsInGame = fenPositionRepository.getFensByGameId(gameId);

                for (FenPosition fenPos : positionsInGame) {
                    if (fenPos.getMoveNumber() > NUM_SKIP_MOVES) {
                        try {
                            indexSinglePosition(fenPos);
                            indexedInGame++;
                        } catch (IOException | IllegalArgumentException e) {
                            log.error("Failed to index FEN ID {} (Game ID {}): {}", fenPos.getId(), gameId, e.getMessage());
                        }
                    } else {
                        skippedInGame++;
                    }
                }
                totalDocumentsIndexed += indexedInGame;
                totalDocumentsSkipped += skippedInGame;
                totalGamesProcessed++;
                if (totalGamesProcessed % 100 == 0) {
                    log.info("Progress: Processed {} / {} games", totalGamesProcessed, allGameIds.size());
                }

            } catch (Exception e) {
                log.error("Failed to process positions for game ID {}: {}", gameId, e.getMessage(), e);
            }
        }


        try {
            log.info("Committing final changes to Lucene index...");
            indexWriter.commit();
            long endTime = System.currentTimeMillis();
            log.info("Lucene index build completed. Games Processed: {}, Documents Indexed: {}, Documents Skipped: {}. Time: {} ms",
                    totalGamesProcessed, totalDocumentsIndexed, totalDocumentsSkipped, (endTime - startTime));
        } catch (IOException e) {
            log.error("Error committing Lucene index changes", e);

            try { indexWriter.rollback(); } catch (IOException rbEx) {
                log.error("Couldn't roll back");
            }
        }
    }

    /**
     * Clears the entire Lucene index
     */
    public void clearIndex() {
        log.warn("Attempting to delete all documents from the Lucene index...");
        try {
            indexWriter.deleteAll();
            indexWriter.commit();
            log.info("Lucene index cleared successfully.");
        } catch (IOException e) {
            log.error("Error clearing Lucene index", e);
        }
    }


    /**
     * Indexes a single FenPosition object
     * @param fenPos The FenPosition object from the database
     * @throws IOException if Lucene fails to add the document
     * @throws IllegalArgumentException if FEN is invalid
     */
    public void indexSinglePosition(FenPosition fenPos) throws IOException, IllegalArgumentException {
        List<String> terms = positionEncoder.transformFenToDocument(fenPos.getFen());
        String termsString = String.join(" ", terms);

        Document doc = new Document();
        doc.add(new TextField(FIELD_TERMS, termsString, Field.Store.NO));

        doc.add(new StoredField(FIELD_FEN_ID, String.valueOf(fenPos.getId())));
        doc.add(new StoredField(FIELD_GAME_ID, String.valueOf(fenPos.getGameId())));
        doc.add(new StoredField(FIELD_MOVE_NUMBER, fenPos.getMoveNumber()));
        doc.add(new StoredField(FIELD_FEN_STRING, fenPos.getFen()));

        indexWriter.addDocument(doc);

    }

    public void indexNewGames(List<Integer> newGameIds) {
        if (newGameIds == null || newGameIds.isEmpty()) {
            log.info("indexNewGames called with no new game IDs. Nothing to do.");
            return;
        }
        log.info("Starting to index {} new games", newGameIds.size());
        long startTime = System.currentTimeMillis();
        long documentsIndexed = 0;
        long documentsSkipped = 0;

        for (Integer gameId : newGameIds) {
            log.debug("Indexing new game ID: {}", gameId);
            try {
                List<FenPosition> positionsInGame = fenPositionRepository.getFensByGameId(gameId);
                for (FenPosition fenPos:positionsInGame) {
                    if (fenPos.getMoveNumber()>NUM_SKIP_MOVES) {
                        try {
                            indexSinglePosition(fenPos);
                            documentsIndexed++;
                        } catch (IOException | IllegalArgumentException e) {
                            log.error("Failed to index FEN ID {} (New Game ID {}): {}", fenPos.getId(), gameId, e.getMessage());
                        }
                    } else {
                        documentsSkipped++;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process positions for new game ID {}: {}", gameId, e.getMessage(), e);
            }
        }

        if (documentsIndexed > 0) {
            try {
                indexWriter.commit();
                long endTime = System.currentTimeMillis();
                log.info("Successfully indexed {} new games ({} documents added, {} skipped). Time: {} ms",
                        newGameIds.size(), documentsIndexed, documentsSkipped, (endTime - startTime));
            } catch (IOException e) {
                log.error("Error committing updated index after adding new games", e);
                try { indexWriter.rollback(); }
                catch (IOException rbEx)
                { log.error("Rollback failed after indexNewGames error", rbEx);}
            }
        } else {
            log.info("No new documents for indexing from the provided game ids.");
        }
    }
}