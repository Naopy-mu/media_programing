package com.example.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.example.game.BpmEvent;
import com.example.game.Note;

public class NoteManager {
    public Array<Note> notes;
    private int maxComboCount = 0;
    
    // BPM情報とオフセット
    public Array<BpmEvent> bpmEvents = new Array<>();
    public float offset = 0f;

    public NoteManager(String songName, float defaultBpm) {
        // 譜面データ読み込み
        notes = new Array<>();
        loadChart(songName, defaultBpm);
    }

    private void loadChart(String songName, float defaultBpm) {
        // 譜面データ読み込み処理
        FileHandle file = null;
        FileHandle localFile = Gdx.files.local("assets/charts/" + songName + ".json");
        FileHandle internalFile = Gdx.files.internal("charts/" + songName + ".json");

        if (localFile.exists()) file = localFile;
        else if (internalFile.exists()) file = internalFile;
        else {
            createFallbackNotes();
            bpmEvents.add(new BpmEvent(0, defaultBpm));
            calculateMaxCombo();
            return;
        }

        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(file);
            
            // オフセット読み込み
            offset = root.getFloat("offset", 0f);

            JsonValue bpmList = root.get("bpmEvents");
            if (bpmList != null) {
                // BPMイベント読み込み
                for (JsonValue b : bpmList) {
                    float time = b.getFloat("time", 0f);
                    float bpm = b.getFloat("bpm", defaultBpm);
                    bpmEvents.add(new BpmEvent(time, bpm));
                }
            }
            // なければデフォルトBPM
            if (bpmEvents.size == 0) {
                bpmEvents.add(new BpmEvent(0, defaultBpm));
            }

            JsonValue notesList = root.get("notes");
            if (notesList != null) {
                for (JsonValue noteVal : notesList) {
                    // ノーツの時間取得（targetTime優先、なければtime、なければ0）
                    float time = 0;
                    if (noteVal.has("targetTime")) time = noteVal.getFloat("targetTime", 0);
                    else time = noteVal.getFloat("time", 0);

                    // レーン取得（なければ0）
                    int lane = noteVal.getInt("lane", 0);
                    
                    float endTime = 0;
                    boolean isHold = false;

                    if (noteVal.has("isHold")) {
                        isHold = noteVal.getBoolean("isHold", false);
                        if (noteVal.has("endTime")) endTime = noteVal.getFloat("endTime", 0);
                        else endTime = time;
                    } else {
                        endTime = noteVal.getFloat("end", 0);
                        isHold = (endTime > 0);
                    }
                    if (!isHold) endTime = time;

                    notes.add(new Note(time, lane, endTime, isHold));
                }
            }

            notes.sort((o1, o2) -> Float.compare(o1.targetTime, o2.targetTime));
            
            // 時間順にBPMイベントもソート
            bpmEvents.sort((o1, o2) -> Float.compare(o1.time, o2.time));
            
            calculateMaxCombo();
            
        } catch (Exception e) {
            System.err.println("【NoteManager Error】" + e.getMessage());
            e.printStackTrace();
            // エラー時はフォールバック
            createFallbackNotes();
            bpmEvents.clear();
            bpmEvents.add(new BpmEvent(0, defaultBpm));
        }
    }

    private void calculateMaxCombo() {
        // 最大コンボ数計算
        maxComboCount = 0;
        for (Note note : notes) {
            maxComboCount++;
            if (note.isHold) {
                float duration = note.endTime - note.targetTime;
                
                // その時点のBPMを取得して拍数を計算
                float bpmAtStart = getBpmAt(note.targetTime);
                float beatDuration = 60f / bpmAtStart; 
                
                int tickCount = (int)(duration / beatDuration);
                maxComboCount += tickCount;
                maxComboCount++;
            }
        }
    }

    public float getBpmAt(float time) {
        // 指定時間のBPMを取得
        if (bpmEvents.size == 0) return 120f;
        
        float bpm = bpmEvents.first().bpm;
        for (BpmEvent e : bpmEvents) {
            if (e.time <= time) bpm = e.bpm;
            else break;
        }
        return bpm;
    }

    private void createFallbackNotes() {
        // フォールバック譜面（テスト用）
        notes.add(new Note(2.0f, 0));
        notes.add(new Note(2.5f, 1));
        notes.add(new Note(3.0f, 2, 5.0f, true));
        notes.add(new Note(4.0f, 3));
    }
    
    public int getMaxCombo() { return maxComboCount; }// 最大コンボ数取得
    public int getTotalNotes() { return notes.size; }// 総ノーツ数取得

    public void checkMiss(float currentMusicTime, JudgeSystem judgeSystem) {
        // ミス判定処理
        for (Note note : notes) {
            if (!note.active) continue;
            if (note.isHold && note.isHolding) continue;
            
            // ミス判定（判定ラインを通り過ぎたか）
            if (currentMusicTime > note.targetTime + 0.2f) {
                note.active = false;
                judgeSystem.applyResult("MISS");
            }
        }
    }
}