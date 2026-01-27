package com.example.game.logic;

import com.badlogic.gdx.utils.Array;
import com.example.game.ChartLoader;
import com.example.game.Note;
import java.util.Iterator;

public class NoteManager {
    public Array<Note> notes;
    
    public NoteManager(String songName) {
        try {
            // ★修正：曲名 + ".csv" を読み込む
            notes = ChartLoader.loadChart(songName + ".csv");
        } catch (Exception e) {
            e.printStackTrace(); // エラーがあればログに出す
            notes = new Array<>();
        }
    }

    // MISS判定のために時間をチェックする
    public void checkMiss(float songPosition, JudgeSystem judge) {
        Iterator<Note> iter = notes.iterator();
        while (iter.hasNext()) {
            Note note = iter.next();
            // GOOD判定幅(0.10)を過ぎたらMISS
            if (note.active && songPosition > note.targetTime + 0.10f) {
                note.active = false;
                judge.miss();
            }
        }
    }
    
    public int getTotalNotes() {
        return notes.size;
    }
}