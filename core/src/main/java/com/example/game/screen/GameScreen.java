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
import com.example.game.BpmEvent;
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
    final float LANE_BOTTOM_Y = 50f;
    final float CAMERA_DEPTH = 1.0f;
    final float NEAR_WIDTH_TOTAL = 1500;
    final float CENTER_X = 1920 / 2f;
    
    float scrollSpeed; 
    float userOffset; 

    boolean isPlaying = false; 
    float countdownTimer = 0;  
    int countIndex = 0;        
    float initialBpm = 120; 
    float beatDuration;     
    float countInterval;
    float introDuration = 0;

    ObjectMap<String, Float> bpmMap = new ObjectMap<>();

    boolean isPaused = false;
    String[] pauseItems = {"RESUME", "RESTART", "QUIT"};
    int pauseIndex = 0;

    private ShapeRenderer fadeRenderer;
    private float fadeInAlpha = 1.0f;
    private boolean isFadingIn = true;
    final float FADE_SPEED = 0.8f;

    private boolean isQuitting = false;

    boolean isResuming = false;
    float resumeTimer = 0;

    public GameScreen(Main game, String songName) {
        this.game = game;
        this.songName = songName;

        fadeRenderer = new ShapeRenderer();
        fadeInAlpha = 1.0f;
        isFadingIn = true;
        
        this.scrollSpeed = GameConfig.getScrollSpeed();

        bpmMap.put("Link Layer", 156f);
        bpmMap.put("Pop!Stack!", 160f);
        bpmMap.put("Timepiece Tower", 140f);
        bpmMap.put("Eigenstate", 174f);

        this.initialBpm = bpmMap.get(songName, 140f);
        
        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("notes-UI.png");
        
        noteManager = new NoteManager(songName, initialBpm);
        this.userOffset = GameConfig.getOffset() + noteManager.offset;

        judgeSystem = new JudgeSystem(noteManager.getMaxCombo());
        
        effectManager = new EffectManager();

        this.beatDuration = 60f / initialBpm;
        this.countInterval = this.beatDuration; 
        
        this.introDuration = (4 * countInterval) + (2 * beatDuration);

        music = Gdx.audio.newMusic(Gdx.files.internal(songName + ".mp3"));
        music.setVolume(0.3f);
        
        music.setOnCompletionListener(m -> {
            forceFinishGame();
        });
        
        try { hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.mp3")); } catch (Exception e) {}
        try { countSound = Gdx.audio.newSound(Gdx.files.internal("count.mp3")); } catch (Exception e) {}

        isPlaying = false;
        countdownTimer = -0.5f;
        countIndex = 0;
        isQuitting = false;
        
        songPosition = -introDuration;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            forceFinishGame();
            return;
        }

        if (isPaused) {
            handlePauseInput();
        } else if (isResuming) {
            updateResumeCountdown(delta);
        } else {
            if (!isPlaying) {
                updateCountdown(delta);
                songPosition = Math.max(-introDuration, countdownTimer - introDuration);
            } else {
                songPosition = music.getPosition();
            }

            judgeSystem.update(delta);
            effectManager.update(delta);
            
            if (isPlaying || songPosition > -0.5f) {
                noteManager.checkMiss(songPosition - userOffset, judgeSystem);
                updateHolds(delta);

                for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
                    if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                        processHit(i);
                    }
                }
            }
            
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                pauseGame();
            }
        }

        // --- 描画処理 ---
        drawLanes();
        drawBeatLines(); // ★追加: 拍線の描画 (ノーツより奥、レーンより手前)
        drawEffects();    
        drawHoldBodies(); 
        drawSyncLines();  
        drawNotes();      
        drawUI();         

        if (isPaused) {
            drawPauseMenu();
        }

        if (isFadingIn) {
            fadeInAlpha -= delta * FADE_SPEED;
            if (fadeInAlpha <= 0f) {
                fadeInAlpha = 0f;
                isFadingIn = false;
            }
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            fadeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            fadeRenderer.setColor(1f, 1f, 1f, fadeInAlpha);
            fadeRenderer.rect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
            fadeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    private void forceFinishGame() {
        if (music != null) {
            music.setOnCompletionListener(null);
            music.stop();
        }
        
        game.setScreen(new ResultScreen(
            game, 
            (int)judgeSystem.score, 
            judgeSystem.maxCombo, 
            judgeSystem.perfectCount, 
            judgeSystem.greatCount, 
            judgeSystem.goodCount, 
            judgeSystem.missCount
        ));
        dispose();
    }

    // ★追加: 拍線を描画するメソッド
    void drawBeatLines() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        // 薄いグレーの線 (RGB: 0.7, Alpha: 0.3)
        shapeRenderer.setColor(0.7f, 0.7f, 0.7f, 0.3f); 

        float currentTime = songPosition - userOffset;
        final float MAX_DRAW_Z = 10.0f;
        // 画面奥（Z=10.0）までの時間幅を計算
        float visibleTimeRange = MAX_DRAW_Z / scrollSpeed;
        float maxVisibleTime = currentTime + visibleTimeRange;

        // BPMイベントごとに区切って拍を計算
        for (int i = 0; i < noteManager.bpmEvents.size; i++) {
            BpmEvent e = noteManager.bpmEvents.get(i);
            float nextEventTime = (i + 1 < noteManager.bpmEvents.size) 
                                  ? noteManager.bpmEvents.get(i+1).time 
                                  : Float.MAX_VALUE;

            // 画面範囲外のBPMイベントはスキップ
            if (e.time > maxVisibleTime) break;
            if (nextEventTime < currentTime) continue;

            float beatDuration = 60f / e.bpm;
            float t = e.time;

            // 現在時刻より前の拍はループで飛ばす（描画負荷軽減）
            // "currentTime" の少し前から計算しないと、手前のラインが消えるので -beatDuration
            if (t < currentTime - beatDuration) {
                float diff = (currentTime - beatDuration) - t;
                long skippedBeats = (long)(diff / beatDuration);
                t += skippedBeats * beatDuration;
            }

            // 次のBPM変更点まで、または画面奥までループ
            while (t < nextEventTime && t <= maxVisibleTime + beatDuration) {
                float zDistance = (t - currentTime) * scrollSpeed;

                // 描画範囲内なら線を引く
                // 手前すぎず(-0.5)、奥すぎない(MAX_DRAW_Z)
                if (zDistance > -0.5f && zDistance <= MAX_DRAW_Z) {
                    float scale = getScale(zDistance);
                    float y = getScreenY(scale);
                    
                    // レーンの左端から右端まで線を引く
                    float x1 = getLaneCenterX(0, scale) - getLaneWidth(scale)/2;
                    float x2 = getLaneCenterX(GameConfig.LANE_COUNT-1, scale) + getLaneWidth(scale)/2;
                    
                    shapeRenderer.line(x1, y, x2, y);
                }
                t += beatDuration;
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void updateCountdown(float delta) {
        countdownTimer += delta;
        if (countdownTimer < 0) return;

        if (countIndex < 4) {
            if (countdownTimer >= countIndex * countInterval) {
                if (countSound != null) countSound.play();
                countIndex++;
            }
        } else {
            if (countdownTimer >= introDuration) {
                music.play();
                isPlaying = true;
            }
        }
    }

    void updateResumeCountdown(float delta) {
        int prevCeil = (int)Math.ceil(resumeTimer);
        resumeTimer -= delta;
        int currentCeil = (int)Math.ceil(resumeTimer);

        if (currentCeil < prevCeil && currentCeil > 0) {
            if (countSound != null) countSound.play();
        }

        if (resumeTimer <= 0) {
            isResuming = false;
            if (isPlaying) {
                music.play();
            }
        }
    }

    void updateHolds(float delta) {
        float currentDisplayTime = songPosition - userOffset;

        for (Note note : noteManager.notes) {
            if (!note.isHold || !note.active) continue;

            if (note.isHolding) {
                if (currentDisplayTime >= note.endTime) {
                    note.isHolding = false;
                    note.active = false;
                    judgeSystem.finishHold(); 
                } else {
                    if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[note.lane])) {
                        note.holdTimer += delta;
                        float currentBpm = noteManager.getBpmAt(currentDisplayTime);
                        float currentBeatDuration = 60f / currentBpm;

                        if (note.holdTimer >= currentBeatDuration) {
                            judgeSystem.addHoldCombo();
                            note.holdTimer -= currentBeatDuration; 
                        }

                        float scale = getScale(0);
                        if (Math.random() < 0.3) {
                            effectManager.spawn(getLaneCenterX(note.lane, scale), JUDGEMENT_LINE_Y, getLaneWidth(scale), Color.CYAN);
                        }
                    } else {
                        note.isHolding = false;
                        note.holdTimer = 0f;
                        judgeSystem.resetCombo();
                    }
                }
            }
        }
    }

    void processHit(int lane) {
        for (Note note : noteManager.notes) {
            if (note.lane != lane || !note.active) continue;
            if (note.isHold && note.isHolding) continue;

            Color resultColor = judgeSystem.checkHit(note.targetTime + userOffset, songPosition);
            if (resultColor != null) {
                if (hitSound != null) hitSound.play();
                float scale = getScale(0);
                effectManager.spawn(getLaneCenterX(lane, scale), JUDGEMENT_LINE_Y, getLaneWidth(scale), resultColor);

                if (note.isHold) {
                    note.isHolding = true;
                    note.holdTimer = 0f; 
                } else {
                    note.active = false;
                }
                return;
            }
        }
    }

    void drawHoldBodies() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float currentDisplayTime = songPosition - userOffset;
        final float MAX_DRAW_Z = 10.0f; 

        for (Note note : noteManager.notes) {
            if (note.active && note.isHold) {
                float startZ;
                if (note.isHolding) startZ = 0;
                else startZ = (note.targetTime - currentDisplayTime) * scrollSpeed;

                float endZ = (note.endTime - currentDisplayTime) * scrollSpeed;

                if (endZ < 0 || startZ > MAX_DRAW_Z) continue;
                if (startZ < 0) startZ = 0;
                if (endZ > MAX_DRAW_Z) endZ = MAX_DRAW_Z;

                float scaleStart = getScale(startZ);
                float scaleEnd = getScale(endZ);

                float x1 = getLaneCenterX(note.lane, scaleStart) - getLaneWidth(scaleStart)/2;
                float x2 = getLaneCenterX(note.lane, scaleStart) + getLaneWidth(scaleStart)/2;
                float y1 = getScreenY(scaleStart);

                float x3 = getLaneCenterX(note.lane, scaleEnd) + getLaneWidth(scaleEnd)/2;
                float x4 = getLaneCenterX(note.lane, scaleEnd) - getLaneWidth(scaleEnd)/2;
                float y2 = getScreenY(scaleEnd);   
                
                if (note.isHolding) shapeRenderer.setColor(0f, 1f, 1f, 0.6f);
                else shapeRenderer.setColor(0f, 0.5f, 0.5f, 0.4f);

                shapeRenderer.triangle(x1, y1, x2, y1, x3, y2);
                shapeRenderer.triangle(x1, y1, x3, y2, x4, y2);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void drawSyncLines() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, 0.5f);

        float currentDisplayTime = songPosition - userOffset;
        float maxDrawZ = 10.0f;

        for (int i = 0; i < noteManager.notes.size - 1; i++) {
            Note currentNote = noteManager.notes.get(i);
            Note nextNote = noteManager.notes.get(i + 1);

            if (!currentNote.active || !nextNote.active) continue;

            if (Math.abs(currentNote.targetTime - nextNote.targetTime) < 0.001f) {
                float timeRemains = currentNote.targetTime - currentDisplayTime;
                float zDistance = timeRemains * scrollSpeed;

                if (zDistance < 0 || zDistance > maxDrawZ) continue;

                float scale = getScale(zDistance);
                float drawY = getScreenY(scale);
                float lineHeight = 5.0f * scale; 

                float x1 = getLaneCenterX(currentNote.lane, scale);
                float x2 = getLaneCenterX(nextNote.lane, scale);

                shapeRenderer.rectLine(x1, drawY, x2, drawY, lineHeight);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void drawNotes() {
        game.batch.begin();
        game.batch.setColor(Color.WHITE);

        for (Note note : noteManager.notes) {
            if (note.active) {
                if (note.isHolding) continue;

                float timeRemains = (note.targetTime + userOffset) - songPosition;
                float zDistance = timeRemains * scrollSpeed;
                
                if (zDistance < -0.2f || zDistance > 10.0f) continue;

                float scale = getScale(zDistance);
                float drawY = getScreenY(scale);
                float drawH = 40f * scale; 

                if (drawY + drawH < LANE_BOTTOM_Y) continue;

                float laneWidth = getLaneWidth(scale);
                float drawW = laneWidth * 1.0f;
                float drawX = getLaneCenterX(note.lane, scale);

                game.batch.draw(noteImg, drawX - drawW/2, drawY, drawW, drawH);
            }
        }
        game.batch.end();
    }

    void drawUI() {
        game.batch.begin();
        
        if (!isPlaying && !isPaused && !isResuming) { 
            game.font.setColor(Color.YELLOW);
            game.font.getData().setScale(3.0f);
            if (countdownTimer >= 0) {
                if (countIndex < 4) game.font.draw(game.batch, "READY...", CENTER_X - 100, 600);
                else game.font.draw(game.batch, "GO!", CENTER_X - 50, 600);
            }
        }

        if (isResuming) {
            game.font.setColor(Color.CYAN);
            game.font.getData().setScale(5.0f); 
            int count = (int)Math.ceil(resumeTimer);
            if (count > 0) {
                game.font.draw(game.batch, String.valueOf(count), CENTER_X - 20, 600);
            }
        }

        if (judgeSystem.messageTimer > 0) {
            float maxTime = judgeSystem.message.startsWith("MISS") ? 1.0f : 0.5f;
            float progress = judgeSystem.messageTimer / maxTime;
            float baseScale = 2.5f;
            float animScale = baseScale + (progress * 0.5f); 
            float alpha = 1.0f;
            if (progress < 0.3f) alpha = progress / 0.3f;

            Color c = judgeSystem.messageColor;
            game.font.setColor(c.r, c.g, c.b, alpha);
            
            game.font.getData().setScale(animScale);
            game.font.draw(game.batch, judgeSystem.message, CENTER_X - (50 * animScale), 450 + (progress * 20));

            if (!judgeSystem.timingMessage.isEmpty()) {
                if (judgeSystem.timingMessage.equals("FAST")) game.font.setColor(1, 0, 0, alpha);
                else game.font.setColor(0, 0, 1, alpha);
                game.font.getData().setScale(1.5f);
                game.font.draw(game.batch, judgeSystem.timingMessage, CENTER_X - 40, 400);
            }
        }

        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(2.0f);
        game.font.draw(game.batch, "Music: " + songName, 20, 1050);
        
        float currentBpm = noteManager.getBpmAt(songPosition - userOffset);
        game.font.draw(game.batch, "BPM: " + (int)currentBpm, 20, 1010); 
        
        game.font.draw(game.batch, "Speed: " + String.format("%.1f", scrollSpeed), 20, 970);
        game.font.draw(game.batch, "Score: " + (int)judgeSystem.score, 20, 930);

        if (judgeSystem.combo > 0) {
            switch (judgeSystem.comboStatus) {
                case 0: game.font.setColor(Color.GOLD); break; 
                case 1: game.font.setColor(Color.CYAN); break; 
                default: game.font.setColor(Color.WHITE); break; 
            }
            
            game.font.getData().setScale(3.0f); 
            String comboText = String.valueOf(judgeSystem.combo);
            float textWidth = comboText.length() * 40f; 
            game.font.draw(game.batch, comboText, CENTER_X - (textWidth / 2), 600);
        }
        
        game.font.setColor(Color.GRAY);
        game.font.getData().setScale(1.5f);
        game.font.draw(game.batch, "[F1] FORCE FINISH", GameConfig.SCREEN_WIDTH - 300, 50);
        
        game.batch.end();
    }

    void pauseGame() {
        isPaused = true;
        if (music.isPlaying()) music.pause(); 
        pauseIndex = 0; 
    }

    void resumeGame() {
        isPaused = false;
        isResuming = true;
        resumeTimer = 3.0f;
    }

    void restartGame() {
        music.setOnCompletionListener(null);
        music.stop(); 
        
        music.setOnCompletionListener(m -> {
            forceFinishGame();
        });
        isPaused = false;
        isPlaying = false;
        countdownTimer = -0.5f; 
        countIndex = 0;
        songPosition = 0;
        isQuitting = false;
        
        noteManager = new NoteManager(songName, initialBpm);
        this.userOffset = GameConfig.getOffset() + noteManager.offset;
        judgeSystem = new JudgeSystem(noteManager.getMaxCombo());
        
        effectManager = new EffectManager();
        
        songPosition = -introDuration;
    }

    private void returnToSongSelect() {
        if (isQuitting) return;
        isQuitting = true;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                dispose(); 
                System.gc();
                game.setScreen(new SongSelectScreen(game));
            }
        });
    }

    void handlePauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            pauseIndex--;
            if (pauseIndex < 0) pauseIndex = pauseItems.length - 1;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            pauseIndex++;
            if (pauseIndex >= pauseItems.length) pauseIndex = 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            switch (pauseIndex) {
                case 0: resumeGame(); break;
                case 1: restartGame(); break;
                case 2: returnToSongSelect(); break;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) resumeGame();
    }

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

    void drawLanes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float laneDrawBottomY = LANE_BOTTOM_Y;
        float scaleStart = (VANISHING_POINT_Y - laneDrawBottomY) / (VANISHING_POINT_Y - JUDGEMENT_LINE_Y);
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[i])) shapeRenderer.setColor(1, 1, 0, 0.3f);
            else shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.5f);
            float x1 = getLaneCenterX(i, scaleStart) - getLaneWidth(scaleStart)/2;
            float x2 = getLaneCenterX(i, scaleStart) + getLaneWidth(scaleStart)/2;
            float scaleFar = getScale(10.0f);
            float x3 = getLaneCenterX(i, scaleFar) + getLaneWidth(scaleFar)/2;
            float x4 = getLaneCenterX(i, scaleFar) - getLaneWidth(scaleFar)/2;
            float y2 = getScreenY(scaleFar);
            shapeRenderer.triangle(x1, laneDrawBottomY, x2, laneDrawBottomY, x3, y2);
            shapeRenderer.triangle(x1, laneDrawBottomY, x3, y2, x4, y2);
        }
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.CYAN); 
        float lineHeight = 4.0f;
        float lineY = JUDGEMENT_LINE_Y - lineHeight/2;
        float scaleJust = getScale(0); 
        float leftX = getLaneCenterX(0, scaleJust) - getLaneWidth(scaleJust)/2;
        float rightX = getLaneCenterX(GameConfig.LANE_COUNT-1, scaleJust) + getLaneWidth(scaleJust)/2;
        shapeRenderer.rect(leftX, lineY, rightX - leftX, lineHeight);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            float xNear = (CENTER_X - (NEAR_WIDTH_TOTAL * scaleStart)/2) + ((NEAR_WIDTH_TOTAL * scaleStart)/4)*i;
            float scaleFar = getScale(10.0f); 
            float totalWFar = NEAR_WIDTH_TOTAL * scaleFar;
            float xFar = (CENTER_X - totalWFar/2) + (totalWFar/4)*i;
            float yFar = getScreenY(scaleFar);
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
        try {
            if (shapeRenderer != null) shapeRenderer.dispose();
            if (noteImg != null) noteImg.dispose();
        } catch (Exception e) { }
        try {
            if (hitSound != null) hitSound.dispose();
            if (countSound != null) countSound.dispose();
        } catch (Exception e) { }
        try {
            if (music != null) {
                music.setOnCompletionListener(null);
                music.stop(); 
                music.dispose();
                music = null;
            }
        } catch (Exception e) { }
        if (fadeRenderer != null) fadeRenderer.dispose();
    }
}