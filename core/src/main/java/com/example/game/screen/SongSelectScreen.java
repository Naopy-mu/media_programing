package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.example.game.Main;
import com.example.game.GameConfig;

public class SongSelectScreen extends ScreenAdapter {
    final Main game;
    
    String[] songs = {
        "Timepiece Tower",
        "Eigenstate",
        "Link Layer",
        "Pop!Stack!"
    };
    int selectedIndex = 0;

    public SongSelectScreen(Main game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        game.batch.begin();
        
        game.font.getData().setScale(3.0f);
        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "SELECT MUSIC", GameConfig.SCREEN_WIDTH/2f - 200, 900);

        for (int i = 0; i < songs.length; i++) {
            float y = 600 - (i * 120); 

            if (i == selectedIndex) {
                game.font.getData().setScale(2.5f);
                game.font.setColor(Color.YELLOW);
                game.font.draw(game.batch, "> " + songs[i] + " <", GameConfig.SCREEN_WIDTH/2f - 300, y);
            } else {
                game.font.getData().setScale(2.0f);
                game.font.setColor(Color.GRAY);
                game.font.draw(game.batch, songs[i], GameConfig.SCREEN_WIDTH/2f - 200, y);
            }
        }
        
        // ガイド表示
        game.font.getData().setScale(1.5f);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "UP/DOWN: Select   SPACE: Start", GameConfig.SCREEN_WIDTH/2f - 250, 200);
        
        // ★追加：オプションへのガイドを表示
        game.font.setColor(Color.GRAY);
        game.font.draw(game.batch, "[O] OPTION / [ESC] BACK", 20, 50);

        game.batch.end();

        handleInput();
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
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game, songs[selectedIndex])); 
            dispose();
        }

        // ★追加：Oキーでオプション画面へ
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            game.setScreen(new OptionScreen(game)); 
            dispose();
        }

        // ★おまけ：ESCキーでタイトルに戻る（あると便利です）
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new TitleScreen(game));
            dispose();
        }
    }
}