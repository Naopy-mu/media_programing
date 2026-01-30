package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.MathUtils;
import com.example.game.Main;
import com.example.game.GameConfig;

public class SongSelectScreen extends ScreenAdapter {
    final Main game;
    
    // --- 曲データ ---
    String[] songs = { "Timepiece Tower", "Eigenstate", "Link Layer", "Pop!Stack!" };
    int[] difficulties = { 5, 8, 4, 6 };
    int selectedIndex = 0;
    float currentScroll = 0;
    Texture panelImg;

    // --- アニメーション素材 ---
    Animation<Texture> loopAnimation;
    Array<Texture> loopTextures;
    final int TOTAL_LOOP_FRAMES = 192; 
    final String LOOP_PATH = "tunnel/%05d.png"; 

    Animation<Texture> moveAnimation;
    Array<Texture> moveTextures;
    final int TOTAL_MOVE_FRAMES = 192; 
    final String MOVE_PATH = "tunnel_move/%05d.png"; 

    float animationTime = 0; 
    float moveAnimTime = 0;

    // --- 描画・演出変数 ---
    ShapeRenderer shapeRenderer;
    final float ANIM_DISPLAY_SIZE = 800f;
    final float BROWSING_CENTER_X = 500f;
    float currentAnimX;

    // ホワイトアウト演出用の変数
    float fadeAlpha = 0f;       

    Music previewMusic;
    String currentPlayingSong = "";

    enum State { BROWSING, MOVING_CENTER, PLAYING_INTRO }
    State currentState = State.BROWSING;
    float uiAlpha = 1.0f;

    // メモリ解放済みフラグ
    boolean assetsDisposed = false;

    public SongSelectScreen(Main game) {
        this.game = game;
        
        try {
            panelImg = new Texture("song-select-UI.png");
        } catch(Exception e) {
            System.err.println("UI画像が見つかりません！");
        }
        
        shapeRenderer = new ShapeRenderer();
        currentAnimX = BROWSING_CENTER_X;

        // =========================================================
        // ★設定反映：画像を「全フレーム」読み込む (i += 1)
        // =========================================================
        
        // --- 1. ループ画像の読み込み ---
        loopTextures = new Array<>();
        // 指定通り i += 1 に変更
        for (int i = 1; i <= TOTAL_LOOP_FRAMES; i += 1) { 
            String path = String.format(LOOP_PATH, i);
            try {
                if (Gdx.files.internal(path).exists()) {
                    loopTextures.add(new Texture(Gdx.files.internal(path)));
                }
            } catch (Throwable e) { /* 無視 */ }
        }
        if (loopTextures.size > 0) {
            // 指定通り 1/15秒 間隔
            loopAnimation = new Animation<>(1f / 15f, loopTextures, Animation.PlayMode.LOOP);
        }

        // --- 2. 突入画像の読み込み ---
        moveTextures = new Array<>();
        // 指定通り i += 1 に変更
        for (int i = 1; i <= TOTAL_MOVE_FRAMES; i += 1) { 
            String path = String.format(MOVE_PATH, i);
            try {
                if (Gdx.files.internal(path).exists()) {
                    moveTextures.add(new Texture(Gdx.files.internal(path)));
                }
            } catch (Throwable e) { /* 無視 */ }
        }
        if (moveTextures.size > 0) {
            // 指定通り 1/60秒 間隔
            moveAnimation = new Animation<>(1f / 60f, moveTextures, Animation.PlayMode.NORMAL);
        }

        playPreview(songs[0]);
    }

    void playPreview(String songName) {
        if (songName.equals(currentPlayingSong)) return;
        if (previewMusic != null) { previewMusic.stop(); previewMusic.dispose(); }
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
        if (assetsDisposed) {
            Gdx.gl.glClearColor(1, 1, 1, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            return;
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT);
        
        updateLogic(delta);
        
        drawCircularAnimation();

        // 突入アニメ中（PLAYING_INTRO）は UIを一切描画しない
        if (currentState != State.PLAYING_INTRO) {
            game.batch.begin();
            // フェードアウト中のみ描画
            if (uiAlpha > 0.01f) {
                drawCirclesAndSpectrum(delta);
                drawSongList(delta);
            }
            game.batch.end();
        }

        // 段階的ホワイトアウト処理
        if (fadeAlpha > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(1f, 1f, 1f, fadeAlpha);
            shapeRenderer.rect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
            shapeRenderer.end();
            
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    void drawCircularAnimation() {
        if (currentState == State.PLAYING_INTRO) {
            Texture moveFrame = null;
            if (moveAnimation != null) moveFrame = moveAnimation.getKeyFrame(moveAnimTime, false);
            
            if (moveFrame != null) {
                game.batch.begin();
                game.batch.setColor(Color.WHITE);
                game.batch.draw(moveFrame, 0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
                game.batch.end();
            }
            return;
        }

        Texture loopFrame = null;
        if (loopAnimation != null) loopFrame = loopAnimation.getKeyFrame(animationTime);
        if (loopFrame == null) return;

        if (game.batch.isDrawing()) game.batch.end();

        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
        Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 1);
        Gdx.gl.glStencilOp(GL20.GL_REPLACE, GL20.GL_REPLACE, GL20.GL_REPLACE);
        Gdx.gl.glColorMask(false, false, false, false);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.circle(currentAnimX, GameConfig.SCREEN_HEIGHT / 2f, ANIM_DISPLAY_SIZE / 2f);
        shapeRenderer.end();

        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 1);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

        game.batch.begin();
        game.batch.setColor(Color.WHITE);

        float drawX = currentAnimX - ANIM_DISPLAY_SIZE / 2f;
        float drawY = (GameConfig.SCREEN_HEIGHT / 2f) - ANIM_DISPLAY_SIZE / 2f;
        
        int texW = loopFrame.getWidth();
        int texH = loopFrame.getHeight();
        int cropSize = Math.min(texW, texH);
        int srcX = (texW - cropSize) / 2;
        int srcY = (texH - cropSize) / 2;

        game.batch.draw(loopFrame, drawX, drawY, ANIM_DISPLAY_SIZE, ANIM_DISPLAY_SIZE, srcX, srcY, cropSize, cropSize, false, false);
        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
    }

    void updateLogic(float delta) {
        if (assetsDisposed) return; 

        animationTime += delta;
        switch (currentState) {
            case BROWSING:
                handleInput();
                currentScroll = MathUtils.lerp(currentScroll, (float)selectedIndex, 0.1f);
                break;
            case MOVING_CENTER:
                float targetX = GameConfig.SCREEN_WIDTH / 2f;
                currentAnimX = MathUtils.lerp(currentAnimX, targetX, 0.2f);
                // 中央移動中はUIをフェードアウト
                uiAlpha = MathUtils.lerp(uiAlpha, 0f, 0.2f); 

                if (Math.abs(currentAnimX - targetX) < 5.0f) {
                    currentAnimX = targetX; 
                    currentState = State.PLAYING_INTRO;
                    moveAnimTime = 0;
                    uiAlpha = 0f; // UIを完全に消去
                }
                break;
            case PLAYING_INTRO:
                moveAnimTime += delta;
                
                if (moveAnimation != null) {
                    float duration = moveAnimation.getAnimationDuration();
                    float progress = moveAnimTime / duration;
                    
                    // アニメの後半から白くしていく
                    if (progress > 0.5f) {
                        fadeAlpha = (progress - 0.5f) * 2.0f; 
                    } else {
                        fadeAlpha = 0f;
                    }
                    
                    if (fadeAlpha > 1.0f) fadeAlpha = 1.0f;

                    if (moveAnimation.isAnimationFinished(moveAnimTime)) {
                        manualDisposeHeavyAssets();
                        assetsDisposed = true;
                        game.setScreen(new GameScreen(game, songs[selectedIndex]));
                        return;
                    }
                }
                break;
        }
    }

    void drawCirclesAndSpectrum(float delta) {
        float centerX = BROWSING_CENTER_X; 
        float centerY = GameConfig.SCREEN_HEIGHT / 2f;
        String difText = String.valueOf(difficulties[selectedIndex]);
        game.font.getData().setScale(5.0f);
        game.font.setColor(0, 1, 1, uiAlpha);
        game.font.draw(game.batch, difText, centerX - 30, centerY + 40);
    }

    void drawSongList(float delta) {
        if (panelImg == null) return;
        float aspectRatio = (float)panelImg.getHeight() / (float)panelImg.getWidth();
        float centerY = GameConfig.SCREEN_HEIGHT / 2f;
        for (int i = 0; i < songs.length; i++) {
            float distance = i - currentScroll;
            if (Math.abs(distance) > 5.0f) continue;
            float radius = 450f + Math.abs(distance) * 20f; 
            float angleRad = distance * -0.3f;
            float itemX = BROWSING_CENTER_X + MathUtils.cos(angleRad) * radius;
            float itemY = centerY           + MathUtils.sin(angleRad) * radius;
            float scale = Math.max(0.6f, 1.0f - Math.abs(distance) * 0.15f);
            float alpha = Math.max(0.3f, 1.0f - Math.abs(distance) * 0.5f);
            if (i != selectedIndex || currentState != State.BROWSING) alpha *= 0.5f;
            alpha *= uiAlpha;
            float imgW = 900 * scale;          
            float imgH = imgW * aspectRatio;   
            game.batch.setColor(1f, 1f, 1f, alpha);
            game.batch.draw(panelImg, itemX, itemY - imgH/2, imgW, imgH);
            game.font.getData().setScale(2.0f * scale);
            Color c = (i == selectedIndex) ? Color.WHITE : Color.LIGHT_GRAY;
            game.font.setColor(c.r, c.g, c.b, alpha);
            game.font.draw(game.batch, songs[i], itemX + 50, itemY + 20);
        }
        game.batch.setColor(1, 1, 1, 1);
    }

    void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex--; if (selectedIndex < 0) selectedIndex = songs.length - 1; playPreview(songs[selectedIndex]);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex++; if (selectedIndex >= songs.length) selectedIndex = 0; playPreview(songs[selectedIndex]);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            currentState = State.MOVING_CENTER; if (previewMusic != null) previewMusic.stop();
        }
    }

    private void manualDisposeHeavyAssets() {
        System.out.println("Switching screens: Disposing heavy assets now...");
        if (loopTextures != null) {
            for (Texture t : loopTextures) { if (t != null) t.dispose(); }
            loopTextures.clear();
        }
        if (moveTextures != null) {
            for (Texture t : moveTextures) { if (t != null) t.dispose(); }
            moveTextures.clear();
        }
    }

    @Override
    public void dispose() {
        if (!assetsDisposed) {
            manualDisposeHeavyAssets();
        }
        if (panelImg != null) panelImg.dispose();
        if (previewMusic != null) previewMusic.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}