package com.example.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

public class GameConfig {
    public static final int SCREEN_WIDTH = 1920;
    public static final int SCREEN_HEIGHT = 1080;
    
    public static final int LANE_COUNT = 4;
    public static final int[] KEY_MAPPING = {
        Input.Keys.D,
        Input.Keys.F,
        Input.Keys.J,
        Input.Keys.K
    };

    // ★追加：設定保存用の名前
    private static final String PREFS_NAME = "RhythmGamePrefs";

    // ★追加：速度（Speed）を取得する
    public static float getScrollSpeed() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getFloat("speed", 5.0f); // 保存されてなければデフォルト 5.0
    }

    // ★追加：速度（Speed）を保存する
    public static void setScrollSpeed(float value) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        // 範囲制限（1.0 ～ 10.0）
        if (value < 1.0f) value = 1.0f;
        if (value > 10.0f) value = 10.0f;
        
        prefs.putFloat("speed", value);
        prefs.flush(); // 即座に書き込み
    }

    // ★追加：判定調整（Offset）を取得する
    public static float getOffset() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getFloat("offset", 0.0f); // デフォルト 0.0
    }

    // ★追加：判定調整（Offset）を保存する
    public static void setOffset(float value) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat("offset", value);
        prefs.flush();
    }
}