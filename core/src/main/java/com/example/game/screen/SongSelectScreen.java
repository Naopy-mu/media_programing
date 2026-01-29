package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer; // ★マスク用に必要
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.MathUtils;
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
    int[] difficulties = { 5, 8, 4, 6 };

    int selectedIndex = 0;
    float currentScroll = 0;

    Texture panelImg;

    // アニメーション関連
    Animation<Texture> tunnelAnimation;
    Array<Texture> tunnelTextures;
    float animationTime = 0; 
    
    final int TOTAL_FRAMES = 192; 
    final String FRAME_PATH = "tunnel/%05d.png"; 

    // ★マスク描画用のシェイプレンダラー
    ShapeRenderer shapeRenderer;

    // ★円形アニメーションの設定
    final float ANIM_DISPLAY_SIZE = 800f; // 円の直径
    final float ANIM_CENTER_X = 500f;     // 中心のX座標
    
    Music previewMusic;
    String currentPlayingSong = "";

    enum State {
        BROWSING,
        ANIM_ENTER
    }
    State currentState = State.BROWSING;
    float uiAlpha = 1.0f;

    public SongSelectScreen(Main game) {
        this.game = game;
        panelImg = new Texture("song-select-UI.png");
        shapeRenderer = new ShapeRenderer(); // 初期化

        tunnelTextures = new Array<>();
        for (int i = 1; i <= TOTAL_FRAMES; i++) {
            String fileName = String.format(FRAME_PATH, i);
            try {
                Texture t = new Texture(Gdx.files.internal(fileName));
                tunnelTextures.add(t);
            } catch (Exception e) {
                if (i == 1) break;
            }
        }

        if (tunnelTextures.size > 0) {
            tunnelAnimation = new Animation<>(1f / 30f, tunnelTextures, Animation.PlayMode.LOOP);
        }

        playPreview(songs[0]);
    }

    void playPreview(String songName) {
        if (songName.equals(currentPlayingSong)) return;
        if (previewMusic != null) {
            previewMusic.stop();
            previewMusic.dispose();
        }
        try {
            previewMusic = Gdx.audio.newMusic(Gdx.files.internal(songName + ".mp3"));
            previewMusic.setVolume(0.5f);
            previewMusic.setLooping(true);
            previewMusic.play();
            currentPlayingSong = songName;
        } catch(Exception e) { /* 無視 */ }
    }

    @Override
    public void render(float delta) {
        // ★重要：画面クリア時にステンシルバッファもクリアする
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT);
        
        updateLogic(delta);

        // --- 1. アニメーション描画（円形でくり抜く処理） ---
        drawCircularAnimation();

        game.batch.begin();

        // --- 2. UI描画 ---
        if (currentState == State.ANIM_ENTER) {
            uiAlpha = MathUtils.lerp(uiAlpha, 0f, 0.1f);
        }

        if (uiAlpha > 0.01f) {
            drawCirclesAndSpectrum(delta);
            drawSongList(delta); // ★ここが円周配置になります
        }

        game.batch.end();
    }

    // ★新機能：円形マスクを使ってアニメを描画
    void drawCircularAnimation() {
        Texture currentFrame = null;
        if (tunnelAnimation != null) {
            currentFrame = tunnelAnimation.getKeyFrame(animationTime);
        }
        if (currentFrame == null) return;

        // バッチを一旦終了（マスク処理の準備のため）
        if (game.batch.isDrawing()) game.batch.end();

        // ----------------------------------------------------
        // マスク処理開始（ここから難しい魔法の呪文です）
        // ----------------------------------------------------
        
        // 1. ステンシルテストを有効化（型紙を使うモードON）
        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
        
        // 2. 描画を「型紙（ステンシル）」に書き込む設定にする
        Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 1);
        Gdx.gl.glStencilOp(GL20.GL_REPLACE, GL20.GL_REPLACE, GL20.GL_REPLACE);
        Gdx.gl.glColorMask(false, false, false, false); // 色は塗らない（型紙を作るだけ）

        // 3. 円を描く（これが切り抜く形になります）
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float centerY = GameConfig.SCREEN_HEIGHT / 2f;
        shapeRenderer.circle(ANIM_CENTER_X, centerY, ANIM_DISPLAY_SIZE / 2f); // 半径は直径の半分
        shapeRenderer.end();

        // 4. 今度は「色を塗る」設定に戻す
        Gdx.gl.glColorMask(true, true, true, true);
        // 5. 「型紙がある場所（ステンシルが1の場所）」だけ描画するように制限
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 1);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

        // ----------------------------------------------------
        // 画像の描画（この描画は円の中に閉じ込められます）
        // ----------------------------------------------------
        game.batch.begin();
        game.batch.setColor(Color.WHITE);

        // 画像のトリミング計算（前回と同じ）
        float drawX = ANIM_CENTER_X - ANIM_DISPLAY_SIZE / 2f;
        float drawY = centerY - ANIM_DISPLAY_SIZE / 2f;
        int texW = currentFrame.getWidth();
        int texH = currentFrame.getHeight();
        int cropSize = Math.min(texW, texH);
        int srcX = (texW - cropSize) / 2;
        int srcY = (texH - cropSize) / 2;

        game.batch.draw(currentFrame, 
            drawX, drawY, 
            ANIM_DISPLAY_SIZE, ANIM_DISPLAY_SIZE, 
            srcX, srcY, cropSize, cropSize, 
            false, false);
            
        game.batch.end();

        // ----------------------------------------------------
        // マスク処理終了
        // ----------------------------------------------------
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
    }

    void updateLogic(float delta) {
        animationTime += delta;

        switch (currentState) {
            case BROWSING:
                handleInput();
                currentScroll = MathUtils.lerp(currentScroll, (float)selectedIndex, 0.1f);
                break;
            case ANIM_ENTER:
                if (uiAlpha < 0.05f) {
                    game.setScreen(new GameScreen(game, songs[selectedIndex]));
                    dispose();
                }
                break;
        }
    }

    void drawCirclesAndSpectrum(float delta) {
        float centerX = ANIM_CENTER_X;
        float centerY = GameConfig.SCREEN_HEIGHT / 2f;
        
        String difText = String.valueOf(difficulties[selectedIndex]);
        game.font.getData().setScale(5.0f);
        game.font.setColor(0, 1, 1, uiAlpha);
        game.font.draw(game.batch, difText, centerX - 30, centerY + 40);
    }

    // ★修正：円周に沿ってリストを表示
    void drawSongList(float delta) {
        float aspectRatio = (float)panelImg.getHeight() / (float)panelImg.getWidth();
        
        float centerY = GameConfig.SCREEN_HEIGHT / 2f;

        // リストの描画
        for (int i = 0; i < songs.length; i++) {
            float distance = i - currentScroll;
            if (Math.abs(distance) > 5.0f) continue; // 画面外は描かない
            
            // --- 座標計算（ここがポイント！）---
            // 中心からどれくらい離すか（半径）
            // 選択中の曲はアニメのすぐ横、上下の曲は少し遠ざける
            float radius = 450f + Math.abs(distance) * 20f; 

            // 角度の計算（0度が真右。ラジアン単位）
            // distance * -0.3f で、リストの間隔を調整（数字を変えると広さが変わります）
            float angleRad = distance * -0.3f;

            // 三角関数で座標を決定
            // X = 中心X + cos(角度) * 半径
            // Y = 中心Y + sin(角度) * 半径
            float itemX = ANIM_CENTER_X + MathUtils.cos(angleRad) * radius;
            float itemY = centerY       + MathUtils.sin(angleRad) * radius;

            // サイズと透明度の計算（以前と同じロジック）
            float scale = Math.max(0.6f, 1.0f - Math.abs(distance) * 0.15f);
            float alpha = Math.max(0.3f, 1.0f - Math.abs(distance) * 0.5f);
            
            if (i != selectedIndex || currentState == State.ANIM_ENTER) {
                alpha *= 0.5f;
            }
            alpha *= uiAlpha;

            float imgW = 900 * scale;          
            float imgH = imgW * aspectRatio;   
            
            // パネル描画
            game.batch.setColor(1f, 1f, 1f, alpha);
            // 画像の中心が計算した座標(itemX, itemY)に来るように調整
            game.batch.draw(panelImg, itemX, itemY - imgH/2, imgW, imgH);
            
            // 文字描画
            game.font.getData().setScale(2.0f * scale);
            Color c = (i == selectedIndex) ? Color.WHITE : Color.LIGHT_GRAY;
            game.font.setColor(c.r, c.g, c.b, alpha);
            // 文字はパネルより少し左に調整
            game.font.draw(game.batch, songs[i], itemX + 50, itemY + 20);
        }
        game.batch.setColor(1, 1, 1, 1);
    }

    void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = songs.length - 1;
            playPreview(songs[selectedIndex]);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex++;
            if (selectedIndex >= songs.length) selectedIndex = 0;
            playPreview(songs[selectedIndex]);
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            currentState = State.ANIM_ENTER;
            if (previewMusic != null) previewMusic.stop();
        }
    }

    @Override
    public void dispose() {
        if (panelImg != null) panelImg.dispose();
        if (previewMusic != null) previewMusic.dispose();
        if (tunnelTextures != null) {
            for (Texture t : tunnelTextures) t.dispose();
        }
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}