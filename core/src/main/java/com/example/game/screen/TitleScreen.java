package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.example.game.GameConfig;
import com.example.game.Main;

public class TitleScreen extends ScreenAdapter {
    final Main game;
    ShapeRenderer shapeRenderer;
    float time = 0;

    public TitleScreen(Main game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        time += delta;

        // --- 1. 背景のグリッド演出 ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0, 1, 1, 0.3f);
        
        float scroll = (time * 100) % 100;
        
        for (int i = 0; i < GameConfig.SCREEN_WIDTH; i += 100) {
            shapeRenderer.line(i, 0, i, GameConfig.SCREEN_HEIGHT);
        }
        for (int i = 0; i < GameConfig.SCREEN_HEIGHT + 100; i += 100) {
            float y = i - scroll;
            if (y >= 0 && y <= GameConfig.SCREEN_HEIGHT) {
                shapeRenderer.line(0, y, GameConfig.SCREEN_WIDTH, y);
            }
        }
        shapeRenderer.end();
        
        // --- 2. 文字の描画 ---
        game.batch.begin();

        // タイトルロゴ
        game.neonFont.setColor(Color.CYAN);
        game.neonFont.getData().setScale(0.8f); 
        
        String titleText = "RHYTHM GAME";
        GlyphLayout layout = new GlyphLayout(game.neonFont, titleText);
        float titleX = (GameConfig.SCREEN_WIDTH - layout.width) / 2;
        float titleY = GameConfig.SCREEN_HEIGHT * 0.7f; // 画面の上の方(70%)
        
        // 発光演出
        float glow = Math.abs(MathUtils.sin(time * 2));
        game.neonFont.setColor(0.5f, 1f, 1f, 0.8f + glow * 0.2f);
        game.neonFont.draw(game.batch, titleText, titleX, titleY);

        // スタート案内 (点滅)
        if (time % 1.5f > 0.5f) {
            game.neonFont.getData().setScale(0.3f);
            game.neonFont.setColor(Color.WHITE);
            
            String pushText = "PRESS SPACE TO START";
            GlyphLayout pushLayout = new GlyphLayout(game.neonFont, pushText);
            
            // ★修正: 表示位置をもっと下げる (titleY - 350)
            game.neonFont.draw(game.batch, pushText, 
                (GameConfig.SCREEN_WIDTH - pushLayout.width) / 2, 
                titleY - 350); 
        }

        game.batch.end();

        // --- 3. 画面遷移 ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new TransitionScreen(game));
            dispose();
        }
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}