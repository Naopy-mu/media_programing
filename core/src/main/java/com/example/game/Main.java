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

    // ★修正1：キラキラも少し範囲を小さくする
    class Particle {
        float x, y;
        float vx, vy;
        float life;
        float maxLife;
        Color color;

        Particle(float x, float y) {
            this.x = x;
            this.y = y;
            double angle = Math.random() * Math.PI * 2;
            
            // ★変更点：飛び散るスピードを抑えめに (300+200 -> 150+100)
            float speed = (float)(Math.random() * 150 + 100); 
            
            this.vx = (float)Math.cos(angle) * speed;
            this.vy = (float)Math.sin(angle) * speed;
            
            this.maxLife = (float)(Math.random() * 0.4 + 0.2); 
            this.life = this.maxLife;
            
            this.color = new Color(0.5f, 0.8f, 1f, 1f); 
        }
    }

    // ★修正2：波紋の最大サイズをノーツ幅に合わせる
    class Ripple {
        float x, y;
        float radius;     
        float maxRadius;  
        float life;       
        
        Ripple(float x, float y) {
            this.x = x;
            this.y = y;
            this.radius = 5; // 初期サイズも少し小さく
            
            // ★変更点：最大半径を「レーン幅の半分」にする
            // これで直径がちょうどレーン幅（ノーツの幅）と同じになります
            this.maxRadius = GameConfig.LANE_WIDTH / 2f; 
            
            this.life = 0.4f; // 消えるまでの時間も少し短くしてキレを出す
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

        try {
            hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.mp3"));
        } catch (Exception e) {}
    }

    void startGame() {
        gameState = 1; 
        score = 0;
        combo = 0;
        message = "";
        songPosition = 0;
        particles.clear(); 
        ripples.clear(); 
        
        try {
            notes = ChartLoader.loadChart("chart.csv");
            if (notes.size > 0) {
                scorePerNote = 1000000f / notes.size;
            }
        } catch (Exception e) {
            notes = new Array<>();
        }

        music.stop();
        music.play();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);

        if (gameState == 0) drawTitle();
        else if (gameState == 1) updateAndDrawGame();
        else if (gameState == 2) drawResult();
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
            if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                checkHit(i);
            }
        }

        // 2. MISS判定
        Iterator<Note> iter = notes.iterator();
        while (iter.hasNext()) {
            Note note = iter.next();
            if (note.active && songPosition > note.targetTime + 0.2f) {
                note.active = false;
                message = "MISS...";
                messageTimer = 1.0f;
                combo = 0; 
            }
        }

        // 3. 描画
        // (A) レーン
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[i])) {
                shapeRenderer.setColor(1, 1, 0, 0.3f);
                float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
                shapeRenderer.rect(x, 0, GameConfig.LANE_WIDTH, GameConfig.SCREEN_HEIGHT);
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.line(0, GameConfig.JUDGEMENT_LINE_Y, GameConfig.SCREEN_WIDTH, GameConfig.JUDGEMENT_LINE_Y);
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
            shapeRenderer.line(x, 0, x, GameConfig.SCREEN_HEIGHT);
        }
        shapeRenderer.end();

        // (B) 波紋とキラキラ
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE); 
        
        // --- 波紋 ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Iterator<Ripple> rIter = ripples.iterator();
        while (rIter.hasNext()) {
            Ripple r = rIter.next();
            r.radius += (r.maxRadius - r.radius) * 8.0f * Gdx.graphics.getDeltaTime(); // 広がる速度を少し速く
            r.life -= Gdx.graphics.getDeltaTime();

            if (r.life <= 0) {
                rIter.remove();
            } else {
                float alpha = r.life * 2.5f; 
                shapeRenderer.setColor(0.2f, 1.0f, 1.0f, alpha); 
                shapeRenderer.circle(r.x, r.y, r.radius); 
            }
        }
        shapeRenderer.end();

        // --- キラキラ ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Iterator<Particle> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle p = pIter.next();
            p.x += p.vx * Gdx.graphics.getDeltaTime();
            p.y += p.vy * Gdx.graphics.getDeltaTime();
            p.life -= Gdx.graphics.getDeltaTime();

            if (p.life <= 0) {
                pIter.remove();
            } else {
                float size = (p.life / p.maxLife) * 10.0f; // 粒子も少し小さく
                shapeRenderer.setColor(0.7f, 0.9f, 1.0f, p.life); 
                shapeRenderer.rect(p.x - size/2, p.y - size/2, size, size);
            }
        }
        shapeRenderer.end();
        
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


        // (C) ノーツとUI
        batch.begin();
        font.getData().setScale(2.0f);
        font.setColor(Color.WHITE);

        for (Note note : notes) {
            if (note.active) {
                float y = GameConfig.JUDGEMENT_LINE_Y + (note.targetTime - songPosition) * GameConfig.NOTE_SPEED;
                float x = GameConfig.LANE_START_X + (note.lane * GameConfig.LANE_WIDTH);
                if (y < GameConfig.SCREEN_HEIGHT && y > -100) {
                    batch.draw(noteImg, x + 5, y, GameConfig.LANE_WIDTH - 10, 64);
                }
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            gameState = 0; 
        }
    }

    void spawnEffects(float x, float y) {
        ripples.add(new Ripple(x, y));
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y));
        }
    }

    void checkHit(int lane) {
        for (Note note : notes) {
            if (note.lane != lane || !note.active) continue;
            float timeDiff = Math.abs(note.targetTime - songPosition);
            if (timeDiff < 0.2f) {
                message = "PERFECT!!";
                messageTimer = 1.0f; 
                note.active = false; 
                score += scorePerNote;
                combo++; 
                if (hitSound != null) hitSound.play();
                
                float hitX = GameConfig.LANE_START_X + (lane * GameConfig.LANE_WIDTH) + (GameConfig.LANE_WIDTH / 2);
                spawnEffects(hitX, GameConfig.JUDGEMENT_LINE_Y + 30);

                return; 
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        noteImg.dispose();
        font.dispose();
        music.dispose();
        if (hitSound != null) hitSound.dispose();
    }
}