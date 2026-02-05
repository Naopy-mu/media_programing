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
    
    // ★追加: リザルト表示用の集計カウンター
    public int maxCombo = 0;
    public int perfectCount = 0; // Perfect!! (Cyan)
    public int greatCount = 0;   // Perfect (Yellow)
    public int goodCount = 0;    // Good
    public int missCount = 0;    // Miss
    
    public float scorePerCombo = 0;
    public int comboStatus = 0; // 0:AP, 1:FC, 2:Normal

    public String message = "";
    public String timingMessage = "";
    public Color messageColor = Color.WHITE;
    public float messageTimer = 0;

    public JudgeSystem(int maxPossibleCombo) {
        if (maxPossibleCombo > 0) {
            scorePerCombo = 1000000f / maxPossibleCombo;
        }
        comboStatus = 0;
        
        // カウンター初期化
        score = 0;
        combo = 0;
        maxCombo = 0;
        perfectCount = 0;
        greatCount = 0;
        goodCount = 0;
        missCount = 0;
    }

    public void update(float deltaTime) {
        if (messageTimer > 0) {
            messageTimer -= deltaTime;
        }
    }

    public Color checkHit(float targetTime, float songPosition) {
        float diff = targetTime - songPosition;
        float absDiff = Math.abs(diff);

        if (absDiff > WINDOW_GOOD) return null;

        timingMessage = (diff > 0) ? "FAST" : "LATE";
        messageTimer = 0.5f;
        
        increaseCombo(); // コンボ加算処理を共通化

        if (absDiff <= WINDOW_THEORY) {
            message = "PERFECT!!";
            messageColor = Color.CYAN;
            timingMessage = ""; 
            score += scorePerCombo; 
            perfectCount++; // ★加算
            return Color.CYAN;
        } else if (absDiff <= WINDOW_PERFECT) {
            message = "PERFECT";
            messageColor = Color.YELLOW;
            score += scorePerCombo;
            greatCount++;   // ★加算
            return Color.YELLOW;
        } else {
            if (comboStatus == 0) comboStatus = 1;
            message = "GOOD";
            messageColor = Color.GREEN;
            score += scorePerCombo * 0.5f;
            goodCount++;    // ★加算
            return Color.GREEN;
        }
    }

    // コンボ処理の共通化
    private void increaseCombo() {
        combo++;
        if (combo > maxCombo) maxCombo = combo; // 最大コンボ更新
    }

    public void addHoldCombo() {
        increaseCombo();
        score += scorePerCombo;
    }

    public void finishHold() {
        increaseCombo();
        score += scorePerCombo;
    }

    public void miss() {
        message = "MISS...";
        timingMessage = "";
        messageColor = Color.GRAY;
        messageTimer = 1.0f;
        combo = 0;
        comboStatus = 2;
        missCount++; // ★加算
    }
    
    public void applyResult(String result) {
        if ("MISS".equals(result)) miss();
    }

    public void resetCombo() {
        if (combo > 0) {
            combo = 0;
            comboStatus = 2;
            missCount++; // 途中離しもミス扱いなら加算
        }
    }
    
    public String getRank() {
        if (score >= 980000) return "S+"; 
        if (score >= 950000) return "S";
        if (score >= 900000) return "A";
        if (score >= 800000) return "B";
        if (score >= 700000) return "C";
        return "D";
    }
}