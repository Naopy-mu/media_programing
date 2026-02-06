package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.example.game.GameConfig;
import com.example.game.Main;

public class ResultScreen extends ScreenAdapter {
    final Main game;
    String songName;
    
    int score;
    int maxCombo;
    int perfect, great, good, miss;
    String rank;
    Color rankColor;

    float timeElapsed = 0;
    int displayScore = 0;

    public ResultScreen(Main game, String songName, int score, int maxCombo, int perfect, int great, int good, int miss) {
        // コンストラクタ
        this.game = game;
        this.songName = songName;
        this.score = score;
        this.maxCombo = maxCombo;
        this.perfect = perfect;
        this.great = great;
        this.good = good;
        this.miss = miss;

        calculateRank();
    }

    private void calculateRank() {
        // ランク計算
        if (score >= 980000) { rank = "S+"; rankColor = Color.CYAN; }
        else if (score >= 950000) { rank = "S"; rankColor = Color.GOLD; }
        else if (score >= 900000) { rank = "A"; rankColor = Color.GREEN; }
        else if (score >= 800000) { rank = "B"; rankColor = Color.YELLOW; }
        else if (score >= 700000) { rank = "C"; rankColor = Color.ORANGE; }
        else { rank = "D"; rankColor = Color.GRAY; }
    }

    @Override
    public void render(float delta) {
        // 背景クリア
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        timeElapsed += delta;
        float progress = Math.min(timeElapsed / 2.0f, 1.0f);
        displayScore = (int)Interpolation.pow2Out.apply(0, score, progress);

        game.batch.begin();

        // タイトル
        game.neonFont.setColor(Color.WHITE);
        game.neonFont.getData().setScale(0.5f);
        drawCenteredText("RESULT", 950);

        // ランク
        if (timeElapsed > 0.5f) {
            game.neonFont.setColor(rankColor);
            game.neonFont.getData().setScale(1.5f); 
            game.neonFont.draw(game.batch, rank, 350, 800);
        }

        // スコア表示
        game.neonFont.setColor(Color.WHITE);
        game.neonFont.getData().setScale(0.4f);
        game.neonFont.draw(game.batch, "SCORE", 1000, 750);
        
        game.neonFont.getData().setScale(0.55f); 
        game.neonFont.draw(game.batch, String.format("%,d", displayScore), 1000, 670);

        // 詳細ステータス表示
        drawDetailStats(GameConfig.SCREEN_WIDTH / 2f);

        // 操作ガイド
        if (timeElapsed > 3.0f) {
            game.neonFont.setColor(Color.LIGHT_GRAY);
            game.neonFont.getData().setScale(0.25f);
            drawCenteredText("[SPACE] SONG SELECT    [R] RETRY", 100);
        }

        game.batch.end();

        // 入力処理
        if (timeElapsed > 1.0f) {
            // 曲選択へ戻るまたはリトライ
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                // 曲選択画面へ戻る
                game.setScreen(new SongSelectScreen(game));
                dispose();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                // リトライ
                 game.setScreen(new GameScreen(game, songName));
                 dispose();
            }
        }
    }

    private void drawDetailStats(float centerX) {
        // 詳細ステータス描画
        float startY = 450;
        float lineHeight = 55; 
        float labelX = centerX - 200;
        float valueX = centerX + 300;

        game.neonFont.getData().setScale(0.25f);

        game.neonFont.setColor(Color.YELLOW);
        game.neonFont.draw(game.batch, "MAX COMBO", labelX, startY);
        game.neonFont.draw(game.batch, String.valueOf(maxCombo), valueX, startY);

        game.neonFont.setColor(Color.CYAN);
        game.neonFont.draw(game.batch, "PERFECT", labelX, startY - lineHeight);
        game.neonFont.draw(game.batch, String.valueOf(perfect), valueX, startY - lineHeight);

        game.neonFont.setColor(Color.GREEN);
        game.neonFont.draw(game.batch, "GREAT", labelX, startY - lineHeight * 2);
        game.neonFont.draw(game.batch, String.valueOf(great), valueX, startY - lineHeight * 2);
        
        game.neonFont.setColor(Color.YELLOW);
        game.neonFont.draw(game.batch, "GOOD", labelX, startY - lineHeight * 3);
        game.neonFont.draw(game.batch, String.valueOf(good), valueX, startY - lineHeight * 3);

        game.neonFont.setColor(Color.RED);
        game.neonFont.draw(game.batch, "MISS", labelX, startY - lineHeight * 4);
        game.neonFont.draw(game.batch, String.valueOf(miss), valueX, startY - lineHeight * 4);
    }

    private void drawCenteredText(String text, float y) {
        // 中央揃えテキスト描画
        var layout = new GlyphLayout(game.neonFont, text);
        float x = (GameConfig.SCREEN_WIDTH - layout.width) / 2f;
        game.neonFont.draw(game.batch, text, x, y);
    }
}