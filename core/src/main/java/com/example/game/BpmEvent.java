package com.example.game;

public class BpmEvent {
    public float time; // 変わる時間
    public float bpm;  // 新しいBPM

    public BpmEvent() {} // JSON用

    public BpmEvent(float time, float bpm) {
        this.time = time;
        this.bpm = bpm;
    }
}