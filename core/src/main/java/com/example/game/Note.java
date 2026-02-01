package com.example.game;

public class Note {
    public float targetTime;
    public int lane;
    public boolean active;

    public float endTime;    
    public boolean isHold;   
    public boolean isHolding; 

    // ★追加：ホールド時のコンボ計算用タイマー
    public float holdTimer; 

    public Note(float targetTime, int lane) {
        this(targetTime, lane, 0, false);
    }

    public Note(float targetTime, int lane, float endTime, boolean isHold) {
        this.targetTime = targetTime;
        this.lane = lane;
        this.active = true;
        this.endTime = endTime;
        this.isHold = isHold;
        this.isHolding = false;
        this.holdTimer = 0f; // 初期化
    }
}