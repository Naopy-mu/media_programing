package com.example.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.example.game.screen.TitleScreen;

public class Main extends Game {
    public SpriteBatch batch;
    public BitmapFont font;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        
        // 最初の画面（タイトル）をセット
        setScreen(new TitleScreen(this));
    }

    @Override
    public void render() {
        // 現在セットされているScreenのrenderを呼び出す
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}