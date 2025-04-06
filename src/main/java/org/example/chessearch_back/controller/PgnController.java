package org.example.chessearch_back.controller;

import org.example.chessearch_back.service.PgnParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pgn")
public class PgnController {

    @Autowired
    private PgnParserService parserService;

    @PostMapping("/fen")
    public ResponseEntity<List<String>> getFensFromPgn(@RequestBody String pgn) {
        try {
            List<String> fens = parserService.parsePgnToFens(pgn);
            return ResponseEntity.ok(fens);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
