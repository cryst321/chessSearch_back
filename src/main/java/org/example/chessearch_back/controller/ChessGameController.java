package org.example.chessearch_back.controller;

import org.example.chessearch_back.dto.ChessGameDto;
import org.example.chessearch_back.service.ChessGameService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
public class ChessGameController {

    private final ChessGameService chessGameService;

    public ChessGameController(ChessGameService chessGameService) {
        this.chessGameService = chessGameService;
    }

    @GetMapping("/{id}")
    public ChessGameDto getGameById(@PathVariable int id) {
        return chessGameService.getGameDtoById(id);
    }
}
