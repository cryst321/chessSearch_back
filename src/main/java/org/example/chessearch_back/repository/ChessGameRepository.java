package org.example.chessearch_back.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.example.chessearch_back.model.ChessGame;
@Repository
public class ChessGameRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ChessGameRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ChessGame save(ChessGame game) {
        final String sql = "INSERT INTO chess_games (pgn) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, game.getPgn());
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        game.setId(id);
        return game;
    }

    public ChessGame findById(Long id) {
        final String sql = "SELECT id, pgn FROM chess_games WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new ChessGameRowMapper(), id);
    }

    public List<ChessGame> findAll() {
        final String sql = "SELECT id, pgn FROM chess_games";
        return jdbcTemplate.query(sql, new ChessGameRowMapper());
    }

    private static class ChessGameRowMapper implements RowMapper<ChessGame> {
        @Override
        public ChessGame mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChessGame game = new ChessGame();
            game.setId(rs.getLong("id"));
            game.setPgn(rs.getString("pgn"));
            return game;
        }
    }
}