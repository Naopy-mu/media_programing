package com.example.game; // ★あなたのパッケージ名

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
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

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("libgdx.png"); // assetsにある画像
        font = new BitmapFont();
        font.getData().setScale(2.0f);

        // ★音楽の読み込み
        // ファイル名: "Timepiece Tower.mp3" (スペースあり、スペル修正済み)
        music = Gdx.audio.newMusic(Gdx.files.internal("Timepiece Tower.mp3"));
        music.play();

        // ★譜面の読み込み
        // assets/chart.csv からデータを読み込む
        try {
            notes = ChartLoader.loadChart("chart.csv");
            System.out.println("譜面読み込み成功: " + notes.size + "個のノーツ");
        } catch (Exception e) {
            System.out.println("譜面読み込みエラー: " + e.getMessage());
            // エラー時は空のリストにして落ちないようにする
            notes = new Array<>();
        }
    }

    @Override
    public void render() {
        // 画面を黒でクリア
        ScreenUtils.clear(0, 0, 0, 1);
        
        // 音楽再生位置を取得
        songPosition = music.getPosition();

        // --- 1. キー入力判定 (Input Logic) ---
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                checkHit(i);
            }
        }

        // --- 2. MISS判定 (Game Logic) ---
        Iterator<Note> iter = notes.iterator();
        while (iter.hasNext()) {
            Note note = iter.next();
            // ノーツが有効で、判定ラインを大きく過ぎていたら (0.2秒遅れ)
            if (note.active && songPosition > note.targetTime + 0.2f) {
                note.active = false; // 消す
                message = "MISS...";
                messageTimer = 1.0f;
            }
        }

        // --- 3. 描画処理 (Rendering) ---
        
        // (A) レーンが光るエフェクト
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[i])) {
                shapeRenderer.setColor(1, 1, 0, 0.3f); // 黄色 半透明
                float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
                shapeRenderer.rect(x, 0, GameConfig.LANE_WIDTH, GameConfig.SCREEN_HEIGHT);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // (B) 枠線と判定ライン
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        // 判定ライン
        shapeRenderer.line(0, GameConfig.JUDGEMENT_LINE_Y, GameConfig.SCREEN_WIDTH, GameConfig.JUDGEMENT_LINE_Y);
        // 縦線
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
            shapeRenderer.line(x, 0, x, GameConfig.SCREEN_HEIGHT);
        }
        shapeRenderer.end();

        // (C) ノーツの描画
        batch.begin();
        for (Note note : notes) {
            if (note.active) {
                // Y座標の計算
                float y = GameConfig.JUDGEMENT_LINE_Y + (note.targetTime - songPosition) * GameConfig.NOTE_SPEED;
                float x = GameConfig.LANE_START_X + (note.lane * GameConfig.LANE_WIDTH);
                
                // 画面内にある時だけ描画
                if (y < GameConfig.SCREEN_HEIGHT && y > -100) {
                    batch.draw(noteImg, x + 5, y, GameConfig.LANE_WIDTH - 10, 64);
                }
            }
        }

        // (D) メッセージと時間の描画
        if (messageTimer > 0) {
            font.draw(batch, message, 100, 300);
            messageTimer -= Gdx.graphics.getDeltaTime();
        }
        font.draw(batch, "Time: " + String.format("%.2f", songPosition), 10, 470);
        batch.end();
    }

    // ヒット判定メソッド
    void checkHit(int lane) {
        for (Note note : notes) {
            // 違うレーンや無効なノーツは無視
            if (note.lane != lane || !note.active) continue;

            // 時間差（絶対値）
            float timeDiff = Math.abs(note.targetTime - songPosition);

            // 0.2秒以内ならHIT
            if (timeDiff < 0.2f) {
                message = "PERFECT!!";
                messageTimer = 1.0f; 
                note.active = false; // 判定済みにする
                return; // 1個叩いたら終了
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        noteImg.dispose();
        font.dispose();
        music.dispose(); // 音楽も破棄
    }
}