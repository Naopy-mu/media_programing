package com.example.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.assets.AssetManager;
import com.example.game.screen.TitleScreen; // ★修正: 最初はTitleScreenへ

public class Main extends Game {
    public SpriteBatch batch;
    
    public BitmapFont font;      
    public BitmapFont neonFont;  
    
    public AssetManager assetManager;

    @Override
    public void create() {
        batch = new SpriteBatch();
        assetManager = new AssetManager();

        // 1. 通常フォント
        font = new BitmapFont(); 
        font.getData().setScale(1.5f);

        // 2. ネオンフォント (タイトル表示に必要なので起動時に読み込む)
        assetManager.load("NeonFont_Final.fnt", BitmapFont.class);
        assetManager.finishLoading(); // ここで完了まで待つ
        
        neonFont = assetManager.get("NeonFont_Final.fnt", BitmapFont.class);
        
        neonFont.getData().setScale(0.2f); 
        neonFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        neonFont.setUseIntegerPositions(false);
        neonFont.getData().setLineHeight(neonFont.getData().capHeight * 1.2f);

        // ★修正: 起動したらすぐタイトル画面へ
        this.setScreen(new TitleScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        assetManager.dispose();
        if (font != null) font.dispose();
    }
}