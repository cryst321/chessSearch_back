package org.example.chessearch_back.service;

import org.example.chessearch_back.model.ChessGame;
import org.example.chessearch_back.model.FenPosition;
import org.example.chessearch_back.repository.ChessGameRepository;
import org.example.chessearch_back.repository.FenPositionRepository;
import org.example.chessearch_back.service.PgnParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GameManagementService {

    private static final Logger log = LoggerFactory.getLogger(GameManagementService.class);
    private static final DateTimeFormatter PGN_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private final PgnParserService pgnParserService;
    private final ChessGameRepository chessGameRepository;
    private final FenPositionRepository fenPositionRepository;

    private static final Pattern PGN_TAG_PATTERN = Pattern.compile("\\[\\s*(\\w+)\\s*\"([^\"]*)\"\\s*]");


    @Autowired
    public GameManagementService(PgnParserService pgnParserService,
                                 ChessGameRepository chessGameRepository,
                                 FenPositionRepository fenPositionRepository) {
        this.pgnParserService = pgnParserService;
        this.chessGameRepository = chessGameRepository;
        this.fenPositionRepository = fenPositionRepository;
    }

    /**
     * Processes an uploaded PGN file, saves games and their FENs to the database.
     *
     * @param pgnFile The MultipartFile containing PGN data.
     * @return A list of database IDs for the newly saved games.
     * @throws IOException If an error occurs reading the file.
     * @throws Exception   For other processing errors (e.g., PGN parsing, DB issues).
     */
    @Transactional
    public List<Integer> processAndSavePgn(MultipartFile pgnFile) throws IOException, Exception {
        if (pgnFile.isEmpty()) {
            log.warn("processAndSavePgn called with an empty file.");
            return new ArrayList<>();
        }
        log.info("Processing PGN file: {}", pgnFile.getOriginalFilename());
        // Use a BufferedReader to read the PGN content line by line
        try (InputStream inputStream = pgnFile.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return processPgnWithReader(reader);
        }
    }

    /**
     * Processes a PGN string (one or multiple games), saves games and their FENs in the database
     * @param pgnStringData The String containing PGN data.
     * @return A list of database IDs for saved games.
     */
    @Transactional
    public List<Integer> processAndSavePgnString(String pgnStringData) throws IOException, Exception {
        if (pgnStringData == null || pgnStringData.trim().isEmpty()) {
            log.warn("processAndSavePgnString called with an empty or null string.");
            return new ArrayList<>();
        }
        log.info("Processing PGN string data (length: {} chars)...", pgnStringData.length());
        try (BufferedReader reader = new BufferedReader(new StringReader(pgnStringData))) {
            return processPgnWithReader(reader);
        }
    }

    /**
     * Core logic to process PGN data
     * @param reader The BufferedReader to read PGN data from
     * @return A list of database IDs for saved games
     * @throws IOException If an error occurs reading
     */
    private List<Integer> processPgnWithReader(BufferedReader reader) throws IOException {
        List<Integer> newGameIds = new ArrayList<>();
        StringBuilder currentGamePgn = new StringBuilder();
        String line;
        int gamesProcessedInSource = 0;

        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("[Event ") && !currentGamePgn.isEmpty()) {
                try {
                    Integer newGameId = processSinglePgnGameString(currentGamePgn.toString());
                    if (newGameId != null) {
                        newGameIds.add(newGameId);
                        gamesProcessedInSource++;
                    }
                } catch (Exception e) {
                    log.error("Error processing a single game block from PGN source: {}. PGN snippet: {}",
                            e.getMessage(), currentGamePgn.substring(0, Math.min(200, currentGamePgn.length())));
                }
                currentGamePgn.setLength(0);
            }
            if (!line.trim().isEmpty() || !currentGamePgn.isEmpty() || line.trim().startsWith("[")) {
                currentGamePgn.append(line).append("\n");
            }
        }

        if (!currentGamePgn.isEmpty()) {
            try {
                Integer newGameId = processSinglePgnGameString(currentGamePgn.toString());
                if (newGameId != null) {
                    newGameIds.add(newGameId);
                    gamesProcessedInSource++;
                }
            } catch (Exception e) {
                log.error("Error processing the last game block from PGN source: {}. PGN snippet: {}",
                        e.getMessage(), currentGamePgn.substring(0, Math.min(200, currentGamePgn.length())));
            }
        }
        log.info("Finished processing PGN source. {} games extracted and saved.", gamesProcessedInSource);
        return newGameIds;
    }


    private Integer processSinglePgnGameString(String pgnGameString) throws Exception {
        if (pgnGameString == null || pgnGameString.trim().isEmpty()) {
            return null;
        }

        Map<String, String> tags = extractPgnTags(pgnGameString);

        String substring = pgnGameString.substring(0, Math.min(100, pgnGameString.length()));
        if (!tags.containsKey("Event") || !tags.containsKey("White") || !tags.containsKey("Black")) {
            log.warn("Skipping PGN block due to missing essential tags (Event, White, Black). PGN: {}", substring);
            return null;
        }

        ChessGame gameToSave = new ChessGame();
        gameToSave.setPgn(pgnGameString.trim());
        gameToSave.setWhite(tags.get("White"));
        gameToSave.setBlack(tags.get("Black"));
        gameToSave.setResult(tags.get("Result"));
        gameToSave.setEvent(tags.get("Event"));
        gameToSave.setSite(tags.get("Site"));
        gameToSave.setDate(parsePgnDate(tags.get("UTCDate")));
        gameToSave.setWhiteElo(parsePgnInteger(tags.get("WhiteElo")));
        gameToSave.setBlackElo(parsePgnInteger(tags.get("BlackElo")));
        gameToSave.setEco(tags.get("ECO"));

        Integer gameId = chessGameRepository.saveAndReturnId(gameToSave);
        if (gameId == null) {
            log.error("Failed to save game to database. PGN: {}", substring);
            return null;
        }

        List<String> fens = pgnParserService.parsePgnToFens(pgnGameString);

        if (fens != null && !fens.isEmpty()) {
            List<FenPosition> fenPositionsToSave = new ArrayList<>();
            for (int i = 0; i < fens.size(); i++) {
                FenPosition fenPos = new FenPosition();
                fenPos.setGameId(gameId);
                fenPos.setMoveNumber(i + 1);
                fenPos.setFen(fens.get(i));
                fenPositionsToSave.add(fenPos);
            }
            fenPositionRepository.saveBatch(fenPositionsToSave);
        }

        return gameId;
    }


    private Map<String, String> extractPgnTags(String pgn) {
        Map<String, String> tags = new HashMap<>();
        Matcher matcher = PGN_TAG_PATTERN.matcher(pgn);
        while (matcher.find()) {
            tags.put(matcher.group(1), matcher.group(2));
        }
        return tags;
    }

    private LocalDate parsePgnDate(String dateStr) {
        if (dateStr == null || dateStr.contains("?") || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), PGN_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse date string: '{}' - {}", dateStr, e.getMessage());
            return null;
        }
    }

    private Integer parsePgnInteger(String s) {
        if (s == null || s.trim().isEmpty() || s.equals("?")) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse integer: '{}' - {}", s, e.getMessage());
            return null;
        }
    }
}

