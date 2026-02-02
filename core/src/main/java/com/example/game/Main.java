package com.example.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager; // ★追加
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.example.game.screen.TitleScreen;

public class Main extends Game {
    public SpriteBatch batch;
    public BitmapFont font;
    
    // ★追加: 画像や音声を管理するマネージャー
    public AssetManager assetManager;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        
        // ★追加: マネージャーの初期化
        assetManager = new AssetManager();

        this.setScreen(new TitleScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        // ★追加: アプリ終了時にマネージャーも破棄
        assetManager.dispose();
    }
}