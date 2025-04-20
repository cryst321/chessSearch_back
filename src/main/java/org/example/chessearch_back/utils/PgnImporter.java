package org.example.chessearch_back.utils;

import org.example.chessearch_back.service.PgnParserService;

import java.io.BufferedReader;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Script for filling chess_search database
 */
public class PgnImporter {

    private final PgnParserService pgnParserService;
    private final Connection db;
    private static final DateTimeFormatter PGN_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private static final String pgnPath = "src/main/resources/unpacked_lichess_2013_01.pgn";

    public static void main(String[] args) {
        Connection conn = null;
        Properties dbProps = new Properties();

        try {
            Path propsPath = Paths.get("src/main/resources/application.properties");
            if (!Files.exists(propsPath)) {
                throw new IOException("application.properties not found at: " + propsPath.toAbsolutePath());
            }
            try (InputStream input = new FileInputStream(propsPath.toFile())) {
                dbProps.load(input);
            } catch (IOException ex) {
                System.err.println("Error loading application.properties:");
                ex.printStackTrace();
                return;
            }

            String dbUrl = dbProps.getProperty("spring.datasource.url");
            String dbUsername = dbProps.getProperty("spring.datasource.username");
            String dbPassword = dbProps.getProperty("spring.datasource.password");

            if (dbUrl == null || dbUsername == null || dbPassword == null) {
                throw new RuntimeException("Missing required database properties in application.properties");
            }


            System.out.println("Connecting to database: " + dbUrl);
            conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            System.out.println("Database connection established.");

            PgnParserService parser = new PgnParserService();
            PgnImporter importer = new PgnImporter(parser, conn);

            Path filePath = Path.of(pgnPath);
            importer.importFromFile(filePath);

            System.out.println("Import completed successfully.");

        } catch (Exception e) {
            System.err.println("An error occurred during import:");
            e.printStackTrace();
        } finally {

            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    System.out.println("Database connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection:");
                e.printStackTrace();
            }
        }
    }

    public PgnImporter(PgnParserService parser, Connection dbConnection) {
        this.pgnParserService = parser;
        this.db = dbConnection;
    }

    public void importFromFile(Path filePath) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            StringBuilder currentGamePgn = new StringBuilder();
            String line;
            int gameCount = 0;
            int maxGames = 100;
            long startTime = System.currentTimeMillis();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[Event ") && !currentGamePgn.isEmpty()) {
                    processSingleGame(currentGamePgn.toString());
                    gameCount++;
                    if (gameCount % 100 == 0) {
                        System.out.println("Processed " + gameCount + " games");
                    }
                    if (gameCount >= maxGames) {
                        System.out.println("Reached max game limit (" + maxGames + "). Stopping import.");
                        break;
                    }
                    currentGamePgn.setLength(0);
                }
                if (!line.trim().isEmpty() || !currentGamePgn.isEmpty()) {
                    currentGamePgn.append(line).append("\n");
                }
            }

            if (!currentGamePgn.isEmpty() && gameCount < maxGames) {
                processSingleGame(currentGamePgn.toString());
                gameCount++;
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Finished processing " + gameCount + " games in " + (endTime - startTime) + " ms.");
        }
    }

    private void processSingleGame(String pgn) throws Exception {
        Map<String, String> tags = extractTags(pgn);
        int pgnId = insertPgnIntoDb(tags, pgn);
        List<String> fens = pgnParserService.parsePgnToFens(pgn);
        insertFens(pgnId, fens);
    }

    private Map<String, String> extractTags(String pgn) {
        Map<String, String> tags = new HashMap<>();
        Pattern pattern = Pattern.compile("\\[\\s*(\\w+)\\s*\"([^\"]*)\"\\s*\\]");
        Matcher matcher = pattern.matcher(pgn);
        while (matcher.find()) {
            tags.put(matcher.group(1), matcher.group(2));
        }
        return tags;
    }
    private int insertPgnIntoDb(Map<String, String> tags, String pgnText) throws SQLException {
        String sql = """
                INSERT INTO chess_game 
                (pgn, white, black, result, event, site, date, whiteelo, blackelo, eco) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement stmt = db.prepareStatement(sql)) {
            stmt.setString(1, pgnText);
            stmt.setString(2, tags.get("White"));
            stmt.setString(3, tags.get("Black"));
            stmt.setString(4, tags.get("Result"));
            stmt.setString(5, tags.get("Event"));
            stmt.setString(6, tags.get("Site"));
            stmt.setObject(7, parseDate(tags.get("UTCDate")));
            Integer whiteElo = parseInt(tags.get("WhiteElo"));
            if (whiteElo != null) {
                stmt.setInt(8, whiteElo);
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }

            Integer blackElo = parseInt(tags.get("BlackElo"));
            if (blackElo != null) {
                stmt.setInt(9, blackElo);
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }

            stmt.setString(10, tags.get("ECO"));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Failed to retrieve ID after PGN insertion.");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error inserting game. SQLState: " + e.getSQLState() + ", ErrorCode: " + e.getErrorCode());
            System.err.println("Failed PGN Tags: " + tags);
            System.err.println("Failed PGN Text (start): " + pgnText.substring(0, Math.min(200, pgnText.length())) + "...");
            throw e;
        } catch (Exception e) {
            System.err.println("Non-SQL Error during PGN insertion preparation for tags: " + tags);
            e.printStackTrace();
            throw new SQLException("Error preparing PGN insertion statement", e);
        }
    }
    private void insertFens(int pgnId, List<String> fens) throws SQLException {
        String sql = "INSERT INTO fen_position (game_id, move_number, fen) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = db.prepareStatement(sql)) {
            int batchSize = 0;
            for (int i = 0; i < fens.size(); i++) {
                stmt.setInt(1, pgnId);
                stmt.setInt(2, i + 1);
                stmt.setString(3, fens.get(i));
                stmt.addBatch();
                batchSize++;

                if (batchSize % 1000 == 0 || i == fens.size() - 1) {
                    stmt.executeBatch();
                    batchSize = 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error inserting FENs for game ID: " + pgnId);
            throw e;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.contains("?") || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), PGN_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Could not parse date string: '" + dateStr + "' - " + e.getMessage());
            return null;
        }
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.contains("?") || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeStr.trim());
        } catch (DateTimeParseException e) {
            System.err.println("Could not parse time: '" + timeStr + "' - " + e.getMessage());
            return null;
        }
    }

    private Integer parseInt(String s) {
        if (s == null || s.trim().isEmpty() || s.equals("?")) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            System.err.println("Could not parse integer: '" + s + "' - " + e.getMessage());
            return null;
        }
    }
}
