package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.example.game.GameConfig;
import com.example.game.Main;

public class DevSelectScreen extends ScreenAdapter {
    final Main game;
    
    // 編集可能な曲リスト
    String[] songs = { "Link Layer", "Pop!Stack!", "Timepiece Tower", "Eigenstate" };
    int selectedIndex = 0;

    public DevSelectScreen(Main game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        // 背景を黒く
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleInput();

        game.batch.begin();
        
        // タイトル
        game.font.setColor(Color.MAGENTA);
        game.font.getData().setScale(2.5f);
        game.font.draw(game.batch, "=== DEVELOPER MODE ===", 50, GameConfig.SCREEN_HEIGHT - 50);
        game.font.draw(game.batch, "SELECT SONG TO EDIT", 50, GameConfig.SCREEN_HEIGHT - 100);

        // 曲リスト表示
        for (int i = 0; i < songs.length; i++) {
            float y = GameConfig.SCREEN_HEIGHT - 250 - (i * 60);
            
            if (i == selectedIndex) {
                game.font.setColor(Color.GREEN); // 選択中は緑
                game.font.draw(game.batch, "> " + songs[i], 100, y);
            } else {
                game.font.setColor(Color.GRAY);
                game.font.draw(game.batch, "  " + songs[i], 100, y);
            }
        }
        
        // 操作説明
        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(1.5f);
        game.font.draw(game.batch, "[UP/DOWN]: Select   [ENTER]: Edit   [ESC]: Back", 50, 50);
        
        game.batch.end();
    }

    void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = songs.length - 1;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex++;
            if (selectedIndex >= songs.length) selectedIndex = 0;
        }
        
        // エディター起動
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new EditorScreen(game, songs[selectedIndex]));
        }
        
        // 戻る
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new SongSelectScreen(game));
        }
    }
}