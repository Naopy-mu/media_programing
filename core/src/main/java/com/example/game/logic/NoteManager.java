package com.example.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.example.game.Note;

public class NoteManager {
    public Array<Note> notes;
    private int maxComboCount = 0; // 計算された最大コンボ数

    // コンストラクタ：BPMを受け取る
    public NoteManager(String songName, float bpm) {
        notes = new Array<>();
        loadChart(songName, bpm);
    }

    private void loadChart(String songName, float bpm) {
        String filePath = "charts/" + songName + ".json";
        float beatDuration = 60f / bpm; // 1拍の時間

        try {
            FileHandle file = Gdx.files.internal(filePath);

            // ファイルがない場合の処理
            if (!file.exists()) {
                System.err.println("【警告】譜面ファイルが見つかりません: " + filePath);
                createFallbackNotes();
                calculateMaxCombo(beatDuration);
                return;
            }

            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(file);
            JsonValue notesList = root.get("notes");

            if (notesList != null) {
                for (JsonValue noteVal : notesList) {
                    float time = noteVal.getFloat("time");
                    int lane = noteVal.getInt("lane");
                    float endTime = noteVal.getFloat("end", 0);
                    
                    boolean isHold = (endTime > 0);
                    if (!isHold) endTime = time;

                    notes.add(new Note(time, lane, endTime, isHold));
                }
            }

            // 時間順にソート
            notes.sort((o1, o2) -> Float.compare(o1.targetTime, o2.targetTime));
            
            // 読み込み完了後に最大コンボ数を計算
            calculateMaxCombo(beatDuration);
            System.out.println("Chart Loaded: " + songName + " / MaxCombo: " + maxComboCount);

        } catch (Exception e) {
            System.err.println("【エラー】譜面読み込み失敗: " + e.getMessage());
            createFallbackNotes();
            calculateMaxCombo(beatDuration);
        }
    }

    // 理論上の最大コンボ数を計算する
    private void calculateMaxCombo(float beatDuration) {
        maxComboCount = 0;
        for (Note note : notes) {
            // 1. 始点（タップ判定）で +1
            maxComboCount++;

            if (note.isHold) {
                // ホールド時間の長さ
                float duration = note.endTime - note.targetTime;
                
                // 2. ホールド中のコンボ数（拍数）
                // 例: 2.5拍なら intキャストで 2回加算
                int tickCount = (int)(duration / beatDuration);
                maxComboCount += tickCount;

                // 3. 終点（完走判定）で +1
                maxComboCount++;
            }
        }
    }

    // テスト用データ生成
    private void createFallbackNotes() {
        System.out.println("テスト用データを生成します...");
        notes.add(new Note(2.0f, 0));
        notes.add(new Note(2.5f, 1));
        notes.add(new Note(3.0f, 2, 5.0f, true)); // ホールド
        notes.add(new Note(4.0f, 3));
        notes.sort((o1, o2) -> Float.compare(o1.targetTime, o2.targetTime));
    }
    
    // 最大コンボ数を返す（JudgeSystemの初期化に使用）
    public int getMaxCombo() {
        return maxComboCount;
    }
    
    public int getTotalNotes() {
        return notes.size;
    }

    // ミス判定処理
    public void checkMiss(float currentMusicTime, JudgeSystem judgeSystem) {
        for (Note note : notes) {
            if (!note.active) continue;
            // ホールド中はミス判定しない
            if (note.isHold && note.isHolding) continue;

            float missTimeThreshold = 0.2f; 
            
            if (currentMusicTime > note.targetTime + missTimeThreshold) {
                note.active = false;
                judgeSystem.applyResult("MISS");
            }
        }
    }
}