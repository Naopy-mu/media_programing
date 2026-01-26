package com.example.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
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

    Array<Note> notes = new Array<>();
    float songPosition = 0;
    
    String message = "";
    float messageTimer = 0;

    float score = 0;
    int combo = 0;
    float scorePerNote = 0;

    // ★追加：ゲームの状態 (0=プレイ中, 1=リザルト)
    int gameState = 0; 

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("libgdx.png");
        font = new BitmapFont();
        
        // 音楽読み込み
        music = Gdx.audio.newMusic(Gdx.files.internal("Timepiece Tower.mp3"));
        
        // ★追加：曲が終わったら「リザルト状態」にする設定
        music.setOnCompletionListener(music -> {
            gameState = 1; // 状態を1(リザルト)に切り替え
            System.out.println("曲が終了しました。リザルト画面へ移動します。");
        });

        startGame(); // ゲーム開始処理
    }

    // ゲームを初期化して開始するメソッド
    void startGame() {
        gameState = 0; // プレイ中
        score = 0;
        combo = 0;
        message = "";
        songPosition = 0;
        
        // 譜面の再読み込み
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

        // ★状態によって描画を分ける
        if (gameState == 0) {
            updateAndDrawGame(); // ゲーム画面
        } else {
            drawResult(); // リザルト画面
        }
    }

    // --- ゲーム中の処理 ---
    void updateAndDrawGame() {
        songPosition = music.getPosition();

        // 1. 入力判定
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
        // (A) レーン発光
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

        // (B) 枠線
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.line(0, GameConfig.JUDGEMENT_LINE_Y, GameConfig.SCREEN_WIDTH, GameConfig.JUDGEMENT_LINE_Y);
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
            shapeRenderer.line(x, 0, x, GameConfig.SCREEN_HEIGHT);
        }
        shapeRenderer.end();

        // (C) 画像と文字
        batch.begin();
        font.getData().setScale(2.0f); // ゲーム中は文字サイズ2

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

    // --- ★追加：リザルト画面の描画処理 ---
    void drawResult() {
        batch.begin();
        
        // 大きな文字で「CLEAR!!」
        font.getData().setScale(4.0f);
        font.setColor(Color.YELLOW);
        font.draw(batch, "GAME CLEAR!!", 100, 400);

        // 最終スコア表示
        font.getData().setScale(3.0f);
        font.setColor(Color.WHITE);
        font.draw(batch, "SCORE: " + (int)score, 150, 300);

        // ランク判定（簡易版）
        String rank = "C";
        if (score >= 900000) rank = "S";
        else if (score >= 800000) rank = "A";
        else if (score >= 700000) rank = "B";

        font.draw(batch, "RANK: " + rank, 200, 200);

        // リトライ案内
        font.getData().setScale(1.5f);
        font.draw(batch, "Press SPACE to Retry", 180, 100);
        
        batch.end();

        // スペースキーでリトライ
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            startGame();
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
    }
}