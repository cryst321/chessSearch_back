package org.example.chessearch_back.repository;

import org.example.chessearch_back.dto.ChessGameDto;
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

    public ChessGameDto findById(int id) {
        String sql = "SELECT * FROM chess_game WHERE id = ?";

        return jdbcTemplate.queryForObject(sql,
                new Object[]{id},
                (rs, rowNum) -> {
                    ChessGameDto game = new ChessGameDto();
                    game.setWhite(rs.getString("white"));
                    game.setBlack(rs.getString("black"));
                    game.setResult(rs.getString("result"));
                    game.setEvent(rs.getString("event"));
                    game.setSite(rs.getString("site"));
                    game.setDate(rs.getDate("date") != null ?
                            rs.getDate("date").toLocalDate() : null);
                    game.setPgn(rs.getString("pgn"));
                    return game;
                });
    }
}