package com.example.connectfour.gameboard;

import android.graphics.Color;

public class Piece {
    private int y;
    private int x;
    private int color;


    public Piece(int y, int x, int color) {
        this.y = y;
        this.x = x;
        this.color = color;
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getColor() {
        return color;
    }
}
