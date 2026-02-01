package com.example.game.logic;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class JudgeSystem {
    // 判定基準
    final float WINDOW_THEORY  = 0.025f;
    final float WINDOW_PERFECT = 0.05f;
    final float WINDOW_GOOD    = 0.10f;

    public float score = 0;
    public int combo = 0;
    
    // 1コンボあたりのスコア（1,000,000 / 最大コンボ数）
    public float scorePerCombo = 0;
    
    // 0: All Perfect, 1: Full Combo, 2: Normal
    public int comboStatus = 0;

    // 表示用メッセージ
    public String message = "";
    public String timingMessage = "";
    public Color messageColor = Color.WHITE;
    public float messageTimer = 0;

    // コンストラクタ：最大コンボ数を受け取る
    public JudgeSystem(int maxPossibleCombo) {
        if (maxPossibleCombo > 0) {
            scorePerCombo = 1000000f / maxPossibleCombo;
        }
        comboStatus = 0;
    }

    public void update(float deltaTime) {
        if (messageTimer > 0) {
            messageTimer -= deltaTime;
        }
    }

    // タップ時の判定
    public Color checkHit(float targetTime, float songPosition) {
        float diff = targetTime - songPosition;
        float absDiff = Math.abs(diff);

        if (absDiff > WINDOW_GOOD) return null; // 判定外

        timingMessage = (diff > 0) ? "FAST" : "LATE";
        messageTimer = 0.5f;
        
        // コンボ加算（始点）
        addCombo();

        if (absDiff <= WINDOW_THEORY) {
            message = "PERFECT!!";
            messageColor = Color.CYAN;
            timingMessage = ""; 
            score += scorePerCombo; 
            return Color.CYAN;
        } else if (absDiff <= WINDOW_PERFECT) {
            message = "PERFECT";
            messageColor = Color.YELLOW;
            score += scorePerCombo;
            return Color.YELLOW;
        } else {
            if (comboStatus == 0) comboStatus = 1; // AP -> FC
            message = "GOOD";
            messageColor = Color.GREEN;
            score += scorePerCombo * 0.5f; // GOODは半分
            return Color.GREEN;
        }
    }

    // 単純なコンボ加算
    public void addCombo() {
        combo++;
    }

    // ホールド中の1拍ごとの加算（コンボ+1, スコア満点）
    public void addHoldCombo() {
        combo++;
        score += scorePerCombo;
    }

    // ホールド完走時の加算（コンボ+1, スコア満点）
    public void finishHold() {
        combo++;
        score += scorePerCombo;
    }

    // ミス処理
    public void miss() {
        message = "MISS...";
        timingMessage = "";
        messageColor = Color.GRAY;
        messageTimer = 1.0f;
        combo = 0;
        comboStatus = 2; // Normalへ
    }
    
    // 外部からの結果適用
    public void applyResult(String result) {
        if ("MISS".equals(result)) {
            miss();
        }
    }

    // コンボリセット（ホールドを離した時など）
    public void resetCombo() {
        if (combo > 0) {
            combo = 0;
            comboStatus = 2;
        }
    }
    
    // 廃止されたメソッド（互換性のために空で残すか削除）
    public void addHoldScore() {
        // 何もしない
    }
    
    // ランク計算（浮動小数点の誤差を考慮して少し甘めに）
    public String getRank() {
        if (score >= 999950) return "SSS"; 
        if (score >= 900000) return "S";
        if (score >= 800000) return "A";
        if (score >= 700000) return "B";
        return "C";
    }
}