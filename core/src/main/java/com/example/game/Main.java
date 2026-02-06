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
        // コンストラクタ
        batch = new SpriteBatch();
        assetManager = new AssetManager();

        // 通常フォント
        font = new BitmapFont(); 
        font.getData().setScale(1.5f);

        // ネオンフォント
        assetManager.load("NeonFont_Final.fnt", BitmapFont.class);
        assetManager.finishLoading();
        
        // ネオンフォント取得と設定
        neonFont = assetManager.get("NeonFont_Final.fnt", BitmapFont.class);
        
        // フィルタ設定で滑らかに表示
        neonFont.getData().setScale(0.2f); 
        neonFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        neonFont.setUseIntegerPositions(false);
        neonFont.getData().setLineHeight(neonFont.getData().capHeight * 1.2f);

        // 起動したらすぐタイトル画面へ
        this.setScreen(new TitleScreen(this));
    }

    @Override
    public void render() {
        // 描画処理
        super.render();
    }

    @Override
    public void dispose() {
        // 終了処理
        batch.dispose();
        assetManager.dispose();
        if (font != null) font.dispose();
    }
}