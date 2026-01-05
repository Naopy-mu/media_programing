package com.example.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter { // ★ファイル名がMain.java以外なら、ここの名前も合わせる
    SpriteBatch batch;
    ShapeRenderer shapeRenderer; // 線を引くためのツール
    Texture noteImg;             // ノーツの画像

    // ノーツのリスト
    Array<Note> notes = new Array<>();
    
    // 時間管理用（音楽の代わり）
    float songPosition = 0;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        
        // assetsフォルダにある画像を指定
        // (デフォルトで "libgdx.png" か "badlogic.jpg" があるはずです。確認してください)
        noteImg = new Texture("libgdx.png"); 

        // ★テスト用にノーツを登録してみる
        // new Note(落ちてくる時間, レーン番号0~3)
        notes.add(new Note(2.0f, 0)); // 2秒後に一番左
        notes.add(new Note(3.0f, 1)); // 3秒後に左から2番目
        notes.add(new Note(4.0f, 2)); // 4秒後に右から2番目
        notes.add(new Note(5.0f, 3)); // 5秒後に一番右
        
        // 同時押しのテスト
        notes.add(new Note(6.0f, 1));
        notes.add(new Note(6.0f, 2));
    }

    @Override
    public void render() {
        // 1. 画面を黒でクリア
        ScreenUtils.clear(0, 0, 0, 1);
        
        // 2. 時間を進める
        songPosition += Gdx.graphics.getDeltaTime();

        // --- 3. レーンの枠線を描画 (ShapeRenderer) ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);

        // 判定ライン（横線）
        shapeRenderer.line(0, GameConfig.JUDGEMENT_LINE_Y, GameConfig.SCREEN_WIDTH, GameConfig.JUDGEMENT_LINE_Y);

        // 縦のレーン区切り線（5本引く）
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            // GameConfigの定数を使ってX座標を計算
            float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
            shapeRenderer.line(x, 0, x, GameConfig.SCREEN_HEIGHT);
        }
        shapeRenderer.end();

        // --- 4. ノーツの描画 (SpriteBatch) ---
        batch.begin();
        for (Note note : notes) {
            if (note.active) {
                // Y座標：「(叩く時間 -今の時間) × 速さ」 + 判定ライン位置
                float y = GameConfig.JUDGEMENT_LINE_Y + (note.targetTime - songPosition) * GameConfig.NOTE_SPEED;

                // X座標：「全体の開始位置」 + 「レーン番号 × 1レーンの幅」
                float x = GameConfig.LANE_START_X + (note.lane * GameConfig.LANE_WIDTH);

                // 画面内にある時だけ描画
                if (y < GameConfig.SCREEN_HEIGHT && y > -100) {
                    // ノーツの幅をレーン幅より少し小さく(左右-5px)して描画
                    batch.draw(noteImg, x + 5, y, GameConfig.LANE_WIDTH - 10, 64);
                }
            }
        }
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        noteImg.dispose();
    }
}