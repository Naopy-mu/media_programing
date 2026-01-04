package com.example.game; // ← 確認したパッケージ名

public class Note {
    public float targetTime; // 判定時間
    public int lane;         // レーン番号
    public boolean active;   // 表示フラグ
    
    public Note(float targetTime, int lane) {
        this.targetTime = targetTime;
        this.lane = lane;
        this.active = true;
    }
}