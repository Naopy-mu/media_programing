package com.example.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.example.game.BpmEvent; // 追加
import com.example.game.Note;

public class NoteManager {
    public Array<Note> notes;
    private int maxComboCount = 0;
    
    // ★追加: BPM情報とオフセット
    public Array<BpmEvent> bpmEvents = new Array<>();
    public float offset = 0f;

    public NoteManager(String songName, float defaultBpm) {
        notes = new Array<>();
        loadChart(songName, defaultBpm);
    }

    private void loadChart(String songName, float defaultBpm) {
        FileHandle file = null;
        FileHandle localFile = Gdx.files.local("assets/charts/" + songName + ".json");
        FileHandle internalFile = Gdx.files.internal("charts/" + songName + ".json");

        if (localFile.exists()) file = localFile;
        else if (internalFile.exists()) file = internalFile;
        else {
            createFallbackNotes();
            // BPMイベントがない場合はデフォルトを1つ入れる
            bpmEvents.add(new BpmEvent(0, defaultBpm));
            calculateMaxCombo();
            return;
        }

        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(file);
            
            // ★オフセット読み込み
            offset = root.getFloat("offset", 0f);

            // ★BPMイベント読み込み
            JsonValue bpmList = root.get("bpmEvents");
            if (bpmList != null) {
                for (JsonValue b : bpmList) {
                    bpmEvents.add(new BpmEvent(b.getFloat("time"), b.getFloat("bpm")));
                }
            }
            // なければデフォルトBPM
            if (bpmEvents.size == 0) {
                bpmEvents.add(new BpmEvent(0, defaultBpm));
            }

            JsonValue notesList = root.get("notes");
            if (notesList != null) {
                for (JsonValue noteVal : notesList) {
                    float time = 0;
                    if (noteVal.has("targetTime")) time = noteVal.getFloat("targetTime", 0);
                    else time = noteVal.getFloat("time", 0);

                    // ★オフセット適用（エディタではそのまま保存するが、ゲームでは再生位置と合わせるため適用済みとして扱うか、
                    //   あるいは描画側で引くか。ここではGameScreenで userOffset + offset するのが一般的）
                    //   今回はデータをそのまま保持します。

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
            e.printStackTrace();
            createFallbackNotes();
            bpmEvents.add(new BpmEvent(0, defaultBpm));
        }
    }

    // ★可変BPM対応のコンボ計算
    private void calculateMaxCombo() {
        maxComboCount = 0;
        for (Note note : notes) {
            maxComboCount++;
            if (note.isHold) {
                float duration = note.endTime - note.targetTime;
                
                // その時点のBPMを取得して拍数を計算
                float bpmAtStart = getBpmAt(note.targetTime);
                float beatDuration = 60f / bpmAtStart; // 簡易的に始点のBPMを使用
                
                int tickCount = (int)(duration / beatDuration);
                maxComboCount += tickCount;
                maxComboCount++;
            }
        }
    }

    // 指定時間のBPMを取得
    public float getBpmAt(float time) {
        float bpm = bpmEvents.first().bpm;
        for (BpmEvent e : bpmEvents) {
            if (e.time <= time) bpm = e.bpm;
            else break;
        }
        return bpm;
    }

    private void createFallbackNotes() {
        notes.add(new Note(2.0f, 0));
        notes.add(new Note(2.5f, 1));
        notes.add(new Note(3.0f, 2, 5.0f, true));
        notes.add(new Note(4.0f, 3));
    }
    
    public int getMaxCombo() { return maxComboCount; }
    public int getTotalNotes() { return notes.size; }

    public void checkMiss(float currentMusicTime, JudgeSystem judgeSystem) {
        for (Note note : notes) {
            if (!note.active) continue;
            if (note.isHold && note.isHolding) continue;
            if (currentMusicTime > note.targetTime + 0.2f) {
                note.active = false;
                judgeSystem.applyResult("MISS");
            }
        }
    }
}