package com.battlesnake.data;

public class Path {

    public Move[] moves;

    public double getTotalScore() {
        double totalScore = 0;
        for (Move move : moves) {
            totalScore = totalScore + move.getScore();
        }

        return totalScore;
    }
}
