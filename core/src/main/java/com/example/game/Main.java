package com.example.game; // ← ★元のまま！

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music; // ← ★追加：音楽用
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
    Music music; // ← ★追加：音楽変数

    Array<Note> notes = new Array<>();
    float songPosition = 0;
    
    String message = "";
    float messageTimer = 0;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("libgdx.png");
        font = new BitmapFont();
        font.getData().setScale(2.0f);

        // ★追加：音楽の読み込みと再生
        // assetsフォルダに bgm.mp3 を入れておくこと！
        music = Gdx.audio.newMusic(Gdx.files.internal("Timepiece Tower.mp3"));
        music.play(); // 再生開始

        // テストデータ
        notes.add(new Note(2.0f, 0));
        notes.add(new Note(3.0f, 1));
        notes.add(new Note(4.0f, 2));
        notes.add(new Note(5.0f, 3));
        notes.add(new Note(6.0f, 1));
        notes.add(new Note(7.0f, 2));
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        
        // ★変更：タイマーではなく「音楽の再生時間」を使う
        // これで曲とズレなくなります
        songPosition = music.getPosition();

        // --- 1. 入力判定 ---
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                checkHit(i);
            }
        }

        // --- ★追加：MISS判定のチェック ---
        // 通り過ぎたノーツがないか確認する
        Iterator<Note> iter = notes.iterator();
        while (iter.hasNext()) {
            Note note = iter.next();
            // まだ生きていて、かつ 時間が通り過ぎていたら (0.2秒以上遅れたら)
            if (note.active && songPosition > note.targetTime + 0.2f) {
                note.active = false; // 消す
                message = "MISS..."; // メッセージ
                messageTimer = 1.0f;
            }
        }


        // --- 2. 描画 ---
        // (A) レーン光る処理
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

        // (C) ノーツ
        batch.begin();
        for (Note note : notes) {
            if (note.active) {
                float y = GameConfig.JUDGEMENT_LINE_Y + (note.targetTime - songPosition) * GameConfig.NOTE_SPEED;
                float x = GameConfig.LANE_START_X + (note.lane * GameConfig.LANE_WIDTH);
                
                if (y < GameConfig.SCREEN_HEIGHT && y > -100) {
                    batch.draw(noteImg, x + 5, y, GameConfig.LANE_WIDTH - 10, 64);
                }
            }
        }

        // (D) メッセージと時間
        if (messageTimer > 0) {
            font.draw(batch, message, 100, 300);
            messageTimer -= Gdx.graphics.getDeltaTime();
        }
        font.draw(batch, "Time: " + String.format("%.2f", songPosition), 10, 470);
        batch.end();
    }

    void checkHit(int lane) {
        for (Note note : notes) {
            if (note.lane != lane || !note.active) continue;

            float timeDiff = Math.abs(note.targetTime - songPosition);

            if (timeDiff < 0.2f) { // 0.2秒以内ならHIT
                message = "PERFECT!!";
                messageTimer = 1.0f; 
                note.active = false; 
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
        music.dispose(); // ← ★追加：音楽も後片付け
    }
}