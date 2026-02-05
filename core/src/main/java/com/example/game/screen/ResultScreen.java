package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils; // 必要ならimport
import com.example.game.GameConfig;
import com.example.game.Main;

public class ResultScreen extends ScreenAdapter {
    final Main game;
    
    int score;
    int maxCombo;
    int perfect, great, good, miss;
    String rank;
    Color rankColor;

    float timeElapsed = 0;
    int displayScore = 0;

    // コンストラクタ：詳細データを受け取る
    public ResultScreen(Main game, int score, int maxCombo, int perfect, int great, int good, int miss) {
        this.game = game;
        this.score = score;
        this.maxCombo = maxCombo;
        this.perfect = perfect;
        this.great = great;
        this.good = good;
        this.miss = miss;

        calculateRank();
    }

    private void calculateRank() {
        if (score >= 980000) { rank = "S+"; rankColor = Color.CYAN; }
        else if (score >= 950000) { rank = "S"; rankColor = Color.GOLD; }
        else if (score >= 900000) { rank = "A"; rankColor = Color.GREEN; }
        else if (score >= 800000) { rank = "B"; rankColor = Color.YELLOW; }
        else if (score >= 700000) { rank = "C"; rankColor = Color.ORANGE; }
        else { rank = "D"; rankColor = Color.GRAY; }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        timeElapsed += delta;
        float progress = Math.min(timeElapsed / 2.0f, 1.0f);
        displayScore = (int)Interpolation.pow2Out.apply(0, score, progress);

        game.batch.begin();

        // TITLE
        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(3.0f);
        drawCenteredText("RESULT", 750);

        // RANK (2秒後に表示)
        if (timeElapsed > 2.0f) {
            game.font.setColor(rankColor);
            game.font.getData().setScale(12.0f);
            drawCenteredText(rank, 550);
        }

        // SCORE
        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(5.0f);
        drawCenteredText(String.format("SCORE: %,d", displayScore), 350);

        // STATS
        drawDetailStats(GameConfig.SCREEN_WIDTH / 2f);

        // GUIDE
        if (timeElapsed > 3.0f) {
            game.font.setColor(Color.LIGHT_GRAY);
            game.font.getData().setScale(1.5f);
            drawCenteredText("Press [SPACE] to Return", 50);
        }

        game.batch.end();

        if (timeElapsed > 3.0f) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                game.setScreen(new SongSelectScreen(game));
                dispose();
            }
        }
    }

    private void drawDetailStats(float centerX) {
        float startY = 250;
        float lineHeight = 40;
        float labelX = centerX - 200;
        float valueX = centerX + 50;

        game.font.getData().setScale(2.0f);

        game.font.setColor(Color.YELLOW);
        game.font.draw(game.batch, "MAX COMBO", labelX, startY);
        game.font.draw(game.batch, String.valueOf(maxCombo), valueX, startY);

        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "PERFECT", labelX, startY - lineHeight * 1.5f);
        game.font.draw(game.batch, String.valueOf(perfect), valueX, startY - lineHeight * 1.5f);

        game.font.setColor(Color.GREEN);
        game.font.draw(game.batch, "GREAT", labelX, startY - lineHeight * 2.5f);
        game.font.draw(game.batch, String.valueOf(great), valueX, startY - lineHeight * 2.5f);
        
        game.font.setColor(Color.YELLOW);
        game.font.draw(game.batch, "GOOD", labelX, startY - lineHeight * 3.5f);
        game.font.draw(game.batch, String.valueOf(good), valueX, startY - lineHeight * 3.5f);

        game.font.setColor(Color.RED);
        game.font.draw(game.batch, "MISS", labelX, startY - lineHeight * 4.5f);
        game.font.draw(game.batch, String.valueOf(miss), valueX, startY - lineHeight * 4.5f);
    }

    private void drawCenteredText(String text, float y) {
        var layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, text);
        float x = (GameConfig.SCREEN_WIDTH - layout.width) / 2f;
        game.font.draw(game.batch, text, x, y);
    }
}