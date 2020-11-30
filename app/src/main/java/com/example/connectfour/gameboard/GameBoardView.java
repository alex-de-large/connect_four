package com.example.connectfour.gameboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.connectfour.R;

import java.util.Arrays;

public class GameBoardView extends View {
    private int height;
    private int width;
    private int circleRadius;
    private Paint paint;
    private int cellSizeX;
    private int cellSizeY;
    private boolean isInit = false;
    private Piece[][] mPieces;
    private boolean isPlayerWon;
    private int movesNum;
    private Pair pair1;
    private Pair pair2;

    private final static int COLUMNS_NUM = 7;
    private final static int ROWS_NUM = 7;
    private final static int CELL_PADDING = 5;
    private final static int WIN_COMB_LENGTH = 4;
    private final static int VICTORY_LINE_WIDTH = 20;

    public GameBoardView(Context context) {
        super(context);
    }

    public GameBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GameBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isInit) {
            init();
        }

        drawBoard(canvas);

        postInvalidateDelayed(500);
        invalidate();
    }

    public void init() {
        height = getHeight();
        width = getWidth();
        cellSizeX = width / COLUMNS_NUM;
        cellSizeY = height / ROWS_NUM;
        circleRadius = Math.min(cellSizeX, cellSizeY) / 2 - CELL_PADDING * 2;
        isPlayerWon = false;
        movesNum = 0;

        paint = new Paint();
        mPieces = new Piece[ROWS_NUM][COLUMNS_NUM];

        isInit = true;

    }

    public void clearBoard() {
        mPieces = new Piece[ROWS_NUM][COLUMNS_NUM];
    }

    public int getMovesNum() {
        return movesNum;
    }

    private void drawBoard(Canvas canvas) {
        paint.reset();
        paint.setColor(getResources().getColor(R.color.boardColor2));
        paint.setAntiAlias(true);
        canvas.drawRect(0, 0, width, height, paint);

        int offsetX = cellSizeX / 2;
        int offsetY = cellSizeY / 2;
        for (int i = 0; i < ROWS_NUM; i++) {
            int y = cellSizeY * i + + offsetY;
            for (int j = 0; j < COLUMNS_NUM; j++) {
                int x = cellSizeX * j + offsetX;

                if (mPieces[i][j] != null) {
                    paint.setColor(mPieces[i][j].getColor());
                    mPieces[i][j].setX(x);
                    mPieces[i][j].setY(y);
                } else {
                    paint.setColor(getResources().getColor(R.color.white));
                }
                canvas.drawCircle(x, y, circleRadius, paint);
            }
        }

        if (isPlayerWon) {
            int startX = mPieces[pair1.getI()][pair1.getJ()].getX();
            int startY = mPieces[pair1.getI()][pair1.getJ()].getY();
            int endX = mPieces[pair2.getI()][pair2.getJ()].getX();
            int endY = mPieces[pair2.getI()][pair2.getJ()].getY();
            paint.setColor(getResources().getColor(R.color.lineColor));
            paint.setStrokeWidth(VICTORY_LINE_WIDTH);
            canvas.drawLine(startX, startY, endX, endY, paint);
        }
    }

    public boolean makeMove(int row, int column, int color) {
        if (row == -1) {
            return false;
        }

        mPieces[row][column] = new Piece(row, column, color);
        movesNum++;
        return true;
    }

    public int getColumn(int x) {
        return x / (cellSizeX);
    }

    public int getRow(int column) {
        int i = ROWS_NUM - 1;
        while (mPieces[i][column] != null) {
            i--;
            if (i == -1)
                break;
        }
        return i;
    }

    public GameState checkGameState(int playerColor) {
        for (int i = 0; i < ROWS_NUM; i++) {
            for (int j = 0; j < COLUMNS_NUM; j++) {
                if (mPieces[i][j] != null && mPieces[i][j].getColor() == playerColor) {

                    int check1 = 1;
                    int check2 = 1;
                    int check3 = 1;
                    int check4 = 1;
                    int i1 = 0;
                    int j1 = 0;
                    for (int k = 1; k < WIN_COMB_LENGTH; k++) {
                        if (checkBounds(i + k, j) && mPieces[i + k][j].getColor() ==  playerColor) {
                            check1++;
                            i1 = i + k;
                            j1 = j;
                        }
                        if (checkBounds(i, j + k) && mPieces[i][j + k].getColor() ==  playerColor) {
                            check2++;
                            i1 = i;
                            j1 = j + k;
                        }
                        if (checkBounds(i - k, j + k) && mPieces[i - k][j + k].getColor() ==  playerColor) {
                            check3++;
                            i1 = i - k;
                            j1 = j + k;
                        }
                        if (checkBounds(i - k, j - k) && mPieces[i - k][j - k].getColor() ==  playerColor) {
                            check4++;
                            i1 = i - k;
                            j1 = j - k;
                        }
                    }

                    if (check1 == WIN_COMB_LENGTH ||
                            check2 == WIN_COMB_LENGTH ||
                            check3 == WIN_COMB_LENGTH ||
                            check4 == WIN_COMB_LENGTH) {
                        isPlayerWon = true;
                        pair1 = new Pair(i, j);
                        pair2 = new Pair(i1, j1);
                        return GameState.PLAYER_WIN;
                    }
                }
            }
        }

        if (movesNum == (ROWS_NUM * COLUMNS_NUM)) {
            Toast.makeText(getContext(), "TIE", Toast.LENGTH_SHORT).show();
            setEnabled(false);
            return GameState.TIE;
        } else {
            return GameState.GAME_CONTINUES;
        }
    }

    private boolean checkBounds(int i, int j) {
        if (i < ROWS_NUM && i >= 0 && j < COLUMNS_NUM && j >= 0)
            return mPieces[i][j] != null;
        return false;
    }

    private static class Pair {
        private int i;
        private int j;

        Pair(int i, int j) {
            this.i = i;
            this.j = j;
        }

        public int getI() {
            return i;
        }

        public int getJ() {
            return j;
        }
    }

}
