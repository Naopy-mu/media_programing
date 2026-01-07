package com.example.game; // ← ★いつものパッケージ名に合わせてください！

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

public class ChartLoader {
    
    // CSVファイルを読み込んで、Noteのリストを返すメソッド
    public static Array<Note> loadChart(String fileName) {
        Array<Note> notes = new Array<>();
        
        // assetsフォルダからファイルを読み込む
        FileHandle file = Gdx.files.internal(fileName);
        
        // ファイルの中身を文字列として全部読み込む
        String text = file.readString();
        
        // 改行コードで区切って、1行ごとのデータにする
        String[] lines = text.split("\n"); // Windowsの場合は \r\n かもしれませんが、一旦これで
        
        for (String line : lines) {
            // 空行や余計なスペースを取り除く
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // カンマ "," で区切る
            String[] parts = line.split(",");
            
            if (parts.length >= 2) {
                try {
                    // 1つ目が時間、2つ目がレーン番号
                    float time = Float.parseFloat(parts[0].trim());
                    int lane = Integer.parseInt(parts[1].trim());
                    
                    // ノーツを作ってリストに追加
                    notes.add(new Note(time, lane));
                    
                } catch (NumberFormatException e) {
                    System.out.println("読み込みエラー: " + line);
                }
            }
        }
        
        // 時間順に並べ替えておくと安心（ソート）
        notes.sort((o1, o2) -> Float.compare(o1.targetTime, o2.targetTime));
        
        return notes;
    }
}