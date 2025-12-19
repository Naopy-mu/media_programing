package com.example.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

public class MyFirstGame extends ApplicationAdapter {
    SpriteBatch batch;
    ShapeRenderer shapeRenderer; // 図形（判定ライン）を描く用
    Texture noteImg;             // ノーツの画像
    Music music;                 // 音楽
    BitmapFont font;             // 文字表示用

    // ゲーム設定
    float songPosition = 0;      // 曲の現在の再生時間（秒）
    float noteSpeed = 500;       // ノーツが落ちる速さ（ピクセル/秒）
    float judgementLineY = 100;  // 判定ラインのY座標

    // 譜面データ（ノーツが来るタイミング：秒）
    Array<Float> notes = new Array<>();
    
    // 判定用メッセージ
    String message = "";

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont(); // デフォルトのフォント
        font.getData().setScale(2);

        // 画像と音楽の読み込み (assetsフォルダに入れておくこと)
        noteImg = new Texture("libgdx.png"); // 既存の画像で代用
        music = Gdx.audio.newMusic(Gdx.files.internal("bgm.mp3"));

        // ★譜面を作成（曲の開始から何秒後に叩くかを手動で登録）
        notes.add(2.0f); // 2秒目
        notes.add(3.5f); // 3.5秒目
        notes.add(5.0f); // 5秒目
        
        // 曲を再生
        music.play();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);

        // 1. 現在の再生時間を取得
        songPosition = music.getPosition();

        // --- 入力判定の処理 ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            checkHit();
        }

        // --- 描画処理 ---
        
        // 判定ラインを描く（赤線）
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.line(0, judgementLineY, 640, judgementLineY);
        shapeRenderer.end();

        batch.begin();

        // ノーツを描画・管理
        // (後ろからループしているのは、リストから削除する可能性があるため)
        for (int i = 0; i < notes.size; i++) {
            float targetTime = notes.get(i);
            
            // 重要：ノーツのY座標の計算式
            // 「(叩く時間 - 現在の時間) × 速さ」で、判定ラインからの距離が決まる
            float y = judgementLineY + (targetTime - songPosition) * noteSpeed;

            // 画面内にある時だけ描画
            if (y < 800 && y > -100) {
                // 画像の中心をX座標300に合わせる
                batch.draw(noteImg, 300, y, 64, 64);
            }
        }

        // メッセージ表示（HIT! や MISS!）
        font.draw(batch, "Time: " + String.format("%.2f", songPosition), 20, 460);
        font.draw(batch, message, 300, 300);
        
        batch.end();
    }
    
    // キーを押したときの判定ロジック
    void checkHit() {
        if (notes.size == 0) return;

        // 一番手前（最初）にあるノーツを見る
        float targetTime = notes.get(0);
        
        // ズレ（差）を計算。絶対値をとる
        float diff = Math.abs(targetTime - songPosition);

        // 0.15秒以内のズレなら「HIT」とする
        if (diff < 0.15f) {
            message = "PERFECT!!";
            notes.removeIndex(0); // 叩いたノーツを消す
        } else if (diff < 0.3f) {
            message = "GOOD";
            notes.removeIndex(0);
        } else {
            message = "MISS...";
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        noteImg.dispose();
        music.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}