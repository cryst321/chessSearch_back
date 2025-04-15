package org.example.chessearch_back.utils;

import org.example.chessearch_back.service.PgnParserService;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Script for filling chess_search database
 */
public class PgnImporter {

    private final PgnParserService pgnParserService;
    private final Connection db;
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/chess_search",
                    "postgres",
                    "sadcat"
            );

            PgnParserService parser = new PgnParserService();

            PgnImporter importer = new PgnImporter(parser, conn);

            Path filePath = Path.of("src/main/resources/unpacked_lichess_2013_01.pgn");

            importer.importFromFile(filePath);

            System.out.println("✅ Імпорт завершено успішно");

        } catch (Exception e) {
            System.err.println("❌ Щось пішло не так:");
            e.printStackTrace();
        }
    }

    public PgnImporter(PgnParserService parser, Connection dbConnection) {
        this.pgnParserService = parser;
        this.db = dbConnection;
    }

    public void importFromFile(Path filePath) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            StringBuilder currentGame = new StringBuilder();
            String line;
            int gameCount = 0;
            int maxGames = 100;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[Event ")) {
                    if (!currentGame.isEmpty()) {
                        processSingleGame(currentGame.toString());
                        gameCount++;
                        if (gameCount >= maxGames) break;
                        currentGame.setLength(0);
                    }
                }
                currentGame.append(line).append("\n");
            }

            if (!currentGame.isEmpty() && gameCount < maxGames) {
                processSingleGame(currentGame.toString());
            }

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
        Pattern pattern = Pattern.compile("\\[(\\w+) \"([^\"]*)\"]");
        Matcher matcher = pattern.matcher(pgn);
        while (matcher.find()) {
            tags.put(matcher.group(1), matcher.group(2));
        }
        return tags;
    }

    private int insertPgnIntoDb(Map<String, String> tags, String pgnText) throws SQLException {
        String sql = """
                INSERT INTO chess_game (pgn, white, black, result,event, site,date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement stmt = db.prepareStatement(sql)) {
            stmt.setString(1, pgnText);
            stmt.setString(2, tags.get("White"));
            stmt.setString(3, tags.get("Black"));
            stmt.setString(4, tags.get("Result"));
            stmt.setString(5, tags.get("Event"));
            stmt.setString(6, tags.get("Site"));
            stmt.setObject(7, parseDate(tags.get("Date")));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
            throw new SQLException("Failed to insert PGN");
        }
    }

    private void insertFens(int pgnId, List<String> fens) throws SQLException {
        String sql = "INSERT INTO fen_position (game_id, move_number, fen) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = db.prepareStatement(sql)) {
            for (int i = 0; i < fens.size(); i++) {
                stmt.setInt(1, pgnId);
                stmt.setInt(2, i + 1);
                stmt.setString(3, fens.get(i));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.contains("??")) return null;
        return LocalDate.parse(dateStr);
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.contains("??")) return null;
        return LocalTime.parse(timeStr);
    }

    private Integer parseInt(String s) {
        if (s == null || s.isEmpty()) return null;
        return Integer.parseInt(s);
    }
}

