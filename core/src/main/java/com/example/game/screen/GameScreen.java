package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.example.game.GameConfig;
import com.example.game.Main;
import com.example.game.Note;
import com.example.game.logic.EffectManager;
import com.example.game.logic.JudgeSystem;
import com.example.game.logic.NoteManager;

import java.util.Iterator;

public class GameScreen extends ScreenAdapter {
    final Main game;
    
    // ロジッククラス
    NoteManager noteManager;
    JudgeSystem judgeSystem;
    EffectManager effectManager;

    // リソース
    ShapeRenderer shapeRenderer;
    Texture noteImg;
    Music music;
    Sound hitSound;
    
    float songPosition = 0;

    // 3D設定
    final float VANISHING_POINT_Y = 1000;
    final float JUDGEMENT_LINE_Y = 50;
    final float CAMERA_DEPTH = 1.0f;
    final float NEAR_WIDTH_TOTAL = 1500;
    final float CENTER_X = 1920 / 2f;
    final float SCROLL_SPEED_3D = 5.0f;

    public GameScreen(Main game) {
        this.game = game;
        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("libgdx.png");
        
        // ロジックの生成
        noteManager = new NoteManager();
        judgeSystem = new JudgeSystem(noteManager.getTotalNotes());
        effectManager = new EffectManager();

        // 音楽ロード
        music = Gdx.audio.newMusic(Gdx.files.internal("Timepiece Tower.mp3"));
        music.setVolume(0.3f);
        music.setOnCompletionListener(m -> {
            // 曲が終わったらリザルトへ
            game.setScreen(new ResultScreen(game, (int)judgeSystem.score, judgeSystem.getRank()));
            dispose();
        });
        
        try { hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.mp3")); } catch (Exception e) {}

        music.play();
    }

    @Override
    public void render(float delta) {
        // --- 1. 更新処理 (Update) ---
        songPosition = music.getPosition();
        judgeSystem.update(delta);
        effectManager.update(delta);
        noteManager.checkMiss(songPosition, judgeSystem);

        // キー入力判定
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                processHit(i);
            }
        }

        // --- 2. 描画処理 (Draw) ---
        ScreenUtils.clear(0, 0, 0, 1);
        
        // (A) レーン描画
        drawLanes();

        // (B) エフェクト描画
        drawEffects();

        // (C) ノーツ描画
        drawNotes();

        // (D) UI描画
        drawUI();
    }
    
    void processHit(int lane) {
        for (Note note : noteManager.notes) {
            if (note.lane != lane || !note.active) continue;
            
            Color resultColor = judgeSystem.checkHit(note.targetTime, songPosition);
            if (resultColor != null) {
                // ヒット成功
                note.active = false;
                if (hitSound != null) hitSound.play();
                
                // エフェクト発生
                float scale = getScale(0);
                effectManager.spawn(getLaneCenterX(lane, scale), JUDGEMENT_LINE_Y, getLaneWidth(scale), resultColor);
                return;
            }
        }
    }

    // --- 描画ヘルパーメソッド ---

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
        
        // 判定メッセージ
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

        // スコア表示
        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(2.0f);
        game.font.draw(game.batch, "Time: " + String.format("%.2f", songPosition), 20, 1050);
        game.font.draw(game.batch, "Score: " + (int)judgeSystem.score, 20, 1010);
        game.font.draw(game.batch, "Combo: " + judgeSystem.combo, 20, 970);
        
        game.batch.end();
    }

    // 3D計算ヘルパー
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
    }
}