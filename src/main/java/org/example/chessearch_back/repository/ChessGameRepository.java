package org.example.chessearch_back.repository;

import org.example.chessearch_back.dto.ChessGameDto;
import org.example.chessearch_back.dto.GamePreviewDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
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

    public List<GamePreviewDto> findGamePreviews(int limit, int offset) {
        String sql = """
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
            ORDER BY cg.date DESC, cg.id DESC

            LIMIT ? OFFSET ?;
            """;

        return jdbcTemplate.query(sql, new GamePreviewDtoRowMapper(), limit, offset);
    }

    public List<Integer> findAllGameIds() {
        String sql = "SELECT id FROM chess_game ORDER BY id";

        return jdbcTemplate.queryForList(sql, Integer.class);
    }

    public long countTotalGames() {
        String sql = "SELECT COUNT(*) FROM chess_game";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return (count != null) ? count : 0L;
    }
}