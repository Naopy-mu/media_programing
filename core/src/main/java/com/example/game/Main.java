package com.example.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.Iterator; 

public class Main extends ApplicationAdapter {
    SpriteBatch batch;
    ShapeRenderer shapeRenderer;
    Texture noteImg;
    BitmapFont font;
    Music music;
    Sound hitSound; 

    Array<Note> notes = new Array<>();
    Array<Particle> particles = new Array<>(); 
    Array<Ripple> ripples = new Array<>();     
    
    float songPosition = 0;
    String message = "";
    float messageTimer = 0;
    float score = 0;
    int combo = 0;
    float scorePerNote = 0;
    int gameState = 0; 

    // ★3D遠近感の設定
    final float VANISHING_POINT_Y = 550; 
    final float JUDGEMENT_LINE_Y = 50;   
    final float CAMERA_DEPTH = 1.0f;     
    
    final float NEAR_WIDTH_TOTAL = 600;  
    final float FAR_WIDTH_TOTAL = 20;    
    final float CENTER_X = 320;          
    
    // ★追加：3Dモード用のスクロール速度（ここをいじると速さが変わります）
    final float SCROLL_SPEED_3D = 5.0f; 

    class Particle {
        float x, y;
        float vx, vy;
        float life, maxLife;
        Color color;
        Particle(float x, float y) {
            this.x = x; this.y = y;
            double angle = Math.random() * Math.PI * 2;
            float speed = (float)(Math.random() * 150 + 100); 
            this.vx = (float)Math.cos(angle) * speed;
            this.vy = (float)Math.sin(angle) * speed;
            this.maxLife = (float)(Math.random() * 0.4 + 0.2); 
            this.life = this.maxLife;
            this.color = new Color(0.5f, 0.8f, 1f, 1f); 
        }
    }

    class Ripple {
        float x, y;
        float radius, maxRadius, life;
        Ripple(float x, float y, float width) {
            this.x = x; this.y = y;
            this.radius = 5; 
            this.maxRadius = width / 1.5f; 
            this.life = 0.4f; 
        }
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("libgdx.png");
        font = new BitmapFont();
        
        music = Gdx.audio.newMusic(Gdx.files.internal("Timepiece Tower.mp3"));
        music.setVolume(0.3f);
        music.setOnCompletionListener(music -> { gameState = 2; });

        try { hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.mp3")); } catch (Exception e) {}
    }

    void startGame() {
        gameState = 1; score = 0; combo = 0; message = ""; songPosition = 0;
        particles.clear(); ripples.clear(); 
        try {
            notes = ChartLoader.loadChart("chart.csv");
            if (notes.size > 0) scorePerNote = 1000000f / notes.size;
        } catch (Exception e) { notes = new Array<>(); }
        music.stop(); music.play();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        if (gameState == 0) drawTitle();
        else if (gameState == 1) updateAndDrawGame();
        else if (gameState == 2) drawResult();
    }

    float getScale(float zDistance) {
        return CAMERA_DEPTH / (CAMERA_DEPTH + zDistance);
    }

    float getScreenY(float scale) {
        return VANISHING_POINT_Y - (VANISHING_POINT_Y - JUDGEMENT_LINE_Y) * scale;
    }
    
    float getLaneWidth(float scale) {
        return NEAR_WIDTH_TOTAL * scale / 4.0f; 
    }

    float getLaneCenterX(int lane, float scale) {
        float totalW = NEAR_WIDTH_TOTAL * scale;
        float startX = CENTER_X - (totalW / 2.0f);
        float oneLaneW = totalW / 4.0f;
        return startX + (oneLaneW * lane) + (oneLaneW / 2.0f);
    }

    void drawTitle() {
        batch.begin();
        font.getData().setScale(3.0f);
        font.setColor(Color.CYAN);
        font.draw(batch, "RHYTHM GAME", 150, 350);
        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Press SPACE to Start", 180, 200);
        batch.end();
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (hitSound != null) hitSound.play();
            startGame();
        }
    }

    void updateAndDrawGame() {
        songPosition = music.getPosition();

        // 1. 入力
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) checkHit(i);
        }

        // 2. MISS判定
        Iterator<Note> iter = notes.iterator();
        while (iter.hasNext()) {
            Note note = iter.next();
            if (note.active && songPosition > note.targetTime + 0.2f) {
                note.active = false; message = "MISS..."; messageTimer = 1.0f; combo = 0; 
            }
        }

        // 3. 描画
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[i])) {
                shapeRenderer.setColor(1, 1, 0, 0.3f);
            } else {
                shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.5f);
            }

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

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE); 
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Iterator<Ripple> rIter = ripples.iterator();
        while (rIter.hasNext()) {
            Ripple r = rIter.next();
            r.radius += (r.maxRadius - r.radius) * 8.0f * Gdx.graphics.getDeltaTime();
            r.life -= Gdx.graphics.getDeltaTime();
            if (r.life <= 0) rIter.remove();
            else {
                shapeRenderer.setColor(0.2f, 1.0f, 1.0f, r.life * 2.5f);
                shapeRenderer.circle(r.x, r.y, r.radius); 
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Iterator<Particle> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle p = pIter.next();
            p.x += p.vx * Gdx.graphics.getDeltaTime();
            p.y += p.vy * Gdx.graphics.getDeltaTime();
            p.life -= Gdx.graphics.getDeltaTime();
            if (p.life <= 0) pIter.remove();
            else {
                float size = (p.life / p.maxLife) * 10.0f;
                shapeRenderer.setColor(0.7f, 0.9f, 1.0f, p.life);
                shapeRenderer.rect(p.x - size/2, p.y - size/2, size, size);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


        // (C) ノーツ描画
        batch.begin();
        font.getData().setScale(2.0f);
        font.setColor(Color.WHITE);

        for (Note note : notes) {
            if (note.active) {
                float timeRemains = note.targetTime - songPosition;
                
                // ★修正：GameConfig.NOTE_SPEED ではなく 3D用の速度定数(5.0f)を使う
                float zDistance = timeRemains * SCROLL_SPEED_3D;

                if (zDistance < -0.2f || zDistance > 10.0f) continue;

                float scale = getScale(zDistance);
                float drawY = getScreenY(scale);
                float drawW = getLaneWidth(scale);
                float drawX = getLaneCenterX(note.lane, scale);
                float drawH = 64f * scale; 

                batch.draw(noteImg, drawX - drawW/2 + 2, drawY, drawW - 4, drawH);
            }
        }

        if (messageTimer > 0) {
            font.draw(batch, message, 100, 300);
            messageTimer -= Gdx.graphics.getDeltaTime();
        }
        
        font.draw(batch, "Time: " + String.format("%.2f", songPosition), 10, 470);
        font.draw(batch, "Score: " + (int)score, 10, 440);
        font.draw(batch, "Combo: " + combo, 10, 410);
        batch.end();
    }

    void drawResult() {
        batch.begin();
        font.getData().setScale(4.0f);
        font.setColor(Color.YELLOW);
        font.draw(batch, "GAME CLEAR!!", 100, 400);
        font.getData().setScale(3.0f);
        font.setColor(Color.WHITE);
        font.draw(batch, "SCORE: " + (int)score, 150, 300);
        String rank = "C";
        if (score >= 900000) rank = "S";
        else if (score >= 800000) rank = "A";
        else if (score >= 700000) rank = "B";
        font.draw(batch, "RANK: " + rank, 200, 200);
        font.getData().setScale(1.5f);
        font.draw(batch, "Press SPACE to Title", 180, 100);
        batch.end();
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) gameState = 0; 
    }

    void spawnEffects(float x, float y, float width) {
        ripples.add(new Ripple(x, y, width));
        for (int i = 0; i < 20; i++) particles.add(new Particle(x, y));
    }

    void checkHit(int lane) {
        for (Note note : notes) {
            if (note.lane != lane || !note.active) continue;
            float timeDiff = Math.abs(note.targetTime - songPosition);
            if (timeDiff < 0.2f) {
                message = "PERFECT!!"; messageTimer = 1.0f; 
                note.active = false; score += scorePerNote; combo++; 
                if (hitSound != null) hitSound.play();
                
                float scale = getScale(0);
                float hitX = getLaneCenterX(lane, scale);
                float hitW = getLaneWidth(scale);
                spawnEffects(hitX, JUDGEMENT_LINE_Y, hitW);
                return; 
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose(); shapeRenderer.dispose(); noteImg.dispose(); font.dispose(); music.dispose();
        if (hitSound != null) hitSound.dispose();
    }
}