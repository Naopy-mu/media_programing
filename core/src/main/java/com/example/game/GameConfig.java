package com.example.game;  // ← これがあなたのパッケージ名です！;

import com.badlogic.gdx.Input;

public class GameConfig {
    // 画面サイズ
    public static final float SCREEN_WIDTH = 640;
    public static final float SCREEN_HEIGHT = 480;

    // レーンの設定
    public static final int LANE_COUNT = 4;        // 4レーン
    public static final float LANE_WIDTH = 100;    // 1レーンの幅
    
    // レーン全体の開始位置
    public static final float LANE_START_X = (SCREEN_WIDTH - (LANE_WIDTH * LANE_COUNT)) / 2;

    // 判定ラインの高さ
    public static final float JUDGEMENT_LINE_Y = 100;

    // ノーツが落ちる速さ
    public static final float NOTE_SPEED = 400;

    // キー配置（D, F, J, K）
    public static final int[] KEY_MAPPING = {
        Input.Keys.D,
        Input.Keys.F,
        Input.Keys.J,
        Input.Keys.K
    };
}