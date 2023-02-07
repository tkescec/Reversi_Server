package hr.reversi.concurrency;

import java.util.ArrayList;

public class PlayerMoveWriter implements Runnable{
    private PlayerMove playerMove;
    private Integer[] move;

    public PlayerMoveWriter(PlayerMove playerMove, Integer[] move) {
        this.playerMove = playerMove;
        this.move = move;
    }

    @Override
    public void run() {
        playerMove.addMove(this.move);
    }
}
