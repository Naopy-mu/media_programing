package com.example.game;

public class Note {
    public float targetTime; // 判定時間（始点）
    public int lane;         // レーン (0-3)
    public boolean active;   // 画面内にあるか

    // ★追加：ホールド用の変数
    public float endTime;    // 終了時間（単押しの場合は0またはtargetTimeと同じ）
    public boolean isHold;   // ホールドノーツかどうか
    public boolean isHolding; // 今現在、押し続けられているか（判定中か）

    // 通常のノーツ用コンストラクタ
    public Note(float targetTime, int lane) {
        this(targetTime, lane, 0, false);
    }

    // ★追加：ホールドノーツ用コンストラクタ
    public Note(float targetTime, int lane, float endTime, boolean isHold) {
        this.targetTime = targetTime;
        this.lane = lane;
        this.active = true;
        this.endTime = endTime;
        this.isHold = isHold;
        this.isHolding = false;
    }
}