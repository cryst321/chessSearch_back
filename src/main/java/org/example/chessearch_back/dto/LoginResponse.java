package org.example.chessearch_back.dto;


/**
 * DTO representing a login response to frontend
 */
public class LoginResponse {
    private String username;
    private String role;
    public LoginResponse(String username, String role) {
        this.username = username;
        this.role = role;
    }
    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}