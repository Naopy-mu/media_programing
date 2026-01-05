package com.example.game; // ← ★元のパッケージ名のまま！

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont; // ← 追加：文字を書くため
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.Iterator; // ← 追加：リストから削除するために必要

public class Main extends ApplicationAdapter {
    SpriteBatch batch;
    ShapeRenderer shapeRenderer;
    Texture noteImg;
    BitmapFont font; // ← 追加：判定結果を表示するフォント

    Array<Note> notes = new Array<>();
    float songPosition = 0;
    
    // 判定結果のメッセージ（"PERFECT!", "MISS..." など）
    String message = "";
    float messageTimer = 0; // メッセージを消すためのタイマー

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("libgdx.png");
        font = new BitmapFont(); // デフォルトの文字フォントを読み込み
        font.getData().setScale(2.0f); // 文字を少し大きくする

        // テスト用データ
        notes.add(new Note(2.0f, 0));
        notes.add(new Note(3.0f, 1));
        notes.add(new Note(4.0f, 2));
        notes.add(new Note(5.0f, 3));
        
        // 連続ノーツのテスト
        notes.add(new Note(6.0f, 1));
        notes.add(new Note(6.5f, 1));
        notes.add(new Note(7.0f, 1));
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        songPosition += Gdx.graphics.getDeltaTime();

        // --- 1. 入力判定処理 (Input Logic) ---
        // 各レーンのキーが「押された瞬間 (JustPressed)」をチェック
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                checkHit(i); // ★判定メソッドを呼び出す
            }
        }

        // --- 2. 描画処理 (Rendering) ---
        
        // (A) レーンが光る処理（押しっぱなしで光る）
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[i])) {
                shapeRenderer.setColor(1, 1, 0, 0.3f); // 薄い黄色
                float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
                shapeRenderer.rect(x, 0, GameConfig.LANE_WIDTH, GameConfig.SCREEN_HEIGHT);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // (B) 枠線と判定ライン
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.line(0, GameConfig.JUDGEMENT_LINE_Y, GameConfig.SCREEN_WIDTH, GameConfig.JUDGEMENT_LINE_Y);
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
            shapeRenderer.line(x, 0, x, GameConfig.SCREEN_HEIGHT);
        }
        shapeRenderer.end();

        // (C) ノーツ描画
        batch.begin();
        for (Note note : notes) {
            if (note.active) {
                float y = GameConfig.JUDGEMENT_LINE_Y + (note.targetTime - songPosition) * GameConfig.NOTE_SPEED;
                float x = GameConfig.LANE_START_X + (note.lane * GameConfig.LANE_WIDTH);
                
                // 画面内にある時だけ描画
                if (y < GameConfig.SCREEN_HEIGHT && y > -100) {
                    batch.draw(noteImg, x + 5, y, GameConfig.LANE_WIDTH - 10, 64);
                }
            }
        }

        // (D) 判定メッセージの表示
        if (messageTimer > 0) {
            font.draw(batch, message, 100, 300);
            messageTimer -= Gdx.graphics.getDeltaTime(); // 時間経過で消していく
        }
        
        // デバッグ用：現在の時間を表示
        font.draw(batch, "Time: " + String.format("%.2f", songPosition), 10, 470);
        
        batch.end();
    }

    // ★重要：判定を行うメソッド
    void checkHit(int lane) {
        // そのレーンに存在する「まだ処理されていないノーツ」を探す
        for (Note note : notes) {
            // 違うレーンや、すでに処理済みのノーツは無視
            if (note.lane != lane || !note.active) continue;

            // 時間のズレ（絶対値）を計算
            float timeDiff = Math.abs(note.targetTime - songPosition);

            // 判定基準（例：0.2秒以内ならHIT）
            if (timeDiff < 0.2f) {
                message = "PERFECT!!";
                messageTimer = 1.0f; // 1秒間表示
                note.active = false; // ノーツを消す（判定済みにする）
                return; // 1回叩いたら1個だけ判定して終了
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        noteImg.dispose();
        font.dispose(); // ← フォントも片付ける
    }
}