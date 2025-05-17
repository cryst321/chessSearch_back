package org.example.chessearch_back.repository;

import org.example.chessearch_back.dto.ChessGameDto;
import org.example.chessearch_back.dto.GamePreviewDto;
import org.example.chessearch_back.model.ChessGame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ChessGameRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ChessGameRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final class ChessGameDtoRowMapper implements RowMapper<ChessGameDto> {
        @Override
        public ChessGameDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChessGameDto game = new ChessGameDto();
            game.setWhite(rs.getString("white"));
            game.setBlack(rs.getString("black"));
            game.setResult(rs.getString("result"));
            game.setEvent(rs.getString("event"));
            game.setSite(rs.getString("site"));

            java.sql.Date sqlDate = rs.getDate("date");
            if (sqlDate != null) {
                game.setDate(sqlDate.toLocalDate());
            } else {
                game.setDate(null);
            }

            game.setPgn(rs.getString("pgn"));

            int whiteEloInt = rs.getInt("whiteelo");
            if (!rs.wasNull()) {
                game.setWhiteElo(whiteEloInt);
            } else {
                game.setWhiteElo(null);
            }

            int blackEloInt = rs.getInt("blackelo");
            if (!rs.wasNull()) {
                game.setBlackElo(blackEloInt);
            } else {
                game.setBlackElo(null);
            }

            game.setEco(rs.getString("eco"));
            return game;
        }
    }

    private static final class GamePreviewDtoRowMapper implements RowMapper<GamePreviewDto> {
        @Override
        public GamePreviewDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            GamePreviewDto preview = new GamePreviewDto();
            preview.setGameId(rs.getInt("game_id"));
            preview.setWhite(rs.getString("white"));
            preview.setBlack(rs.getString("black"));
            preview.setResult(rs.getString("result"));
            java.sql.Date sqlDate = rs.getDate("date");
            if (sqlDate != null) {
                preview.setDate(sqlDate.toLocalDate());
            } else {
                preview.setDate(null);
            }
            preview.setLastFen(rs.getString("last_fen"));
            return preview;
        }
    }

    /**
     * Find a ChessGameDto by its ID using jdbcTemplate.query()
     * @param id The ID of the chess game to find.
     * @return The corresponding ChessGameDto.
     * @throws EmptyResultDataAccessException if no game with the given ID is found.
     * @throws IncorrectResultSizeDataAccessException if more than one game is found.
     */
    public ChessGameDto findById(int id) {
        String sql = "SELECT * FROM chess_game WHERE id = ?";

        List<ChessGameDto> results = jdbcTemplate.query(
                sql,
                new ChessGameDtoRowMapper(),
                id
        );

        return DataAccessUtils.requiredSingleResult(results);
    }

    public List<GamePreviewDto> findGamePreviews(int limit, int offset,String eco, LocalDate dateFrom, LocalDate dateTo, String result,
                                                 Integer minElo, Integer maxElo, String playerName) {

        List<Object> queryParams = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("""
            WITH RankedFen AS (
                SELECT
                    fp.game_id,
                    fp.fen,
                    fp.move_number,
                    ROW_NUMBER() OVER(PARTITION BY fp.game_id ORDER BY fp.move_number DESC) as rn
                FROM fen_position fp
            ), LastFen AS (
                SELECT game_id, fen
                FROM RankedFen
                WHERE rn = 1
            )
            
            SELECT
                cg.id AS game_id,
                cg.white,
                cg.black,
                cg.result,
                cg.date,
                lf.fen AS last_fen
            FROM chess_game cg
            LEFT JOIN LastFen lf ON cg.id = lf.game_id
            """);
        buildWhereClauses(eco, dateFrom, dateTo, result, minElo, maxElo, playerName, queryParams, sqlBuilder);
        sqlBuilder.append(" ORDER BY cg.date DESC, cg.id DESC ");
        sqlBuilder.append(" LIMIT ? OFFSET ? ");

        queryParams.add(limit);
        queryParams.add(offset);
        return jdbcTemplate.query(sqlBuilder.toString(), new GamePreviewDtoRowMapper(), queryParams.toArray());
    }

    public List<Integer> findAllGameIds() {
        String sql = "SELECT id FROM chess_game ORDER BY id";

        return jdbcTemplate.queryForList(sql, Integer.class);
    }

    public long countTotalGames(String eco, LocalDate dateFrom, LocalDate dateTo, String result,
                                Integer minElo, Integer maxElo, String playerName) {

        List<Object> queryParams = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(cg.id) FROM chess_game cg ");

        buildWhereClauses(eco, dateFrom, dateTo, result, minElo, maxElo, playerName, queryParams, sqlBuilder);

        Long count = jdbcTemplate.queryForObject(sqlBuilder.toString(), Long.class, queryParams.toArray());
        return count;
    }

    /**
     * helper method for
     * @param eco opening
     * @param dateFrom -
     * @param dateTo -
     * @param result who won
     * @param minElo -
     * @param maxElo -
     * @param playerName name if searched
     * @param queryParams received params
     * @param sqlBuilder builder for query
     */
    private void buildWhereClauses(String eco, LocalDate dateFrom, LocalDate dateTo, String result, Integer minElo, Integer maxElo, String playerName, List<Object> queryParams, StringBuilder sqlBuilder) {
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");

        if (StringUtils.hasText(eco)) {
            whereClause.append(" AND LOWER(cg.eco) LIKE LOWER(?) ");
            queryParams.add("%" + eco + "%");
        }
        if (dateFrom != null) {
            whereClause.append(" AND cg.date >= ? ");
            queryParams.add(Date.valueOf(dateFrom));
        }
        if (dateTo != null) {
            whereClause.append(" AND cg.date <= ? ");
            queryParams.add(Date.valueOf(dateTo));
        }
        if (StringUtils.hasText(result)) {
            whereClause.append(" AND cg.result = ? ");
            queryParams.add(result);
        }
        if (minElo != null) {
            whereClause.append(" AND (cg.whiteelo >= ? OR cg.blackelo >= ?) ");
            queryParams.add(minElo);
            queryParams.add(minElo);
        }
        if (maxElo != null) {
            whereClause.append(" AND (cg.whiteelo <= ? OR cg.blackelo <= ?) ");
            queryParams.add(maxElo);
            queryParams.add(maxElo);
        }
        if (StringUtils.hasText(playerName)) {
            whereClause.append(" AND (LOWER(cg.white) LIKE LOWER(?) OR LOWER(cg.black) LIKE LOWER(?)) ");
            queryParams.add("%" + playerName + "%");
            queryParams.add("%" + playerName + "%");
        }

        sqlBuilder.append(whereClause);
    }

    public Integer saveAndReturnId(ChessGame game) {
        final String sql = "INSERT INTO chess_game (pgn, white, black, result, event, site, date, whiteelo, blackelo, eco) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"});
            ps.setString(1, game.getPgn());
            ps.setString(2, game.getWhite());
            ps.setString(3, game.getBlack());
            ps.setString(4, game.getResult());
            ps.setString(5, game.getEvent());
            ps.setString(6, game.getSite());
            if (game.getDate() != null) {
                ps.setDate(7, java.sql.Date.valueOf(game.getDate()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            if (game.getWhiteElo() != null) {
                ps.setInt(8, game.getWhiteElo());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            if (game.getBlackElo() != null) {
                ps.setInt(9, game.getBlackElo());
            } else {
                ps.setNull(9, Types.INTEGER);
            }
            ps.setString(10, game.getEco());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeyList().size() > 1) {
            System.err.println("Warning: KeyHolder returned multiple keys: " + keyHolder.getKeyList());
            for (java.util.Map<String, Object> keyMap : keyHolder.getKeyList()) {
                if (keyMap.containsKey("id")) {
                    return ((Number) keyMap.get("id")).intValue();
                }
            }
            throw new RuntimeException("Failed to retrieve 'id' from multiple generated keys.");
        }

        Number key = keyHolder.getKey();
        return key.intValue();
    }
}