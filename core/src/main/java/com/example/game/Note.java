package com.example.game;

public class Note {
    public float targetTime;
    public int lane;
    public boolean active;

    public float endTime;    
    public boolean isHold;   
    public boolean isHolding; 

    // ホールド時のコンボ計算用タイマー
    public float holdTimer; 

    public Note(float targetTime, int lane) {
        // 通常ノート用コンストラクタ
        this(targetTime, lane, 0, false);
    }

    public Note(float targetTime, int lane, float endTime, boolean isHold) {
        // コンストラクタ
        this.targetTime = targetTime;// ノーツの時間
        this.lane = lane;// レーン
        this.active = true;// アクティブ状態
        this.endTime = endTime;// ホールド終了時間
        this.isHold = isHold;// ホールドノートか
        this.isHolding = false;// ホールド中か
        this.holdTimer = 0f; // 初期化
    }

    // JSON読み書き用（必須）
    public Note() {}
}