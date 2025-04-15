package org.example.chessearch_back.repository;

import org.example.chessearch_back.model.FenPosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class FenPositionRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FenPositionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final class FenPositionRowMapper implements RowMapper<FenPosition> {
        @Override
        public FenPosition mapRow(ResultSet rs, int rowNum) throws SQLException {
            FenPosition pos = new FenPosition();
            pos.setId(rs.getInt("id"));
            pos.setGameId(rs.getInt("game_id"));
            pos.setMoveNumber(rs.getInt("move_number"));
            pos.setFen(rs.getString("fen"));
            return pos;
        }
    }

    /**
     * Retrieves a list of FEN positions for a given game ID, ordered by move number.
     * Uses JdbcTemplate .
     * @param gameId The ID of the game whose positions are to be fetched.
     * @return A List of FenPosition objects, or an empty list if none are found.
     */
    public List<FenPosition> getFensByGameId(int gameId) {
        String sql = "SELECT * FROM fen_position WHERE game_id = ? ORDER BY move_number";
        return jdbcTemplate.query(sql, new FenPositionRowMapper(), gameId);

    }
}
