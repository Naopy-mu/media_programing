package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Array;
import com.example.game.Main;
import com.example.game.GameConfig;

public class SongSelectScreen extends ScreenAdapter {
    final Main game;
    
    // --- 曲データ ---
    String[] songs = {
        "Timepiece Tower",
        "Eigenstate",
        "Link Layer",
        "Pop!Stack!"
    };
    // 各曲の難易度（仮）
    int[] difficulties = { 5, 8, 4, 6 };

    int selectedIndex = 0;
    float currentScroll = 0; // スクロール演出用

    // --- 画像素材 ---
    Texture panelImg; // 曲名バー
    Array<Texture> circleTextures; // 円形パーツの配列

    // --- 音楽プレビュー用 ---
    Music previewMusic;
    String currentPlayingSong = "";

    // --- アニメーション管理 ---
    enum State {
        BROWSING,       // 選曲中
        ANIM_SHRINK,    // バーが縮む
        ANIM_MOVE,      // 円が中央へ移動
        ANIM_ENTER      // 円の中へ突入（ゲーム開始直前）
    }
    State currentState = State.BROWSING;
    float stateTimer = 0; // アニメーション経過時間

    // 円のアニメーション用パラメータ
    float[] circleRotations; // 各円の現在の角度
    float[] circleSpeeds;    // 各円の回転速度
    
    // UIアニメーション用変数
    float barWidthScale = 1.0f; // バーの横幅倍率
    float circleX = 300;        // 円のX座標（最初は左側）
    float circleScale = 1.0f;   // 円の大きさ
    float circleAlpha = 1.0f;   // 円の透明度

    ShapeRenderer shapeRenderer;

    public SongSelectScreen(Main game) {
        this.game = game;
        shapeRenderer = new ShapeRenderer();
        
        // 画像読み込み
        panelImg = new Texture("song-select-UI.png");
        
        // 円形画像を読み込む (circle0.png ～ circle5.png)
        circleTextures = new Array<>();
        circleRotations = new float[6];
        circleSpeeds = new float[6];

        for(int i = 0; i < 6; i++) {
            // ※ファイルがない場合はエラー回避のため try-catch
            try {
                Texture tex = new Texture("circle" + i + ".png");
                // 拡大縮小時に滑らかにする設定
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                circleTextures.add(tex);
                
                // 回転速度をランダムに設定（内側と外側で逆回転させるとカッコいい）
                float speed = MathUtils.random(20f, 60f);
                if (i % 2 == 0) speed *= -1; // 偶数は逆回転
                circleSpeeds[i] = speed;
                
            } catch(Exception e) {
                System.out.println("circle" + i + ".png not found");
            }
        }

        // 最初の曲を再生
        playPreview(songs[0]);
    }

    // プレビュー再生メソッド
    void playPreview(String songName) {
        if (songName.equals(currentPlayingSong)) return;

        if (previewMusic != null) {
            previewMusic.stop();
            previewMusic.dispose();
        }
        
        try {
            previewMusic = Gdx.audio.newMusic(Gdx.files.internal(songName + ".mp3"));
            previewMusic.setVolume(0.5f);
            previewMusic.setLooping(true);
            previewMusic.play();
            currentPlayingSong = songName;
        } catch(Exception e) {
            System.out.println("Music not found: " + songName);
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        
        // --- 状態ごとのロジック更新 ---
        updateLogic(delta);

        // --- 描画開始 ---
        
        // 1. スペクトラムと円の描画（背景側）
        drawCirclesAndSpectrum(delta);

        game.batch.begin();
        
        // 2. 曲リスト（UI）の描画
        // アニメーションが進んだら（ENTER状態なら）リストはもう描画しない
        if (currentState != State.ANIM_ENTER) {
            drawSongList(delta);
        }
        
        // ガイド表示
        if (currentState == State.BROWSING) {
            game.font.getData().setScale(1.5f);
            game.font.setColor(Color.WHITE);
            game.font.draw(game.batch, "[UP/DOWN] Select   [SPACE] CONNECT   [O] Option", 20, 100);
        }

        game.batch.end();
    }

    void updateLogic(float delta) {
        // 回転アニメーションは常に更新
        for(int i=0; i<circleRotations.length; i++) {
            circleRotations[i] += circleSpeeds[i] * delta;
        }

        switch (currentState) {
            case BROWSING:
                handleInput();
                // 円の位置：左側（X=350あたり）
                // 目標値へ滑らかに移動
                circleX = MathUtils.lerp(circleX, 350f, 0.1f);
                circleScale = MathUtils.lerp(circleScale, 0.6f, 0.1f); // 選択中は少し小さめ
                currentScroll = MathUtils.lerp(currentScroll, (float)selectedIndex, 0.1f);
                break;

            case ANIM_SHRINK:
                stateTimer += delta;
                // バーを縮める (1.0 -> 0.0)
                barWidthScale = Math.max(0, 1.0f - stateTimer * 3.0f); // 0.3秒で消える
                
                if (stateTimer > 0.4f) {
                    currentState = State.ANIM_MOVE;
                    stateTimer = 0;
                }
                break;

            case ANIM_MOVE:
                stateTimer += delta;
                // 円を中央へ移動 (350 -> 960)
                circleX = MathUtils.lerp(circleX, GameConfig.SCREEN_WIDTH / 2f, 0.15f);
                // 大きさを戻す
                circleScale = MathUtils.lerp(circleScale, 1.0f, 0.15f);
                
                // 回転を加速させる演出
                for(int i=0; i<circleSpeeds.length; i++) {
                    circleSpeeds[i] *= 1.02f; 
                }

                if (stateTimer > 1.0f) {
                    currentState = State.ANIM_ENTER;
                    stateTimer = 0;
                }
                break;
                
            case ANIM_ENTER:
                stateTimer += delta;
                // 突入演出：円を巨大化させつつ透明にする
                circleScale += delta * 15.0f; // 急激に拡大
                circleAlpha -= delta * 1.5f;  // フェードアウト

                if (stateTimer > 0.8f) {
                    // 次の画面へ
                    game.setScreen(new GameScreen(game, songs[selectedIndex]));
                    dispose();
                }
                break;
        }
    }

    // 円とスペクトラムの描画
    void drawCirclesAndSpectrum(float delta) {
        float centerX = circleX;
        float centerY = GameConfig.SCREEN_HEIGHT / 2f;
        
        // --- A. オーディオスペクトラム（擬似）の描画 ---
        // ShapeRendererを使って円の内側に描く
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0, 0.8f, 1, circleAlpha); // シアン色
        
        int spectrumCount = 60; // 線の本数
        float radius = 150 * circleScale; // 円の内側半径
        
        for (int i = 0; i < spectrumCount; i++) {
            float angle = (360f / spectrumCount) * i;
            // 音楽に合わせて長さが変わるように見せる計算
            // MathUtils.sin と random を組み合わせて「音楽っぽさ」を出す
            float time = stateTimer + (System.currentTimeMillis() / 100.0f);
            float noise = MathUtils.sin(time * 5f + i) * MathUtils.random(0.5f, 1.5f);
            
            // ANIM_ENTERの時は激しくする
            if (currentState == State.ANIM_ENTER) noise *= 2.0f;

            float length = 30f + (noise * 20f);
            length *= circleScale;

            float rad = MathUtils.degRad * angle;
            float x1 = centerX + MathUtils.cos(rad) * radius;
            float y1 = centerY + MathUtils.sin(rad) * radius;
            // 内側に向かって線を引く
            float x2 = centerX + MathUtils.cos(rad) * (radius - length);
            float y2 = centerY + MathUtils.sin(rad) * (radius - length);
            
            shapeRenderer.line(x1, y1, x2, y2);
        }
        shapeRenderer.end();

        // --- B. 円形画像の描画 ---
        game.batch.begin();
        game.batch.setColor(1, 1, 1, circleAlpha);

        for (int i = 0; i < circleTextures.size; i++) {
            Texture tex = circleTextures.get(i);
            
            // 画像によってサイズを変える（外側ほど大きく、内側ほど小さく）
            // circle0が一番外側、circle5が一番内側と仮定
            // 画像の元サイズにもよりますが、スケールで調整
            float baseSize = 400f + (5 - i) * 60f; // 適当なサイズ計算
            float size = baseSize * circleScale;
            
            // 回転描画
            game.batch.draw(
                tex,
                centerX - size/2, centerY - size/2, // 描画位置
                size/2, size/2,                     // 回転軸（中心）
                size, size,                         // 幅、高さ
                1.0f, 1.0f,                         // スケール
                circleRotations[i],                 // 角度
                0, 0,                               // 画像切り抜き開始位置
                tex.getWidth(), tex.getHeight(),    // 切り抜きサイズ
                false, false                        // 反転
            );
        }
        
        // --- C. 難易度数値の描画 ---
        if (circleAlpha > 0.1f) {
            String difText = String.valueOf(difficulties[selectedIndex]);
            game.font.getData().setScale(4.0f * circleScale);
            game.font.setColor(0, 1, 1, circleAlpha); // シアン
            
            // 文字を中心へ
            float textW = 40; // 概算
            game.font.draw(game.batch, difText, centerX - textW/2, centerY + 30);
            
            game.font.getData().setScale(1.5f * circleScale);
            game.font.draw(game.batch, "LEVEL", centerX - 40, centerY - 40);
        }

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void drawSongList(float delta) {
        float aspectRatio = (float)panelImg.getHeight() / (float)panelImg.getWidth();

        for (int i = 0; i < songs.length; i++) {
            // スクロール位置計算
            float distance = i - currentScroll;
            if (Math.abs(distance) > 4.0f) continue;

            float centerY = (GameConfig.SCREEN_HEIGHT / 2f) - (distance * 220f);
            
            // X座標（選択中のものだけ右に飛び出している等の演出）
            // 基本は画面中央より右側(1100あたり)に配置し、円(左側)と被らないようにする
            float centerX = 1200f; 
            if (i == selectedIndex) centerX = 1150f; // 選択中は少し左（目立つ位置）へ

            float scale = Math.max(0.6f, 1.0f - Math.abs(distance) * 0.15f);
            float alpha = Math.max(0.3f, 1.0f - Math.abs(distance) * 0.5f);

            // ★重要：決定時の縮小アニメーション
            // 選択中の曲かつ、アニメーション中なら幅を縮める
            float widthAnim = 1.0f;
            if (i == selectedIndex && currentState == State.ANIM_SHRINK) {
                widthAnim = barWidthScale;
            } else if (i != selectedIndex && currentState != State.BROWSING) {
                // 選ばれてない曲はフェードアウト
                alpha *= barWidthScale; 
            }

            float imgW = 900 * scale * widthAnim;          
            float imgH = 900 * scale * aspectRatio; // 幅に合わせて高さも変える   
            
            game.batch.setColor(1f, 1f, 1f, alpha);
            
            // バーの描画（中心基準）
            if (imgW > 0) {
                game.batch.draw(panelImg, centerX - imgW/2, centerY - imgH/2, imgW, imgH);
                
                // 文字の描画
                if (widthAnim > 0.5f) { // 幅が半分以下になったら文字は消す
                    game.font.getData().setScale(2.0f * scale);
                    Color c = (i == selectedIndex) ? Color.WHITE : Color.LIGHT_GRAY;
                    game.font.setColor(c.r, c.g, c.b, alpha);
                    // バーの縮小に合わせて文字もクリッピングなどすべきだが、今回は簡易的に描画
                    game.font.draw(game.batch, songs[i], centerX - 300, centerY + 20);
                }
            }
        }
        
        // 色戻し
        game.batch.setColor(1, 1, 1, 1);
    }

    void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = songs.length - 1;
            playPreview(songs[selectedIndex]);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex++;
            if (selectedIndex >= songs.length) selectedIndex = 0;
            playPreview(songs[selectedIndex]);
        }
        
        // 決定キーでアニメーション開始
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            currentState = State.ANIM_SHRINK; // 縮小フェーズへ移行
            // ここで決定音などを鳴らすと良い
        }

        // オプションなどへの遷移（選曲中のみ）
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            game.setScreen(new OptionScreen(game));
            dispose();
        }
    }

    @Override
    public void dispose() {
        if (panelImg != null) panelImg.dispose();
        if (previewMusic != null) previewMusic.dispose();
        
        // 円画像を解放
        for(Texture t : circleTextures) {
            t.dispose();
        }
    }
}