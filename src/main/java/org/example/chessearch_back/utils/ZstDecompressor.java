package org.example.chessearch_back.utils;

import com.github.luben.zstd.ZstdInputStream;
import java.io.*;

/**
 * Decompresses large PGN files that contain chess games (.zst)
 */
public class ZstDecompressor {

    public static void main(String[] args) throws IOException {
            String inputZst ="src/main/resources/lichess_db_standard_rated_2013-01.pgn.zst";
            String outputPgn = "src/main/resources/unpacked_lichess_2013_01.pgn";

            ZstDecompressor.decompressZstFile(inputZst, outputPgn);

            System.out.println("Розпаковано успішно: " + outputPgn);

        }
    public static void decompressZstFile(String inputZstPath, String outputPgnPath) throws IOException {
        try (InputStream fis = new FileInputStream(inputZstPath);
             ZstdInputStream zis = new ZstdInputStream(fis);
             OutputStream fos = new FileOutputStream(outputPgnPath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
}