package org.example.chessearch_back.service;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.pgn.*;
import org.springframework.stereotype.Service;

import java.io.StringReader;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;


@Service
public class PgnParserService {

    public List<String> parsePgnToFens(String pgn) throws Exception {


        File tempFile = File.createTempFile("temp", ".pgn");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(pgn);
        }

        PgnHolder holder = new PgnHolder(tempFile.getAbsolutePath());
        holder.loadPgn();

        List<String> fens = new ArrayList<>();

        for (Game game : holder.getGames()) {
            game.loadMoveText();
            Board board = new Board();
            fens.add(board.getFen());

            for (Move move : game.getHalfMoves()) {
                board.doMove(move);
                fens.add(board.getFen());
            }
        }
        tempFile.delete();

        return fens;

//        List<String> fens = new ArrayList<>();
//        PGNLoader loader = new PGNLoader(new StringReader(pgn));
//        List<Game> games = loader.loadGames();
//
//        for (Game game : games) {
//            Board board = new Board();
//            fens.add(board.getFen());
//            for (Move move : game.getHalfMoves()) {
//                board.doMove(move);
//                fens.add(board.getFen());
//            }
//        }
//        return fens;
//    }
    }
}