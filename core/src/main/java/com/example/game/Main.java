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
    float songPosition = 0;
    
    String message = "";
    float messageTimer = 0;

    float score = 0;
    int combo = 0;
    float scorePerNote = 0;

    // ★状態管理
    // 0: タイトル, 1: ゲームプレイ, 2: リザルト
    int gameState = 0; 

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("libgdx.png");
        font = new BitmapFont();
        
        music = Gdx.audio.newMusic(Gdx.files.internal("Timepiece Tower.mp3"));
        music.setVolume(0.3f);
        
        // 曲が終わったらリザルト(2)へ
        music.setOnCompletionListener(music -> {
            gameState = 2;
        });

        // 効果音 (mp3)
        try {
            hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.mp3"));
        } catch (Exception e) {
            System.out.println("効果音エラー: " + e.getMessage());
        }

        // 最初はタイトル画面(0)なので、ここではゲームを開始しない
    }

    // ゲームを開始するメソッド
    void startGame() {
        gameState = 1; // プレイ中へ
        score = 0;
        combo = 0;
        message = "";
        songPosition = 0;
        
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
        ScreenUtils.clear(0, 0, 0, 1); // 背景黒

        // ★状態によって画面を切り替える
        if (gameState == 0) {
            drawTitle(); // タイトル画面
        } else if (gameState == 1) {
            updateAndDrawGame(); // ゲーム画面
        } else if (gameState == 2) {
            drawResult(); // リザルト画面
        }
    }

    // --- 0. タイトル画面 ---
    void drawTitle() {
        batch.begin();
        
        // タイトルロゴ
        font.getData().setScale(3.0f);
        font.setColor(Color.CYAN);
        font.draw(batch, "RHYTHM GAME", 150, 350);
        
        // スタート案内
        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Press SPACE to Start", 180, 200);

        batch.end();

        // スペースキーでゲーム開始
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            // ここで効果音を鳴らすとかっこいい
            if (hitSound != null) hitSound.play();
            startGame();
        }
    }

    // --- 1. ゲーム中の処理 ---
    void updateAndDrawGame() {
        songPosition = music.getPosition();

        // 入力判定
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                checkHit(i);
            }
        }

        // MISS判定
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

        // 描画
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
        Gdx.gl.glDisable(GL20.GL_BLEND);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.line(0, GameConfig.JUDGEMENT_LINE_Y, GameConfig.SCREEN_WIDTH, GameConfig.JUDGEMENT_LINE_Y);
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
            shapeRenderer.line(x, 0, x, GameConfig.SCREEN_HEIGHT);
        }
        shapeRenderer.end();

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

    // --- 2. リザルト画面 ---
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
        // ★修正：タイトルに戻る案内
        font.draw(batch, "Press SPACE to Title", 180, 100);
        
        batch.end();

        // スペースキーでタイトル画面(0)へ戻る
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            gameState = 0; 
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