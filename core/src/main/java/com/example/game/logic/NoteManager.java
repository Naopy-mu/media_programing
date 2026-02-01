package com.example.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.example.game.Note;

public class NoteManager {
    public Array<Note> notes;

    public NoteManager(String songName) {
        notes = new Array<>();
        loadChart(songName);
    }

    // JSONファイルから譜面を読み込むメソッド
    private void loadChart(String songName) {
        // ファイルパス: assets/charts/曲名.json
        String filePath = "charts/" + songName + ".json";
        FileHandle file = Gdx.files.internal(filePath);

        if (!file.exists()) {
            System.err.println("【エラー】譜面ファイルが見つかりません: " + filePath);
            // ファイルがない場合はテスト用に適当なノーツを作る（開発用）
            createFallbackNotes();
            return;
        }

        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(file);
            JsonValue notesList = root.get("notes");

            for (JsonValue noteVal : notesList) {
                float time = noteVal.getFloat("time");
                int lane = noteVal.getInt("lane");
                
                // "end" があれば取得、なければ 0 をセット
                float endTime = noteVal.getFloat("end", 0);

                // end が 0 より大きければホールドノーツとみなす
                boolean isHold = (endTime > 0);
                
                // 単押しの場合、便宜上 endTime を targetTime と同じにしておく
                if (!isHold) {
                    endTime = time;
                }

                // ノーツ生成
                notes.add(new Note(time, lane, endTime, isHold));
            }

            // 時間順に並び替え（これをしておかないと判定がバグる）
            notes.sort((o1, o2) -> Float.compare(o1.targetTime, o2.targetTime));

            System.out.println("譜面読み込み完了: " + songName + " (" + notes.size + " notes)");

        } catch (Exception e) {
            System.err.println("【エラー】譜面データの読み込みに失敗しました: " + e.getMessage());
            e.printStackTrace();
            createFallbackNotes();
        }
    }

    // ファイルがなかった時の予備データ（テスト用）
    private void createFallbackNotes() {
        System.out.println("テスト用データを生成します...");
        notes.add(new Note(2.0f, 0));
        notes.add(new Note(2.5f, 1));
        notes.add(new Note(3.0f, 2, 5.0f, true)); // テスト用ホールド
        notes.add(new Note(4.0f, 3));
        notes.sort((o1, o2) -> Float.compare(o1.targetTime, o2.targetTime));
    }
    
    public int getTotalNotes() {
        return notes.size;
    }

    // ミス判定（通り過ぎたノーツを処理）
    public void checkMiss(float currentMusicTime, JudgeSystem judgeSystem) {
        for (Note note : notes) {
            // すでに処理済み(active=false)ならスキップ
            if (!note.active) continue;

            // ホールド中の場合
            if (note.isHold && note.isHolding) {
                // ホールドは押し続けていればミスにはならない
                // (離した瞬間の判定はGameScreenのupdateHoldsで行う)
                continue; 
            }

            // まだ判定されていないノーツのミス判定
            // 判定時間を大幅に過ぎていたらミス
            // (ホールドの場合は「始点」をスルーしたかどうか)
            float missTimeThreshold = 0.2f; // BAD判定幅より過ぎたらミス
            
            if (currentMusicTime > note.targetTime + missTimeThreshold) {
                // ミス確定
                note.active = false;
                judgeSystem.applyResult("MISS");
                System.out.println("MISS: Lane " + note.lane + " Time " + note.targetTime);
            }
        }
    }
}