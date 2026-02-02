package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.example.game.GameConfig;
import com.example.game.Main;

public class TransitionScreen extends ScreenAdapter {
    final Main game;
    private AssetManager manager;
    
    // 演出用
    private float[] rainDrops;
    private char[] rainChars;
    private final int FONT_SIZE = 20;
    private int columns;
    
    // HUD用
    private ShapeRenderer shapeRenderer;
    private float timeElapsed = 0;
    private float displayProgress = 0;
    private String loadingText = "SYSTEM INITIALIZING";
    
    // カラー設定
    private final Color THEME_COLOR = new Color(0f, 1f, 1f, 1f); 
    private final Color BAR_BG_COLOR = new Color(0f, 0.2f, 0.5f, 0.5f); 

    public TransitionScreen(Main game) {
        this.game = game;
        this.manager = game.assetManager;
        this.shapeRenderer = new ShapeRenderer();

        // =========================================================
        // ★重要：ここで「実際に存在するファイル」だけを予約する
        // =========================================================
        
        // 1. UI画像の予約 (ファイル名を正確に！)
        manager.load("song-select-UI.png", Texture.class);
        
        // 2. 連番画像の予約
        // SongSelectScreenで使う定数と同じ設定
        final int LOOP_FRAMES = 192;
        final String LOOP_PATH = "tunnel/%05d.png";

        for (int i = 1; i <= LOOP_FRAMES; i++) {
            manager.load(String.format(LOOP_PATH, i), Texture.class);
        }

        final int MOVE_FRAMES = 192;
        final String MOVE_PATH = "tunnel_move/%05d.png";

        for (int i = 1; i <= MOVE_FRAMES; i++) {
            manager.load(String.format(MOVE_PATH, i), Texture.class);
        }
        
        // =========================================================

        // 雨エフェクト初期化
        columns = GameConfig.SCREEN_WIDTH / FONT_SIZE;
        rainDrops = new float[columns];
        rainChars = new char[columns];
        for (int i = 0; i < columns; i++) {
            rainDrops[i] = MathUtils.random(0, GameConfig.SCREEN_HEIGHT); 
            rainChars[i] = getRandomChar();
        }
    }

    private char getRandomChar() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ<>/@#&";
        return chars.charAt(MathUtils.random(chars.length() - 1));
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        timeElapsed += delta;

        // --- ロード処理 ---
        boolean isFinished = manager.update(); 
        float realProgress = manager.getProgress();
        displayProgress = MathUtils.lerp(displayProgress, realProgress, delta * 5.0f);

        // --- 1. デジタル雨 ---
        game.batch.begin();
        game.font.getData().setScale(1.0f);
        for (int i = 0; i < columns; i++) {
            game.font.setColor(0.4f, 0.8f, 1f, 0.8f); 
            game.font.draw(game.batch, String.valueOf(rainChars[i]), i * FONT_SIZE, rainDrops[i]);
            if (rainDrops[i] < 0) {
                rainDrops[i] = GameConfig.SCREEN_HEIGHT;
                rainChars[i] = getRandomChar();
            } else {
                rainDrops[i] -= (200f + MathUtils.random(100f)) * delta;
            }
            if (MathUtils.randomBoolean(0.05f)) rainChars[i] = getRandomChar();
        }
        game.batch.end();

        // --- 2. HUD描画 ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(THEME_COLOR);
        
        float w = 600;
        float h = 200;
        float x = (GameConfig.SCREEN_WIDTH - w) / 2;
        float y = (GameConfig.SCREEN_HEIGHT - h) / 2;
        
        float jitter = MathUtils.random(-1f, 1f);
        shapeRenderer.rect(x - jitter, y - jitter, w + jitter*2, h + jitter*2);
        
        shapeRenderer.end();

        // プログレスバー
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BAR_BG_COLOR);
        float barWidth = 500;
        float barHeight = 20;
        float barX = (GameConfig.SCREEN_WIDTH - barWidth) / 2;
        float barY = y + 60;
        shapeRenderer.rect(barX, barY, barWidth, barHeight); 
        
        shapeRenderer.setColor(THEME_COLOR);
        shapeRenderer.rect(barX, barY, barWidth * displayProgress, barHeight);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- 3. テキスト描画 ---
        game.batch.begin();
        game.font.setColor(THEME_COLOR);
        game.font.getData().setScale(1.5f);
        
        String blink = (timeElapsed % 0.5f < 0.25f) ? "_" : "";
        if (realProgress >= 1.0f) loadingText = "LOAD COMPLETE";
        
        game.font.draw(game.batch, loadingText + blink, x + 50, y + 160);
        game.font.draw(game.batch, (int)(displayProgress * 100) + "%", x + 500, y + 160);
        
        game.font.getData().setScale(0.8f);
        game.font.setColor(0.5f, 0.8f, 1f, 1f); 
        game.font.draw(game.batch, "ASSET MANAGER: " + (int)(realProgress * 100) + "%", x + 50, y + 110);
        game.font.draw(game.batch, "QUEUED: " + manager.getQueuedAssets(), x + 50, y + 90);
        game.font.draw(game.batch, "LOADED: " + manager.getLoadedAssets(), x + 50, y + 70);
        game.batch.end();

        // --- 4. 遷移 ---
        // ロード完了 かつ バーの表示も伸びきったら
        if (isFinished && displayProgress >= 0.99f) {
            game.setScreen(new SongSelectScreen(game));
        }
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}