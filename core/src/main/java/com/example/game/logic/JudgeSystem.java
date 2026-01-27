package com.example.game.logic;

import com.example.game.GameConfig;
import com.example.game.Note;
import com.badlogic.gdx.graphics.Color;

public class JudgeSystem {
    // 判定基準
    final float WINDOW_THEORY  = 0.025f;
    final float WINDOW_PERFECT = 0.05f;
    final float WINDOW_GOOD    = 0.10f;

    public float score = 0;
    public int combo = 0;
    public float scorePerNote = 0;
    
    // 表示用メッセージ
    public String message = "";
    public String timingMessage = "";
    public Color messageColor = Color.WHITE;
    public float messageTimer = 0;

    public JudgeSystem(int totalNotes) {
        if (totalNotes > 0) {
            scorePerNote = 1000000f / totalNotes;
        }
    }

    public void update(float deltaTime) {
        if (messageTimer > 0) {
            messageTimer -= deltaTime;
        }
    }

    // 判定処理を行い、結果（エフェクトの色）を返す。判定外ならnull
    public Color checkHit(float targetTime, float songPosition) {
        float diff = targetTime - songPosition;
        float absDiff = Math.abs(diff);

        if (absDiff > WINDOW_GOOD) return null; // 判定外

        // FAST/LATE判定
        timingMessage = (diff > 0) ? "FAST" : "LATE";
        messageTimer = 0.5f;
        combo++;

        if (absDiff <= WINDOW_THEORY) {
            message = "PERFECT!!";
            messageColor = Color.CYAN;
            timingMessage = ""; // 理論値は消す
            score += scorePerNote + 1.0f;
            return Color.CYAN;
        } else if (absDiff <= WINDOW_PERFECT) {
            message = "PERFECT";
            messageColor = Color.YELLOW;
            score += scorePerNote * 1.0f;
            return Color.YELLOW;
        } else {
            message = "GOOD";
            messageColor = Color.GREEN;
            score += scorePerNote * 0.5f;
            return Color.GREEN;
        }
    }

    public void miss() {
        message = "MISS...";
        timingMessage = "";
        messageColor = Color.GRAY;
        messageTimer = 1.0f;
        combo = 0;
    }
    
    // ランク計算用
    public String getRank() {
        if (score >= 1000000) return "SSS";
        if (score >= 900000) return "S";
        if (score >= 800000) return "A";
        if (score >= 700000) return "B";
        return "C";
    }
}