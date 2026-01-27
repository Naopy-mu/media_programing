package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.example.game.Main;
import com.example.game.GameConfig;

public class TitleScreen extends ScreenAdapter {
    final Main game;

    public TitleScreen(Main game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        game.batch.begin();
        game.font.getData().setScale(4.0f);
        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "RHYTHM GAME", GameConfig.SCREEN_WIDTH/2f - 250, 700);
        
        game.font.getData().setScale(2.0f);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "Press SPACE to Start", GameConfig.SCREEN_WIDTH/2f - 180, 500);
        game.batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            // ゲーム画面へ移動
            game.setScreen(new GameScreen(game));
            dispose();
        }
    }
}