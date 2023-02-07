package hr.reversi.concurrency;

import java.util.ArrayList;

public class PlayerMove {
    private static Integer[] move;
    private static ArrayList<Integer[]> allPlayedMoves;

    public PlayerMove() {
        move = new Integer[2];
        allPlayedMoves = new ArrayList<Integer[]>();
    }

    public synchronized void printAllPlayedMoves() {
        Integer moveNumber = 1;
        System.out.print("[");
        for (Integer[] move : allPlayedMoves) {
            System.out.println("#" + moveNumber + " [ Row " + move[0] + " | Col " + move[1] + " ]");
            moveNumber++;
        }
        System.out.println("]");
    }

    public synchronized void addMove(Integer[] move) {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.move = move;

        allPlayedMoves.add(this.move);

        System.out.printf("Zapisano %d na element 0.%n", move[0]);
        System.out.printf("Zapisano %d na element 1.%n", move[1]);

    }
}
