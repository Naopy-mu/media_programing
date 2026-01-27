package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.example.game.Main;
import com.example.game.GameConfig;

public class ResultScreen extends ScreenAdapter {
    final Main game;
    int score;
    String rank;

    public ResultScreen(Main game, int score, String rank) {
        this.game = game;
        this.score = score;
        this.rank = rank;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        game.batch.begin();
        game.font.getData().setScale(5.0f);
        game.font.setColor(Color.YELLOW);
        game.font.draw(game.batch, "GAME CLEAR!!", GameConfig.SCREEN_WIDTH/2f - 300, 700);

        game.font.getData().setScale(4.0f);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "SCORE: " + score, GameConfig.SCREEN_WIDTH/2f - 200, 550);

        game.font.draw(game.batch, "RANK: " + rank, GameConfig.SCREEN_WIDTH/2f - 100, 400);

        game.font.getData().setScale(2.0f);
        game.font.draw(game.batch, "Press SPACE to Title", GameConfig.SCREEN_WIDTH/2f - 180, 200);
        game.batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            // タイトルへ戻る
            game.setScreen(new TitleScreen(game));
            dispose();
        }
    }
}