package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils; // ★追加：計算用
import com.badlogic.gdx.utils.ScreenUtils;
import com.example.game.Main;
import com.example.game.GameConfig;

public class SongSelectScreen extends ScreenAdapter {
    final Main game;
    
    String[] songs = {
        "Timepiece Tower",
        "Eigenstate",
        "Link Layer",
        "Pop!Stack!"
    };
    int selectedIndex = 0;
    
    // ★追加：滑らかに動くための「現在位置（小数）」
    float currentScroll = 0;

    Texture panelImg; 

    public SongSelectScreen(Main game) {
        this.game = game;
        panelImg = new Texture("song-select-UI.png");
        // 初期位置をセット
        currentScroll = selectedIndex;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1); 

        // ★重要：現在位置を、目標（selectedIndex）に少しずつ近づける
        // 第3引数の 0.1f を変えると動きの「重さ」が変わります（大きいとキビキビ、小さいとヌルヌル）
        currentScroll = MathUtils.lerp(currentScroll, (float)selectedIndex, 0.1f);

        game.batch.begin();
        
        game.font.getData().setScale(3.0f);
        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "SELECT MUSIC", GameConfig.SCREEN_WIDTH/2f - 200, 950);

        float aspectRatio = (float)panelImg.getHeight() / (float)panelImg.getWidth();

        for (int i = 0; i < songs.length; i++) {
            // ★重要：この要素が「現在位置」からどれくらい離れているか？
            // 0ならド真ん中。1.0なら一つズレている。
            float distance = i - currentScroll;

            // 画面外の描画スキップ判定（少し広めにとる）
            // 距離が 3.5 以上離れていたら描画しない
            if (Math.abs(distance) > 3.5f) continue;

            // --- サイズと位置の計算 ---
            
            // 1. スケール計算（中央に近いほど大きく、遠いほど小さく）
            // 1.0 から 距離*0.15 を引く。ただし最小でも 0.6倍 は維持する。
            float scale = Math.max(0.6f, 1.0f - Math.abs(distance) * 0.15f);

            // 2. Y座標の計算
            // 距離に応じて上下に配置。1単位あたり220px離す。
            float centerY = (GameConfig.SCREEN_HEIGHT / 2f) - (distance * 220f);
            float centerX = GameConfig.SCREEN_WIDTH / 2f;

            // 3. 画像サイズの計算
            float imgW = 900 * scale;          
            float imgH = imgW * aspectRatio;   
            
            // 4. 色（明るさ）の計算
            // 中央は明るく(1.0)、遠くは暗く(透明度を下げる)
            float alpha = Math.max(0.3f, 1.0f - Math.abs(distance) * 0.5f);
            
            // 選択中のインデックスと一致しているなら「白」、それ以外は「グレー」っぽくしつつ、
            // 距離に応じた alpha (透明度) を適用する
            if (i == selectedIndex) {
                 // 完全に選択された状態に近づくほど明るくなる
                game.batch.setColor(1f, 1f, 1f, alpha); 
            } else {
                game.batch.setColor(0.5f, 0.5f, 0.5f, alpha); 
            }

            // 画像の描画
            game.batch.draw(panelImg, centerX - imgW/2, centerY - imgH/2, imgW, imgH);

            // 文字の描画
            game.font.getData().setScale(2.0f * scale);
            
            // 文字色も距離に応じてフェードさせる
            Color fontColor = (i == selectedIndex) ? Color.WHITE : Color.LIGHT_GRAY;
            // アルファ値を適用するために新しいColorオブジェクトを作るか、既存の色設定を使う
            // ここでは簡易的に batch の setColor がフォント描画には効かない場合があるので
            // フォント自体の色を設定します
            game.font.setColor(fontColor.r, fontColor.g, fontColor.b, alpha);
            
            // 文字位置
            game.font.draw(game.batch, songs[i], centerX - imgW/5, centerY + 20);
        }
        
        // 色設定を戻す
        game.batch.setColor(1, 1, 1, 1); 
        game.font.setColor(Color.WHITE);

        game.font.getData().setScale(1.5f);
        game.font.draw(game.batch, "[UP/DOWN] Select   [SPACE] Start   [O] Option", 20, 100);

        game.batch.end();

        handleInput();
    }
    
    void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = songs.length - 1; 
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex++;
            if (selectedIndex >= songs.length) selectedIndex = 0; 
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game, songs[selectedIndex])); 
            dispose();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            game.setScreen(new OptionScreen(game)); 
            dispose();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new TitleScreen(game));
            dispose();
        }
    }

    @Override
    public void dispose() {
        if (panelImg != null) panelImg.dispose();
    }
}