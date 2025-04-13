package org.example.chessearch_back.repository;


import org.example.chessearch_back.model.FenPosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FenPositionRepository {

    private final DataSource dataSource;

    @Autowired
    public FenPositionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<FenPosition> getFensByGameId(int gameId) {
        String sql = "SELECT * FROM fen_position WHERE game_id = ? ORDER BY move_number ASC";
        List<FenPosition> positions = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, gameId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FenPosition pos = new FenPosition();
                    pos.setId(rs.getInt("id"));
                    pos.setGameId(rs.getInt("game_id"));
                    pos.setMoveNumber(rs.getInt("move_number"));
                    pos.setFen(rs.getString("fen"));
                    positions.add(pos);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return positions;
    }
}
