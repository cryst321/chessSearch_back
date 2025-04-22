package org.example.chessearch_back.repository;


import org.example.chessearch_back.model.Admin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class AdminRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final class AdminRowMapper implements RowMapper<Admin> {
        @Override
        public Admin mapRow(ResultSet rs, int rowNum) throws SQLException {
            Admin admin = new Admin();
            admin.setId(rs.getInt("id"));
            admin.setUsername(rs.getString("username"));
            admin.setPassword(rs.getString("password"));
            admin.setRole(rs.getString("role"));
            return admin;
        }
    }

    /**
     * Finds admin by their username
     * @param username The username to search for
     * @return Optional<Admin> contains the admin or Optional.empty()
     */
    public Optional<Admin> findByUsername(String username) {
        String sql = "SELECT id, username, password, role FROM public.admins WHERE username = ?";
        try {
            List<Admin> admins = jdbcTemplate.query(sql, new AdminRowMapper(), username);
            return DataAccessUtils.optionalResult(admins);
        } catch (Exception e) {
            System.err.println("Error fetching admin by username '" + username + "': " + e.getMessage());
            return Optional.empty();
        }}
}