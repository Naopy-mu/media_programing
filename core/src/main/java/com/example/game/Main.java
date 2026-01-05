package com.example.game; // ← ★ここは元のまま！

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20; // ← 追加: 透明度を使うために必要
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter { // ★クラス名はファイル名に合わせてね
    SpriteBatch batch;
    ShapeRenderer shapeRenderer;
    Texture noteImg;
    Array<Note> notes = new Array<>();
    float songPosition = 0;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("libgdx.png"); // assetsにある画像名

        // テスト用データ
        notes.add(new Note(2.0f, 0));
        notes.add(new Note(3.0f, 1));
        notes.add(new Note(4.0f, 2));
        notes.add(new Note(5.0f, 3));
        notes.add(new Note(6.0f, 1));
        notes.add(new Note(6.0f, 2));
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        songPosition += Gdx.graphics.getDeltaTime();

        // --- ★ここから追加機能：キー入力の反応 ---
        
        // 透明度(アルファ)を使えるようにする設定
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled); // 「塗りつぶし」モード
        
        // 4つのレーンを順番にチェック
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            // もし、そのレーンに対応するキーが押されていたら？
            if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[i])) {
                // 色をセット (黄色, 透明度0.5)
                shapeRenderer.setColor(1, 1, 0, 0.5f);
                
                // レーンの場所に長方形を描く
                float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
                shapeRenderer.rect(x, 0, GameConfig.LANE_WIDTH, GameConfig.SCREEN_HEIGHT);
            }
        }
        shapeRenderer.end();
        // --- ★ここまで追加 ---


        // --- 既存の描画処理 ---
        
        // レーンの枠線
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.line(0, GameConfig.JUDGEMENT_LINE_Y, GameConfig.SCREEN_WIDTH, GameConfig.JUDGEMENT_LINE_Y);
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            float x = GameConfig.LANE_START_X + (i * GameConfig.LANE_WIDTH);
            shapeRenderer.line(x, 0, x, GameConfig.SCREEN_HEIGHT);
        }
        shapeRenderer.end();

        // ノーツ描画
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
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        noteImg.dispose();
    }
}