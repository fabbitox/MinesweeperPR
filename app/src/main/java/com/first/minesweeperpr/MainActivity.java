package com.first.minesweeperpr;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private Game game;
    private TableLayout board;
    private int boardWidth;
    private int boardHeight;
    private int[] adjCells;
    private Queue<Integer> toBeOpen;
    private int remainedCount;
    private int explodedCount;
    private TextView remainedTv;
    private TextView explodedTv;
    private boolean overFlag;
    private int foundIndex;
    private boolean finishFlag;
    private boolean flagChecked;
    private boolean firstFlag;
    private Timer timer;
    private int timerCount;
    private TextView timerView;
    private boolean quickMode;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // hide navigation bar
        View decorView = getWindow().getDecorView();
        int systemUiVis = decorView.getSystemUiVisibility();
        systemUiVis |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        systemUiVis |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        systemUiVis |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(systemUiVis);

        // findViewById
        board = findViewById(R.id.board);
        View valueInput = findViewById(R.id.value_input);
        TextView colText = findViewById(R.id.column_text);
        TextView rowText = findViewById(R.id.row_text);
        TextView mineText = findViewById(R.id.mine_text);
        SeekBar colBar = findViewById(R.id.column_bar);
        SeekBar rowBar = findViewById(R.id.row_bar);
        SeekBar mineBar = findViewById(R.id.mine_bar);
        Button startBtn = findViewById(R.id.start_btn);
        Button restartBtn = findViewById(R.id.restart_btn);
        View root = findViewById(R.id.root);
        remainedTv = findViewById(R.id.remained_count);
        explodedTv = findViewById(R.id.exploded_count);
        View gameUi = findViewById(R.id.for_game);
        ImageButton flagBtn = findViewById(R.id.flag_btn);
        timerView = findViewById(R.id.time);

        // initialize variables
        overFlag = false;
        foundIndex = 0;
        flagChecked = false;
        finishFlag = false;
        firstFlag = true;
        root.setBackgroundColor(0xcceeddff);// ?????? ???
        game = Game.getInstance();
        toBeOpen = new LinkedList<>();
        final int[] counts = {5, 8, 4};
        timerCount = 0;
        quickMode = false;

        // ????????? ??????
        findViewById(R.id.easy).setOnClickListener(v -> {
            colBar.setProgress(4);// 9
            rowBar.setProgress(1);// 9
            mineBar.setProgress(6);// 10
        });
        findViewById(R.id.normal).setOnClickListener(v -> {
            colBar.setProgress(11);// 16
            rowBar.setProgress(8);// 16
            mineBar.setProgress(28);// 40
        });
        findViewById(R.id.hard).setOnClickListener(v -> {
            colBar.setProgress(11);// 16
            rowBar.setProgress(22);// 30
            mineBar.setProgress(75);// 99
        });

        // SeekBar ?????? TextView, ?????? ????????????
        colBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                colText.setText(String.format("??????: %s", (progress + 5)));
                counts[0] = progress + 5;
                mineBar.setMax(counts[0] * counts[1] / 5 * 2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        rowBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rowText.setText(String.format("??????: %s", progress + 8));
                counts[1] = progress + 8;
                mineBar.setMax(counts[0] * counts[1] / 5 * 2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mineBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                counts[2] = progress + counts[0] * counts[1] / 20;
                double mineRate = (double)counts[2] / counts[0] / counts[1] * 100;
                mineText.setText(String.format(getString(R.string.mine_text_format), counts[2], mineRate));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        startBtn.setOnClickListener(v -> {
            getBoardSize();
            // ??? ??????
            fillBoard(counts[0], counts[1], counts[2]);
            remainedCount = counts[2];
            explodedCount = 0;
            valueInput.setVisibility(View.INVISIBLE);
            gameUi.setVisibility(View.VISIBLE);
            remainedTv.setText(String.valueOf(remainedCount));
            explodedTv.setText(String.valueOf(explodedCount));
            timerView.setText(String.valueOf(timerCount));
        });
        // ??????????????? ?????? ????????? ??? ?????????
        restartBtn.setOnClickListener(v -> {
            valueInput.setVisibility(View.VISIBLE);
            gameUi.setVisibility(View.INVISIBLE);
            overFlag = false;
            foundIndex = 0;
            flagChecked = false;
            flagBtn.setImageResource(R.drawable.mine);
            finishFlag = false;
            firstFlag = true;
            timerCount = 0;
            timerView.setText(String.valueOf(timerCount));
            if (timer != null) timer.cancel();
            board.removeAllViews();
        });

        flagBtn.setOnClickListener(v -> {
            ImageButton ib = (ImageButton)v;
            if (flagChecked) {
                flagChecked = false;
                ib.setImageResource(R.drawable.mine);
            }
            else {
                flagChecked = true;
                ib.setImageResource(R.drawable.flag);
            }
        });
    }

    private void getBoardSize() {
        if (boardWidth == 0) {
            boardWidth = board.getWidth();
        }
        if (boardHeight == 0) {
            boardHeight = board.getHeight();
        }
    }

    private void fillBoard(int columnCount, int rowCount, int mineCount) {
        int width = boardWidth / columnCount;
        int height = boardHeight / rowCount;
        int buttonSize = Math.min(width, height);
        int i, j;

        game.positionMine(columnCount, rowCount, mineCount);// ?????? ?????? ??????
        for (i = 0; i < rowCount; i++) {
            TableRow row = new TableRow(this);
            board.addView(row);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(buttonSize, buttonSize);
            for (j = 0; j < columnCount; j++) {
                int index = i * columnCount + j;
                ImageButton ib = new ImageButton(this);
                ib.setLayoutParams(lp);
                ib.setBackgroundColor(0x99faf5ff);// ??? ??? ??? ???
                ib.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ib.setImageResource(R.drawable.blank);
                ib.setOnClickListener(v -> {
                    ImageButton currIb = (ImageButton)v;
                    if (flagChecked) {// flag mode
                        openF(currIb, index);
                    }
                    else if (!getFlagState(currIb)) {// ?????? ?????? ??? ?????? ???
                        open(currIb, index);
                    }
                });
                ib.setOnLongClickListener(v -> {
                    if (game.isOpened(index)) {// ?????? ?????????
                        openAdjWithFlag(index);// ?????? ?????? ?????? ?????? ?????? ??? ?????? ????????? ?????? ?????? ?????? ???
                    }
                    else {// ??? ?????? ????????? ????????? ??????????????? ??????
                        toggleFlag((ImageButton)v);
                    }
                    return true;
                });
                row.addView(ib);
            }
        }
        // ????????? ????????? ?????????
        int spaceWidth = boardWidth - buttonSize * columnCount;
        int spaceHeight = boardHeight - buttonSize * rowCount;
        board.setX(board.getLeft() + (spaceWidth >> 1));
        board.setY(board.getTop() + (spaceHeight >> 1));
    }

    private void open(ImageButton ib, int index) {// ??? ??????
        ib.setBackgroundColor(0xddeeddff);// ??? ??? ???
        game.setImage(ib, index);
        game.setOpened(index);
        foundIndex = game.foundTo(foundIndex);
        if (foundIndex == -1 && !finishFlag) {// game is finished
            finishFlag = true;
            timer.cancel();
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
            alertBuilder.setTitle("finish");
            String message;
            if (overFlag) {
                message = explodedCount + "?????? ?????????";
            }
            else {
                message = "?????? ??????";
            }
            alertBuilder.setMessage(message + " ????????? ?????? ?????? " + timerCount + " ??? ?????? ?????? ??????????????????!");
            alertBuilder.show();
        }
        if (firstFlag) {// start timer
            timer = new Timer();
            firstFlag = false;
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    timerCount++;
                    timerView.setText(String.valueOf(timerCount));
                }
            };
            timer.schedule(timerTask, 0, 1000);
        }
        if (game.isMine(index)) {
            overFlag = true;
            remainedCount--;
            remainedTv.setText(String.valueOf(remainedCount));
            explodedCount++;
            explodedTv.setText(String.valueOf(explodedCount));
            ib.setEnabled(false);
        }
        else if (game.countAround(index) == 0) {
            openAdjCells(index);
        }
    }

    private void openF(ImageButton ib, int index) {
        if (game.isOpened(index)) {
            quickMode = true;
            openAdjWithFlag(index);
            quickMode = false;
        }
        else if (!quickMode) {
            toggleFlag(ib);
        }
    }

    private void openAdjCells(int index) {// ?????? ??? ???????????? ???????????? ??????
        adjCells = game.getAdjacentCells(index);
        for (int i = 0; i < 8; i++) {
            int around = adjCells[i];
            if (game.isValidIndex(around, index)) {
                if (!game.isOpened(around)) {
                    toBeOpen.add(around);// ?????? ????????? ??? ?????? ??????
                    game.setOpened(around);
                }
            }
        }
        openQueue();
    }

    private void openQueue() {
        while (!toBeOpen.isEmpty()) {// ??? ??????????????? ?????? ??? ?????? ????????? NullPointer ??? ?????? ??????
            @SuppressWarnings("ConstantConditions") int index = toBeOpen.poll();
            open(getIbByIndex(index), index);// ?????????
        }
    }

    private void toggleFlag(ImageButton ib) {
        boolean flagState = getFlagState(ib);
        if (flagState) {// ?????? ?????? ?????? -> ?????????
            ib.setImageResource(R.drawable.blank);
            ib.setTag(R.string.flag, false);
            remainedCount++;
        }
        else {// ?????? ?????? ?????? -> ??????
            ib.setImageResource(R.drawable.flag);
            ib.setTag(R.string.flag, true);
            remainedCount--;
        }
        remainedTv.setText(String.valueOf(remainedCount));// ?????? ??? ????????????
    }

    private boolean getFlagState(ImageButton ib) {
        boolean flagState;
        Object tag = ib.getTag(R.string.flag);
        if (tag != null) {// ?????? ????????????
            flagState = (boolean)tag;
        }
        else {
            flagState = false;
        }
        return flagState;
    }

    private void openAdjWithFlag(int index) {
        adjCells = game.getAdjacentCells(index);
        int flagCount = 0;
        int mineCount = game.countAround(index);
        for (int i = 0; i < 8; i++) {// ?????? ?????? ??? ??????
            int around = adjCells[i];
            if (game.isValidIndex(around, index)) {// ?????? ??????
                if (getFlagState(getIbByIndex(around))) {
                    flagCount++;
                }
                else if (game.isMine(around) && game.isOpened(around)) {// ????????? ??????
                    flagCount++;
                }
            }
        }
        if (flagCount == mineCount) {// ?????? ?????? ?????????
            for (int i = 0; i < 8; i++) {
                int around = adjCells[i];
                if (game.isValidIndex(around, index)) {
                    if (!getFlagState(getIbByIndex(around)) && !game.isOpened(around)) {
                        toBeOpen.add(around);
                    }
                }
            }
            openQueue();
        }
    }

    private ImageButton getIbByIndex(int index) {// ???????????? index ????????? ?????? return
        int columnCount = game.columnCount;
        int row = index / columnCount;
        int column = index % columnCount;
        TableRow tr = (TableRow)board.getChildAt(row);
        return (ImageButton)tr.getChildAt(column);
    }
}