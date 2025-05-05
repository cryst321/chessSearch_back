package org.example.chessearch_back.service;

import org.example.chessearch_back.dto.SearchResultDto;
import org.example.chessearch_back.parser.PositionEncoder;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PositionSearchService {

    private static final Logger log = LoggerFactory.getLogger(PositionSearchService.class);

    private final SearcherManager searcherManager;
    private final PositionEncoder positionEncoder;
    public static final String FIELD_TERMS = IndexingService.FIELD_TERMS;
    public static final String FIELD_FEN_ID = IndexingService.FIELD_FEN_ID;
    public static final String FIELD_GAME_ID = IndexingService.FIELD_GAME_ID;
    public static final String FIELD_MOVE_NUMBER = IndexingService.FIELD_MOVE_NUMBER;
    public static final String FIELD_FEN_STRING = IndexingService.FIELD_FEN_STRING;


    @Autowired
    public PositionSearchService(SearcherManager searcherManager,
                                 PositionEncoder positionEncoder) {
        this.searcherManager = searcherManager;
        this.positionEncoder = positionEncoder;

    }

    /**
     * Searches the Lucene index for positions similar to the query FEN
     * @param queryFen FEN string of the query position
     * @param numResults max number of unique games to return
     * @return list of SearchResultDto representing most similar positions max 1 per game
     */
    public List<SearchResultDto> searchSimilar(String queryFen, int numResults) {
        log.info("Starting search for FEN: {}", queryFen);
        List<SearchResultDto> finalResults = new ArrayList<>();
        Set<Integer> includedGameIds = new HashSet<>();

        IndexSearcher indexSearcher = null;
        try {
            searcherManager.maybeRefresh();
            indexSearcher = searcherManager.acquire();
            IndexReader reader = indexSearcher.getIndexReader();
            log.debug("Searching index with {} documents.", reader.numDocs());

            List<String> queryTerms = positionEncoder.transformFenToDocument(queryFen)
                    .stream()
                    .filter(term -> !term.contains("|"))
                    .collect(Collectors.toList());

            if (queryTerms.isEmpty()) {
                log.warn("Query FEN resulted in no searchable terms: {}", queryFen);
                return finalResults;
            }
            log.debug("Query terms: {}", String.join(" ", queryTerms));


            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            for (String term : queryTerms) {
                queryBuilder.add(new TermQuery(new Term(FIELD_TERMS, term)), BooleanClause.Occur.SHOULD);
            }
            BooleanQuery query = queryBuilder.build();
            log.debug("Executing Lucene query: {}", query.toString(FIELD_TERMS));


            int initialFetchSize = numResults * 5;
            TopDocs topDocs = indexSearcher.search(query, initialFetchSize);
            log.info("Query yielded {} total hits.", topDocs.totalHits.value);


            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                if (finalResults.size() >= numResults) {
                    break;
                }

                int docId = scoreDoc.doc;
                Document hitDoc = indexSearcher.storedFields().document(docId,
                        Set.of(FIELD_GAME_ID, FIELD_FEN_STRING, FIELD_FEN_ID, FIELD_MOVE_NUMBER));

                int gameId = Integer.parseInt(hitDoc.get(FIELD_GAME_ID));
                String fenString = hitDoc.get(FIELD_FEN_STRING);
                String fenId = hitDoc.get(FIELD_FEN_ID);

                IndexableField moveNumField = hitDoc.getField(FIELD_MOVE_NUMBER);
                int moveNumber = -1;
                if (moveNumField != null && moveNumField.numericValue() != null) {
                    moveNumber = moveNumField.numericValue().intValue();
                } else {
                    log.warn("Move number field missing or not numeric for docId: {}, fenId: {}", docId, fenId);
                }

                if (!includedGameIds.contains(gameId)) {
                    includedGameIds.add(gameId);

                    SearchResultDto resultDto = new SearchResultDto();
                    resultDto.setGameId(gameId);
                    resultDto.setPositionFen(fenString);
                    resultDto.setMoveNumber(moveNumber);

                    finalResults.add(resultDto);
                    log.debug("Added result: Game ID {}, FEN ID {}, Move No {}, Score {}",
                            gameId, fenId, moveNumber, scoreDoc.score);
                }
            }

        } catch (IOException e) {
            log.error("Error acquiring or using IndexSearcher: {}", e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("Error processing query FEN '{}': {}", queryFen, e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during search for FEN '{}': {}", queryFen, e.getMessage(), e);
        } finally {
            if (indexSearcher != null) {
                try {
                    searcherManager.release(indexSearcher);
                    log.debug("IndexSearcher released.");
                } catch (IOException e) {
                    log.error("Error releasing IndexSearcher: {}", e.getMessage(), e);
                }
            }
        }

        log.info("Search completed for FEN: {}. Found {} unique game results.", queryFen, finalResults.size());
        return finalResults;
    }


}