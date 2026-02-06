package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.example.game.GameConfig;
import com.example.game.Main;

public class SongSelectScreen extends ScreenAdapter {
    final Main game;
    
    // 曲データ
    String[] songs = { "Timepiece Tower", "Eigenstate", "Link Layer", "Pop!Stack!" };
    int[] difficulties = { 5, 8, 4, 6 };
    int selectedIndex = 0;
    float currentScroll = 0;
    Texture panelImg;

    // ★追加: ジャケット画像管理用マップ
    ObjectMap<String, Texture> jackets = new ObjectMap<>();

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

    ShapeRenderer shapeRenderer;
    final float ANIM_DISPLAY_SIZE = 800f;
    final float BROWSING_CENTER_X = 500f;
    float currentAnimX;
    float fadeAlpha = 0f;       

    Music previewMusic;
    String currentPlayingSong = "";

    enum State { BROWSING, MOVING_CENTER, PLAYING_INTRO }
    State currentState = State.BROWSING;
    float uiAlpha = 1.0f;
    boolean assetsDisposed = false;

    public SongSelectScreen(Main game) {
        this.game = game;
        
        if (game.assetManager.isLoaded("song-select-UI.png")) {
            panelImg = game.assetManager.get("song-select-UI.png", Texture.class);
        } else {
            panelImg = new Texture("song-select-UI.png");
        }

        // ★追加: ジャケット画像の読み込み
        for (String songName : songs) {
            try {
                Texture tex = new Texture(Gdx.files.internal(songName + ".png"));
                // 拡大時にぼやけるようにフィルターを設定
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                jackets.put(songName, tex);
            } catch (Exception e) {
                Gdx.app.error("SongSelect", "Could not load jacket for: " + songName);
            }
        }
        
        shapeRenderer = new ShapeRenderer();
        currentAnimX = BROWSING_CENTER_X;

        // アニメーション読み込み
        loopTextures = new Array<>();
        for (int i = 1; i <= TOTAL_LOOP_FRAMES; i++) { 
            String path = String.format(LOOP_PATH, i);
            if (game.assetManager.isLoaded(path)) loopTextures.add(game.assetManager.get(path, Texture.class));
            else loopTextures.add(new Texture(Gdx.files.internal(path)));
        }
        if (loopTextures.size > 0) loopAnimation = new Animation<>(1f / 15f, loopTextures, Animation.PlayMode.LOOP);

        moveTextures = new Array<>();
        for (int i = 1; i <= TOTAL_MOVE_FRAMES; i++) { 
            String path = String.format(MOVE_PATH, i);
            if (game.assetManager.isLoaded(path)) moveTextures.add(game.assetManager.get(path, Texture.class));
            else moveTextures.add(new Texture(Gdx.files.internal(path)));
        }
        if (moveTextures.size > 0) moveAnimation = new Animation<>(1f / 60f, moveTextures, Animation.PlayMode.NORMAL);

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
        } catch(Exception e) { }
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

        // ★修正: 背景ジャケット描画
        game.batch.begin();
        Texture currentJacket = jackets.get(songs[selectedIndex]);
        if (currentJacket != null) {
            // 少し暗く(0.6)、少し透明に(0.8)して描画することで、擬似的なブラー効果とUIの引き立てを行う
            game.batch.setColor(0.6f, 0.6f, 0.6f, 0.8f * uiAlpha);
            game.batch.draw(currentJacket, 0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        }
        game.batch.end();
        
        drawCircularAnimation();

        if (currentState != State.PLAYING_INTRO) {
            game.batch.begin();
            if (uiAlpha > 0.01f) {
                drawCirclesAndSpectrum(delta);
                drawSongList(delta);
                drawUI(delta);
            }
            game.batch.end();
        }

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
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            if (previewMusic != null) previewMusic.stop();
            game.setScreen(new DevSelectScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            if (previewMusic != null) previewMusic.stop();
            game.setScreen(new OptionScreen(game));
            return;
        }

        animationTime += delta;
        switch (currentState) {
            case BROWSING:
                handleInput();
                currentScroll = MathUtils.lerp(currentScroll, (float)selectedIndex, 0.1f);
                break;
            case MOVING_CENTER:
                float targetX = GameConfig.SCREEN_WIDTH / 2f;
                currentAnimX = MathUtils.lerp(currentAnimX, targetX, 0.2f);
                uiAlpha = MathUtils.lerp(uiAlpha, 0f, 0.2f); 
                if (Math.abs(currentAnimX - targetX) < 5.0f) {
                    currentAnimX = targetX; 
                    currentState = State.PLAYING_INTRO;
                    moveAnimTime = 0;
                    uiAlpha = 0f; 
                }
                break;
            case PLAYING_INTRO:
                moveAnimTime += delta;
                if (moveAnimation != null) {
                    float duration = moveAnimation.getAnimationDuration();
                    float progress = moveAnimTime / duration;
                    if (progress > 0.5f) fadeAlpha = (progress - 0.5f) * 2.0f; else fadeAlpha = 0f;
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

    // ★修正: 難易度を円の中心に (微調整)
    void drawCirclesAndSpectrum(float delta) {
        float centerX = BROWSING_CENTER_X; 
        float centerY = GameConfig.SCREEN_HEIGHT / 2f;
        String difText = String.valueOf(difficulties[selectedIndex]);
        
        game.neonFont.getData().setScale(0.5f); 
        game.neonFont.setColor(0, 1, 1, uiAlpha);
        
        GlyphLayout layout = new GlyphLayout(game.neonFont, difText);
        float textX = centerX - layout.width / 2f;
        
        // ★変更点: layout.height ではなく getCapHeight() を使うことで、
        // 数字のようなベースラインの影響を受けやすい文字を視覚的な中心に配置します。
        float textY = centerY + 25 + game.neonFont.getCapHeight() / 2f; 
        
        game.neonFont.draw(game.batch, difText, textX, textY);
    }

    // ★修正: 曲名の表示位置を微調整
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
            
            float imgDrawX = itemX; 
            float imgDrawY = itemY - imgH/2;
            game.batch.draw(panelImg, imgDrawX, imgDrawY, imgW, imgH);
            
            // 文字設定
            game.neonFont.getData().setScale(0.25f * scale); 
            
            Color c = (i == selectedIndex) ? Color.WHITE : new Color(0.7f, 0.7f, 0.7f, 1f);
            game.neonFont.setColor(c.r, c.g, c.b, alpha);
            
            String songText = songs[i];
            GlyphLayout layout = new GlyphLayout(game.neonFont, songText);
            
            // ★変更点: 固定値(+130)ではなく、画像の幅に基づいて中心を計算
            float imgCenterX = imgDrawX + (imgW / 2f);
            float textX = imgCenterX - (layout.width / 2f);
            
            // ★変更点: 垂直方向もCapHeightを使用して、フォントの「サイズ感」によるズレを解消
            // itemY はパネルの垂直中心なので、そこからCapHeightの半分を足す
            float textY = itemY + 15 + (game.neonFont.getCapHeight() / 2f); 
            
            game.neonFont.draw(game.batch, songText, textX, textY);
        }
        game.batch.setColor(1, 1, 1, 1);
    }

    void drawUI(float delta) {
        game.font.getData().setScale(1.5f);
        game.font.setColor(Color.LIGHT_GRAY);
        game.font.draw(game.batch, "[SPACE] START   [O] OPTION", 20, 50);
        game.font.draw(game.batch, "[F1] DEV MODE", GameConfig.SCREEN_WIDTH - 250, 50);
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
        System.out.println("Switching screens: Unloading heavy assets from Manager...");
        for (int i = 1; i <= TOTAL_LOOP_FRAMES; i++) {
            String path = String.format(LOOP_PATH, i);
            if (game.assetManager.isLoaded(path)) game.assetManager.unload(path);
        }
        for (int i = 1; i <= TOTAL_MOVE_FRAMES; i++) {
            String path = String.format(MOVE_PATH, i);
            if (game.assetManager.isLoaded(path)) game.assetManager.unload(path);
        }
        if (game.assetManager.isLoaded("song-select-UI.png")) game.assetManager.unload("song-select-UI.png");
        if (loopTextures != null) loopTextures.clear();
        if (moveTextures != null) moveTextures.clear();
    }

    @Override
    public void dispose() {
        if (!assetsDisposed) manualDisposeHeavyAssets();
        if (previewMusic != null) previewMusic.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}