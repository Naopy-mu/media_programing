package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.example.game.GameConfig;
import com.example.game.Main;

public class OptionScreen extends ScreenAdapter {
    final Main game;

    String[] items = {"SPEED", "OFFSET", "BACK"};
    int selectedIndex = 0;

    // 現在の設定値を保持
    float currentSpeed;
    float currentOffset;

    public OptionScreen(Main game) {
        this.game = game;
        // 保存されているデータを読み込む
        currentSpeed = GameConfig.getScrollSpeed();
        currentOffset = GameConfig.getOffset();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        game.batch.begin();

        // タイトル
        game.font.getData().setScale(3.0f);
        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "OPTIONS", GameConfig.SCREEN_WIDTH/2f - 150, 900);

        // 各項目を描画
        for (int i = 0; i < items.length; i++) {
            float y = 600 - (i * 150);
            
            if (i == selectedIndex) {
                game.font.setColor(Color.YELLOW);
                game.font.getData().setScale(2.5f);
                game.font.draw(game.batch, "> " + items[i], GameConfig.SCREEN_WIDTH/2f - 400, y);
            } else {
                game.font.setColor(Color.GRAY);
                game.font.getData().setScale(2.0f);
                game.font.draw(game.batch, items[i], GameConfig.SCREEN_WIDTH/2f - 400, y);
            }

            // 値の表示
            game.font.setColor(Color.WHITE);
            if (i == 0) {
                // SPEED
                String val = String.format("%.1f", currentSpeed);
                game.font.draw(game.batch, val, GameConfig.SCREEN_WIDTH/2f + 100, y);
            } else if (i == 1) {
                // OFFSET
                String val = String.format("%.2f s", currentOffset);
                game.font.draw(game.batch, val, GameConfig.SCREEN_WIDTH/2f + 100, y);
            }
        }
        
        // 操作ガイド
        game.font.getData().setScale(1.5f);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "UP/DOWN: Select   LEFT/RIGHT: Change   SPACE: Return", GameConfig.SCREEN_WIDTH/2f - 400, 200);

        game.batch.end();

        handleInput();
    }

    void handleInput() {
        // 項目移動
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = items.length - 1;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex++;
            if (selectedIndex >= items.length) selectedIndex = 0;
        }

        // 値の変更 (← →)
        if (selectedIndex == 0) { // SPEED
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                currentSpeed += 0.5f;
                if (currentSpeed > 10.0f) currentSpeed = 10.0f;
                GameConfig.setScrollSpeed(currentSpeed); // 保存
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                currentSpeed -= 0.5f;
                if (currentSpeed < 1.0f) currentSpeed = 1.0f;
                GameConfig.setScrollSpeed(currentSpeed); // 保存
            }
        } else if (selectedIndex == 1) { // OFFSET
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                currentOffset += 0.01f;
                GameConfig.setOffset(currentOffset); // 保存
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                currentOffset -= 0.01f;
                GameConfig.setOffset(currentOffset); // 保存
            }
        } else if (selectedIndex == 2) { // BACK
             if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                // ★修正：戻り先を TitleScreen ではなく SongSelectScreen に変更
                game.setScreen(new SongSelectScreen(game));
                dispose();
            }
        }
    }
}