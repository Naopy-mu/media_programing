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
    
    // 曲リスト（とりあえず仮の曲名を入れます）
    String[] songs = {
        "Timepiece Tower",
        "Test Song 1",
        "Test Song 2"
    };
    int selectedIndex = 0;

    public SongSelectScreen(Main game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        game.batch.begin();
        
        // ヘッダー
        game.font.getData().setScale(3.0f);
        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "SELECT MUSIC", GameConfig.SCREEN_WIDTH/2f - 200, 900);

        // リスト表示
        for (int i = 0; i < songs.length; i++) {
            float y = 600 - (i * 120); // 縦にずらして表示

            if (i == selectedIndex) {
                // 選択中の曲：大きく、黄色く、矢印付き
                game.font.getData().setScale(2.5f);
                game.font.setColor(Color.YELLOW);
                game.font.draw(game.batch, "> " + songs[i] + " <", GameConfig.SCREEN_WIDTH/2f - 300, y);
            } else {
                // その他の曲：小さく、グレー
                game.font.getData().setScale(2.0f);
                game.font.setColor(Color.GRAY);
                game.font.draw(game.batch, songs[i], GameConfig.SCREEN_WIDTH/2f - 200, y);
            }
        }
        
        // 操作ガイド
        game.font.getData().setScale(1.5f);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "UP/DOWN: Select   SPACE: Start", GameConfig.SCREEN_WIDTH/2f - 250, 200);

        game.batch.end();

        // キー入力処理
        handleInput();
    }

    void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = songs.length - 1; // 一番上なら一番下へ
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex++;
            if (selectedIndex >= songs.length) selectedIndex = 0; // 一番下なら一番上へ
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            // 決定！ゲーム画面へ遷移
            // ★ポイント：選択された曲名を渡す
            game.setScreen(new GameScreen(game, songs[selectedIndex])); 
            dispose();
        }
    }
}