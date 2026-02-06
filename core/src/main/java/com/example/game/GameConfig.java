package com.example.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

public class GameConfig {
    public static final int SCREEN_WIDTH = 1920;
    public static final int SCREEN_HEIGHT = 1080;
    
    public static final int LANE_COUNT = 4;
    public static final int[] KEY_MAPPING = {
        // 変更済み：キー配置
        Input.Keys.D,
        Input.Keys.F,
        Input.Keys.J,
        Input.Keys.K
    };

    // 設定保存用の名前
    private static final String PREFS_NAME = "RhythmGamePrefs";

    // 速度（Speed）を取得
    public static float getScrollSpeed() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getFloat("speed", 5.0f); // 保存されてなければデフォルト 5.0
    }

    // 速度（Speed）を保存
    public static void setScrollSpeed(float value) {
        // 変更済み：速度保存処理
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        if (value < 1.0f) value = 1.0f;
        if (value > 10.0f) value = 10.0f;
        
        prefs.putFloat("speed", value);
        prefs.flush();
    }

    // 判定調整（Offset）を取得
    public static float getOffset() {
        // 変更済み：判定調整取得処理
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getFloat("offset", 0.0f);
    }

    // 判定調整（Offset）を保存
    public static void setOffset(float value) {
        // 変更済み：判定調整保存処理
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat("offset", value);
        prefs.flush();
    }
}