package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.example.game.GameConfig;
import com.example.game.Main;
import com.example.game.Note;
import com.example.game.logic.EffectManager;
import com.example.game.logic.JudgeSystem;
import com.example.game.logic.NoteManager;

public class GameScreen extends ScreenAdapter {
    final Main game;
    String songName; 
    
    NoteManager noteManager;
    JudgeSystem judgeSystem;
    EffectManager effectManager;

    ShapeRenderer shapeRenderer;
    Texture noteImg;
    Music music;
    Sound hitSound;
    Sound countSound; 

    float songPosition = 0;

    // 3D設定
    final float VANISHING_POINT_Y = 1000;
    final float JUDGEMENT_LINE_Y = 150;
    final float CAMERA_DEPTH = 1.0f;
    final float NEAR_WIDTH_TOTAL = 1500;
    final float CENTER_X = 1920 / 2f;
    
    float scrollSpeed; 
    float userOffset;

    // カウントダウン用
    boolean isPlaying = false; 
    float countdownTimer = 0;  
    int countIndex = 0;        
    float bpm = 120;           
    float beatDuration;        
    float countInterval;       

    ObjectMap<String, Float> bpmMap = new ObjectMap<>();

    // ポーズ機能用の変数
    boolean isPaused = false;
    String[] pauseItems = {"RESUME", "RESTART", "QUIT"};
    int pauseIndex = 0;

    public GameScreen(Main game, String songName) {
        this.game = game;
        this.songName = songName;
        
        this.scrollSpeed = GameConfig.getScrollSpeed();
        this.userOffset = GameConfig.getOffset();

        bpmMap.put("Link Layer", 156f);
        bpmMap.put("Pop!Stack!", 160f);
        bpmMap.put("Timepiece Tower", 140f);
        bpmMap.put("Eigenstate", 174f);

        this.bpm = bpmMap.get(songName, 140f);
        
        this.beatDuration = 60f / this.bpm;
        this.countInterval = this.beatDuration; 

        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("notes-UI.png");
        
        noteManager = new NoteManager(songName);
        judgeSystem = new JudgeSystem(noteManager.getTotalNotes());
        effectManager = new EffectManager();

        music = Gdx.audio.newMusic(Gdx.files.internal(songName + ".mp3"));
        music.setVolume(0.3f);
        music.setOnCompletionListener(m -> {
            game.setScreen(new ResultScreen(game, (int)judgeSystem.score, judgeSystem.getRank()));
            dispose();
        });
        
        try { hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.mp3")); } catch (Exception e) {}
        try { countSound = Gdx.audio.newSound(Gdx.files.internal("count.mp3")); } catch (Exception e) {}

        isPlaying = false;
        countdownTimer = -0.5f;
        countIndex = 0;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        // ポーズ中かどうかで処理を分岐
        if (isPaused) {
            handlePauseInput();
        } else {
            // ゲーム進行
            if (!isPlaying) {
                updateCountdown(delta);
            } else {
                songPosition = music.getPosition();
                judgeSystem.update(delta);
                effectManager.update(delta);
                noteManager.checkMiss(songPosition - userOffset, judgeSystem);

                for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
                    if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                        processHit(i);
                    }
                }
            }
            
            // ポーズボタン（ESC）の監視
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                pauseGame();
            }
        }

        // --- 描画処理 ---
        drawLanes();
        drawEffects();
        if (isPlaying || isPaused) { 
            drawNotes();
        }
        drawUI();

        if (isPaused) {
            drawPauseMenu();
        }
    }

    // ポーズ開始処理
    void pauseGame() {
        isPaused = true;
        if (music.isPlaying()) {
            music.pause(); 
        }
        pauseIndex = 0; 
    }

    // ポーズ解除（再開）処理
    void resumeGame() {
        isPaused = false;
        if (isPlaying) {
            music.play();
        }
    }

    // ★修正：安全なリスタート処理
    void restartGame() {
        // 1. リスナーを一旦解除（停止操作中の誤動作防止）
        music.setOnCompletionListener(null);

        // 2. 音楽を止める
        // ※ここで setPosition(0) を呼ぶとWindowsでクラッシュするため、stop()だけにする
        if (music.isPlaying()) {
            music.stop(); 
        } else {
            music.stop(); // 停止中でも念のため呼んで内部位置をリセットさせる
        }

        // 3. リスナーを再登録
        music.setOnCompletionListener(m -> {
            game.setScreen(new ResultScreen(game, (int)judgeSystem.score, judgeSystem.getRank()));
            dispose();
        });
        
        // 4. 進行状況のリセット
        isPaused = false;
        isPlaying = false;
        countdownTimer = -0.5f; 
        countIndex = 0;
        songPosition = 0;
        
        // 5. ゲームロジックの再生成
        noteManager = new NoteManager(songName);
        judgeSystem = new JudgeSystem(noteManager.getTotalNotes());
        effectManager = new EffectManager();
    }

    // ポーズメニューの入力処理
    void handlePauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            pauseIndex--;
            if (pauseIndex < 0) pauseIndex = pauseItems.length - 1;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            pauseIndex++;
            if (pauseIndex >= pauseItems.length) pauseIndex = 0;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            switch (pauseIndex) {
                case 0: // RESUME
                    resumeGame();
                    break;
                case 1: // RESTART
                    restartGame(); 
                    break;
                case 2: // QUIT
                    // ★修正：ここで music.stop() を書かない！
                    // dispose() の中で安全に止めるので、ここでは削除します。
                    
                    game.setScreen(new SongSelectScreen(game));
                    dispose();
                    break;
            }
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            resumeGame();
        }
    }

    // ポーズメニューの描画
    void drawPauseMenu() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f); 
        shapeRenderer.rect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.batch.begin();
        
        game.font.setColor(Color.CYAN);
        game.font.getData().setScale(4.0f);
        game.font.draw(game.batch, "PAUSED", CENTER_X - 150, 800);

        for (int i = 0; i < pauseItems.length; i++) {
            float y = 600 - (i * 120);
            if (i == pauseIndex) {
                game.font.setColor(Color.YELLOW);
                game.font.getData().setScale(2.5f);
                game.font.draw(game.batch, "> " + pauseItems[i] + " <", CENTER_X - 150, y);
            } else {
                game.font.setColor(Color.GRAY);
                game.font.getData().setScale(2.0f);
                game.font.draw(game.batch, pauseItems[i], CENTER_X - 100, y);
            }
        }
        game.batch.end();
    }

    // --- 以下、描画ヘルパー ---
    void updateCountdown(float delta) {
        countdownTimer += delta;
        if (countdownTimer < 0) return;

        if (countIndex < 4) {
            if (countdownTimer >= countIndex * countInterval) {
                if (countSound != null) countSound.play();
                countIndex++;
            }
        } 
        else {
            float startTime = (4 * countInterval) + (2 * beatDuration);
            if (countdownTimer >= startTime) {
                music.play();
                isPlaying = true;
            }
        }
    }
    
    void processHit(int lane) {
        for (Note note : noteManager.notes) {
            if (note.lane != lane || !note.active) continue;
            Color resultColor = judgeSystem.checkHit(note.targetTime + userOffset, songPosition);
            if (resultColor != null) {
                note.active = false;
                if (hitSound != null) hitSound.play();
                float scale = getScale(0);
                effectManager.spawn(getLaneCenterX(lane, scale), JUDGEMENT_LINE_Y, getLaneWidth(scale), resultColor);
                return;
            }
        }
    }

    void drawLanes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        // --- 1. レーンの背景 ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // ★調整：レーンの見た目上の「底」の高さ
        // 0 だと画面ピッタリ。20 くらいにすると「少し浮いている」感じになります。
        float laneDrawBottomY = 50f;

        // その高さに対応する「倍率（scale）」を逆算します
        // これにより、パースが狂わずに手前まで描画できます
        float scaleStart = (VANISHING_POINT_Y - laneDrawBottomY) / (VANISHING_POINT_Y - JUDGEMENT_LINE_Y);

        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[i])) shapeRenderer.setColor(1, 1, 0, 0.3f);
            else shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.5f);

            // 手前（laneDrawBottomY）の座標
            float x1 = getLaneCenterX(i, scaleStart) - getLaneWidth(scaleStart)/2;
            float x2 = getLaneCenterX(i, scaleStart) + getLaneWidth(scaleStart)/2;
            
            // 奥（消失点）の座標
            float scaleFar = getScale(10.0f);
            float x3 = getLaneCenterX(i, scaleFar) + getLaneWidth(scaleFar)/2;
            float x4 = getLaneCenterX(i, scaleFar) - getLaneWidth(scaleFar)/2;
            float y2 = getScreenY(scaleFar);
            
            // 三角形を描画（手前から奥へ）
            // Y座標は laneDrawBottomY (例:20) から始まります
            shapeRenderer.triangle(x1, laneDrawBottomY, x2, laneDrawBottomY, x3, y2);
            shapeRenderer.triangle(x1, laneDrawBottomY, x3, y2, x4, y2);
        }
        shapeRenderer.end();
        
        // --- 2. 判定ライン（光るバー） ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.CYAN); 
        
        float lineHeight = 4.0f; // 線の太さ
        float lineY = JUDGEMENT_LINE_Y - lineHeight/2; // 判定ライン(Y=50)を中心に

        // 左端〜右端を取得（判定ライン上の幅）
        float scaleJust = getScale(0); // z=0 (ジャストタイミングの場所)
        float leftX = getLaneCenterX(0, scaleJust) - getLaneWidth(scaleJust)/2;
        float rightX = getLaneCenterX(GameConfig.LANE_COUNT-1, scaleJust) + getLaneWidth(scaleJust)/2;
        
        shapeRenderer.rect(leftX, lineY, rightX - leftX, lineHeight);
        shapeRenderer.end();

        // --- 3. レーン区切り線 ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            // 手前（laneDrawBottomY の位置）
            float xNear = (CENTER_X - (NEAR_WIDTH_TOTAL * scaleStart)/2) + ((NEAR_WIDTH_TOTAL * scaleStart)/4)*i;
            
            // 奥
            float scaleFar = getScale(10.0f); 
            float totalWFar = NEAR_WIDTH_TOTAL * scaleFar;
            float xFar = (CENTER_X - totalWFar/2) + (totalWFar/4)*i;
            float yFar = getScreenY(scaleFar);
            
            // 線を引く（手前から奥へ）
            shapeRenderer.line(xNear, laneDrawBottomY, xFar, yFar);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void drawEffects() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (EffectManager.Ripple r : effectManager.ripples) {
            shapeRenderer.setColor(r.color.r, r.color.g, r.color.b, r.life * 2.5f);
            shapeRenderer.circle(r.x, r.y, r.radius);
        }
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (EffectManager.Particle p : effectManager.particles) {
            float size = (p.life / p.maxLife) * 10.0f;
            shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, p.life);
            shapeRenderer.rect(p.x - size/2, p.y - size/2, size, size);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void drawNotes() {
        game.batch.begin();
        for (Note note : noteManager.notes) {
            if (note.active) {
                float timeRemains = (note.targetTime + userOffset) - songPosition;
                float zDistance = timeRemains * scrollSpeed;
                
                if (zDistance < -0.2f || zDistance > 10.0f) continue;

                float scale = getScale(zDistance);
                float drawY = getScreenY(scale);
                
                // ★修正：レーンの本来の幅を取得
                float laneWidth = getLaneWidth(scale);

                // ★調整ポイント：描画する幅の倍率
                // 1.0f = レーン幅ぴったり
                // 0.9f = 少し隙間を空ける（隣とくっつかないようにする）
                // 1.1f = 画像に透明な余白がある場合、少し大きめに描画して合わせる
                float widthScale = 1.0f; 

                float drawW = laneWidth * widthScale;
                float drawX = getLaneCenterX(note.lane, scale);
                
                // 高さを少し太く調整（お好みで変えてください）
                float drawH = 50f * scale; 

                // ★修正：+2 や -4 などの固定値を削除し、純粋に中心に合わせて描画
                game.batch.draw(noteImg, drawX - drawW/2, drawY, drawW, drawH);
            }
        }
        game.batch.end();
    }

    void drawUI() {
        game.batch.begin();
        
        if (!isPlaying && !isPaused) { 
            game.font.setColor(Color.YELLOW);
            game.font.getData().setScale(3.0f);
            if (countdownTimer >= 0) {
                if (countIndex < 4) {
                    game.font.draw(game.batch, "READY...", CENTER_X - 100, 600);
                } else {
                    game.font.draw(game.batch, "GO!", CENTER_X - 50, 600);
                }
            }
        }

        if (judgeSystem.messageTimer > 0) {
            game.font.setColor(judgeSystem.messageColor);
            game.font.getData().setScale(2.5f);
            game.font.draw(game.batch, judgeSystem.message, CENTER_X - 80, 450);
            if (!judgeSystem.timingMessage.isEmpty()) {
                if (judgeSystem.timingMessage.equals("FAST")) game.font.setColor(Color.RED);
                else game.font.setColor(Color.BLUE);
                game.font.getData().setScale(1.5f);
                game.font.draw(game.batch, judgeSystem.timingMessage, CENTER_X - 40, 400);
            }
        }

        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(2.0f);
        game.font.draw(game.batch, "Music: " + songName, 20, 1050);
        game.font.draw(game.batch, "BPM: " + (int)bpm, 20, 1010); 
        game.font.draw(game.batch, "Speed: " + String.format("%.1f", scrollSpeed), 20, 970);
        
        game.font.draw(game.batch, "Score: " + (int)judgeSystem.score, 20, 930);
        game.font.draw(game.batch, "Combo: " + judgeSystem.combo, 20, 890);
        
        game.batch.end();
    }

    float getScale(float zDistance) { return CAMERA_DEPTH / (CAMERA_DEPTH + zDistance); }
    float getScreenY(float scale) { return VANISHING_POINT_Y - (VANISHING_POINT_Y - JUDGEMENT_LINE_Y) * scale; }
    float getLaneWidth(float scale) { return NEAR_WIDTH_TOTAL * scale / 4.0f; }
    float getLaneCenterX(int lane, float scale) {
        float totalW = NEAR_WIDTH_TOTAL * scale;
        float startX = CENTER_X - (totalW / 2.0f);
        float oneLaneW = totalW / 4.0f;
        return startX + (oneLaneW * lane) + (oneLaneW / 2.0f);
    }

    @Override
    public void dispose() {
        // 1. 画像などの破棄
        try {
            if (shapeRenderer != null) shapeRenderer.dispose();
            if (noteImg != null) noteImg.dispose();
        } catch (Exception e) { }

        // 2. 効果音の破棄
        try {
            if (hitSound != null) hitSound.dispose();
            if (countSound != null) countSound.dispose();
        } catch (Exception e) { }

        // 3. 音楽の破棄（一番重要）
        try {
            if (music != null) {
                music.setOnCompletionListener(null);
                
                // ★修正：再生中(isPlaying)じゃなくても、強制的にstopを呼んでバッファを解放させる
                music.stop(); 
                
                music.dispose();
                music = null; // 変数を空にしておく
            }
        } catch (Exception e) { }
    }
}
