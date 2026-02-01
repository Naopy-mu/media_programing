package com.example.game.logic;

import com.example.game.GameConfig;
import com.example.game.Note;
import com.badlogic.gdx.graphics.Color;

public class JudgeSystem {
    // 判定基準 (変更なし)
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

    // 判定処理 (変更なし)
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
    
    // ランク計算用 (変更なし)
    public String getRank() {
        if (score >= 1000000) return "SSS";
        if (score >= 900000) return "S";
        if (score >= 800000) return "A";
        if (score >= 700000) return "B";
        return "C";
    }

    // ========================================================
    // ★以下、エラー解消とホールド機能のために追加したメソッド
    // ========================================================

    // NoteManagerのエラー解消用: 文字列で結果を受け取って既存のmiss()を呼ぶ
    public void applyResult(String result) {
        if ("MISS".equals(result)) {
            miss();
        }
    }

    // GameScreenのエラー解消用: ホールドを離した時にコンボを切る
    public void resetCombo() {
        combo = 0;
        // メッセージを出したくない場合はここを空にするだけでもOK
        // message = "LOST"; 
        // messageColor = Color.GRAY;
        // messageTimer = 0.5f;
    }

    // ホールド押し続け中の加点（1フレームごとの微小加点）
    public void addHoldScore() {
        // 例: 1フレームにつき 10点 加算（バランスは調整してください）
        score += 10;
        // 上限を超えないようにするならここでチェック
        if (score > 1000000 + totalNotesBonus()) score = 1000000 + totalNotesBonus();
    }
    
    // 理論値ボーナス分の計算用ヘルパー（上限チェック用）
    private float totalNotesBonus() {
        return (1000000f / scorePerNote); 
    }
}