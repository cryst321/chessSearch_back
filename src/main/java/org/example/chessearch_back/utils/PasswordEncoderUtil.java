package org.example.chessearch_back.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderUtil {
    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "super_secure_chess_password_208";
        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("Username: admin_acc");
        System.out.println("Raw Password: " + rawPassword);
        System.out.println("BCrypt Encoded Password: " + encodedPassword);
    }
}