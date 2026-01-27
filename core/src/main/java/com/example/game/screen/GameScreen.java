package com.example.game.screen;

import com.badlogic.gdx.Gdx;
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
    final float JUDGEMENT_LINE_Y = 50;
    final float CAMERA_DEPTH = 1.0f;
    final float NEAR_WIDTH_TOTAL = 1500;
    final float CENTER_X = 1920 / 2f;
    final float SCROLL_SPEED_3D = 5.0f;

    // カウントダウン用変数
    boolean isPlaying = false; 
    float countdownTimer = 0;  
    int countIndex = 0;        
    float bpm = 120;           
    float beatDuration;        
    float countInterval;       

    // 曲ごとのBPM設定マップ
    ObjectMap<String, Float> bpmMap = new ObjectMap<>();

    public GameScreen(Main game, String songName) {
        this.game = game;
        this.songName = songName;
        
        bpmMap.put("Link Layer", 156f);
        bpmMap.put("Pop!Stack!", 160f);
        bpmMap.put("Timepiece Tower", 140f);
        bpmMap.put("Eigenstate", 174f);

        this.bpm = bpmMap.get(songName, 140f);
        
        // 1拍の時間
        this.beatDuration = 60f / this.bpm;
        // カウント間隔
        this.countInterval = this.beatDuration; 

        System.out.println("Now Playing: " + songName + " (BPM: " + bpm + ")");

        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("libgdx.png");
        
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

        // ★修正：カウントダウン開始前に0.5秒の「溜め」を作る
        // これにより、画面遷移直後の誤入力（hit音誤爆）を防ぎます
        isPlaying = false;
        countdownTimer = -0.5f; // マイナスからスタート
        countIndex = 0;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        // --- 更新処理 ---
        if (!isPlaying) {
            // カウントダウン中の処理
            updateCountdown(delta);
        } else {
            // ゲーム中の処理
            songPosition = music.getPosition();
            judgeSystem.update(delta);
            effectManager.update(delta);
            noteManager.checkMiss(songPosition, judgeSystem);

            for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
                if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                    processHit(i);
                }
            }
        }

        // --- 描画処理 ---
        drawLanes();
        drawEffects();
        
        if (isPlaying) {
            drawNotes();
        }

        drawUI();
    }

    // カウントダウンのロジック
    void updateCountdown(float delta) {
        countdownTimer += delta;

        // ★修正：タイマーがマイナスの間（最初の0.5秒）は何もしない
        if (countdownTimer < 0) return;

        // 指定間隔ごとに音を鳴らす (0回目, 1回目, 2回目, 3回目)
        if (countIndex < 4) {
            if (countdownTimer >= countIndex * countInterval) {
                if (countSound != null) countSound.play();
                countIndex++;
            }
        } 
        else {
            // 4回鳴らし終わった後の待機処理
            // 「4回カウント(4 * interval)」 + 「2拍待つ(2 * beatDuration)」経過したら開始
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
            Color resultColor = judgeSystem.checkHit(note.targetTime, songPosition);
            if (resultColor != null) {
                note.active = false;
                if (hitSound != null) hitSound.play();
                float scale = getScale(0);
                effectManager.spawn(getLaneCenterX(lane, scale), JUDGEMENT_LINE_Y, getLaneWidth(scale), resultColor);
                return;
            }
        }
    }

    // --- 描画系（変更なし） ---
    void drawLanes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[i])) shapeRenderer.setColor(1, 1, 0, 0.3f);
            else shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.5f);

            float scaleNear = getScale(0);
            float x1 = getLaneCenterX(i, scaleNear) - getLaneWidth(scaleNear)/2;
            float x2 = getLaneCenterX(i, scaleNear) + getLaneWidth(scaleNear)/2;
            float y1 = getScreenY(scaleNear);
            float scaleFar = getScale(10.0f);
            float x3 = getLaneCenterX(i, scaleFar) + getLaneWidth(scaleFar)/2;
            float x4 = getLaneCenterX(i, scaleFar) - getLaneWidth(scaleFar)/2;
            float y2 = getScreenY(scaleFar);
            shapeRenderer.triangle(x1, 0, x2, 0, x3, y2);
            shapeRenderer.triangle(x1, 0, x3, y2, x4, y2);
        }
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.line(0, JUDGEMENT_LINE_Y, GameConfig.SCREEN_WIDTH, JUDGEMENT_LINE_Y); 
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            float scaleNear = getScale(0);
            float scaleFar = getScale(10.0f); 
            float xNear = (CENTER_X - NEAR_WIDTH_TOTAL/2) + (NEAR_WIDTH_TOTAL/4)*i;
            float totalWFar = NEAR_WIDTH_TOTAL * scaleFar;
            float xFar = (CENTER_X - totalWFar/2) + (totalWFar/4)*i;
            float yFar = getScreenY(scaleFar);
            shapeRenderer.line(xNear, 0, xFar, yFar);
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
                float timeRemains = note.targetTime - songPosition;
                float zDistance = timeRemains * SCROLL_SPEED_3D;
                if (zDistance < -0.2f || zDistance > 10.0f) continue;

                float scale = getScale(zDistance);
                float drawY = getScreenY(scale);
                float drawW = getLaneWidth(scale);
                float drawX = getLaneCenterX(note.lane, scale);
                float drawH = 64f * scale;
                game.batch.draw(noteImg, drawX - drawW/2 + 2, drawY, drawW - 4, drawH);
            }
        }
        game.batch.end();
    }

    void drawUI() {
        game.batch.begin();
        
        if (!isPlaying) {
            game.font.setColor(Color.YELLOW);
            game.font.getData().setScale(3.0f);
            
            // 0秒未満（待機中）は何も出さないか、Loading...などを出しても良い
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
        game.font.draw(game.batch, "Score: " + (int)judgeSystem.score, 20, 970);
        game.font.draw(game.batch, "Combo: " + judgeSystem.combo, 20, 930);
        
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
        shapeRenderer.dispose();
        noteImg.dispose();
        music.dispose();
        if (hitSound != null) hitSound.dispose();
        if (countSound != null) countSound.dispose(); 
    }
}