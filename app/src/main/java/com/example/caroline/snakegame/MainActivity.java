package com.example.caroline.snakegame;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;


/**
 * Created by hzlinxuanxuan on 2015/10/8.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private SnakeView viewSnake;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewSnake = (SnakeView) findViewById(R.id.view_snake);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_begin:
                viewSnake.beginGame();
                break;
            case R.id.btn_pause:
                viewSnake.pauseGame();
                break;
            case R.id.btn_reset:
                viewSnake.initGame();
        }

    }
}