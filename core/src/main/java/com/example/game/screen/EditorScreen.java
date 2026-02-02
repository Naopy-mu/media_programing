package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.example.game.GameConfig;
import com.example.game.Main;
import com.example.game.Note;

import java.util.Comparator;

public class EditorScreen extends ScreenAdapter {
    final Main game;
    String songName;
    Music music;

    ShapeRenderer shapeRenderer;
    OrthographicCamera camera;

    // --- エディタ設定 ---
    final float LANE_WIDTH = 100f;
    final float LANE_START_X = (GameConfig.SCREEN_WIDTH - LANE_WIDTH * 4) / 2;
    final float PIXELS_PER_SECOND = 300f; 
    
    float bpm = 120f;
    float beatInterval;
    int snapDivisor = 4;

    Array<Note> notes = new Array<>();
    float currentScrollY = 0;
    boolean isPlaying = false;

    // ★追加：編集モード
    enum EditMode { TAP, HOLD }
    EditMode currentMode = EditMode.TAP;

    // ★追加：ホールド作成中データ
    Note tempHoldStart = null; // 始点が置かれている場合、ここにデータが入る

    // ★追加：UIボタンのエリア定義
    Rectangle btnTap, btnHold;

    public EditorScreen(Main game, String songName) {
        this.game = game;
        this.songName = songName;

        shapeRenderer = new ShapeRenderer();
        
        camera = new OrthographicCamera();
        camera.setToOrtho(false, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

        music = Gdx.audio.newMusic(Gdx.files.internal(songName + ".mp3"));
        
        // BPM設定
        if (songName.equals("Link Layer")) bpm = 156;
        else if (songName.equals("Pop!Stack!")) bpm = 160;
        else if (songName.equals("Timepiece Tower")) bpm = 140;
        else if (songName.equals("Eigenstate")) bpm = 174;
        
        beatInterval = 60f / bpm;

        // UIボタンの位置定義（画面上部）
        btnTap = new Rectangle(20, GameConfig.SCREEN_HEIGHT - 150, 100, 50);
        btnHold = new Rectangle(140, GameConfig.SCREEN_HEIGHT - 150, 100, 50);

        loadExistingChart();
        Gdx.input.setInputProcessor(new EditorInputProcessor());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (isPlaying) {
            music.play();
            currentScrollY = music.getPosition() * PIXELS_PER_SECOND;
        } else {
            music.pause();
        }

        camera.position.set(GameConfig.SCREEN_WIDTH / 2f, currentScrollY + GameConfig.SCREEN_HEIGHT / 2f - 200, 0);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        // --- 1. 譜面エリアの描画 ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        // レーン線
        shapeRenderer.setColor(Color.GRAY);
        for (int i = 0; i <= 4; i++) {
            float x = LANE_START_X + i * LANE_WIDTH;
            shapeRenderer.line(x, currentScrollY - 1000, x, currentScrollY + GameConfig.SCREEN_HEIGHT + 1000);
        }
        // グリッド線
        float startSec = (currentScrollY - 1000) / PIXELS_PER_SECOND;
        float endSec = (currentScrollY + GameConfig.SCREEN_HEIGHT) / PIXELS_PER_SECOND;
        float snapInterval = beatInterval / (snapDivisor / 4f); 
        int startIndex = (int)(startSec / snapInterval);
        int endIndex = (int)(endSec / snapInterval) + 1;

        for (int i = startIndex; i <= endIndex; i++) {
            float time = i * snapInterval;
            float y = time * PIXELS_PER_SECOND;
            if (i % (snapDivisor / 4) == 0) shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0.8f);
            else shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.4f);
            shapeRenderer.line(LANE_START_X, y, LANE_START_X + 4 * LANE_WIDTH, y);
        }
        // 現在位置バー
        float nowY = music.getPosition() * PIXELS_PER_SECOND;
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.line(0, nowY, GameConfig.SCREEN_WIDTH, nowY);
        shapeRenderer.end();

        // ノーツ描画
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for (Note note : notes) {
            float x = LANE_START_X + note.lane * LANE_WIDTH;
            float y = note.targetTime * PIXELS_PER_SECOND;
            
            if (note.isHold) {
                float endY = note.endTime * PIXELS_PER_SECOND;
                shapeRenderer.setColor(0, 1, 1, 0.5f);
                shapeRenderer.rect(x + 5, y, LANE_WIDTH - 10, endY - y);
            }
            shapeRenderer.setColor(Color.CYAN);
            shapeRenderer.rect(x + 5, y, LANE_WIDTH - 10, 20);
        }
        
        // ★ホールド作成中のプレビュー（始点〜カーソル位置）
        if (tempHoldStart != null) {
            float x = LANE_START_X + tempHoldStart.lane * LANE_WIDTH;
            float startY = tempHoldStart.targetTime * PIXELS_PER_SECOND;
            
            // マウス位置のY座標を取得してスナップ
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            float snapTime = getSnappedTime(touchPos.y);
            float currentY = snapTime * PIXELS_PER_SECOND;

            // 始点より手前なら始点と同じにする
            if (currentY < startY) currentY = startY;

            // 半透明の帯を描画
            shapeRenderer.setColor(0, 1, 1, 0.3f);
            shapeRenderer.rect(x + 5, startY, LANE_WIDTH - 10, currentY - startY);
            
            // 始点ノーツ
            shapeRenderer.setColor(Color.YELLOW); // 作成中は黄色
            shapeRenderer.rect(x + 5, startY, LANE_WIDTH - 10, 20);
        }
        
        shapeRenderer.end();
        
        // --- 2. UIエリアの描画（カメラの影響を受けないようIdentity Matrixに戻す） ---
        shapeRenderer.setProjectionMatrix(game.batch.getProjectionMatrix());
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // TAPボタン背景
        if (currentMode == EditMode.TAP) shapeRenderer.setColor(Color.GREEN);
        else shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(btnTap.x, btnTap.y, btnTap.width, btnTap.height);

        // HOLDボタン背景
        if (currentMode == EditMode.HOLD) shapeRenderer.setColor(Color.GREEN);
        else shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(btnHold.x, btnHold.y, btnHold.width, btnHold.height);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- 文字描画 ---
        game.batch.begin();
        
        // ボタンの文字
        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(1.5f);
        game.font.draw(game.batch, "TAP", btnTap.x + 25, btnTap.y + 35);
        game.font.draw(game.batch, "HOLD", btnHold.x + 20, btnHold.y + 35);

        // 各種情報
        game.font.draw(game.batch, "EDIT MODE: " + songName, 20, GameConfig.SCREEN_HEIGHT - 20);
        game.font.draw(game.batch, "Time: " + String.format("%.2f", music.getPosition()), 20, GameConfig.SCREEN_HEIGHT - 50);
        
        // ホールド作成中のガイド
        if (tempHoldStart != null) {
            game.font.setColor(Color.YELLOW);
            game.font.draw(game.batch, ">> Select End Point <<", 300, GameConfig.SCREEN_HEIGHT - 130);
        }

        // 操作説明
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "[Space]: Play/Pause  [S]: Save  [Right Click]: Delete", 20, 100);
        game.font.draw(game.batch, "[1/2/3]: Snap (1/4, 1/8, 1/16)", 20, 70);
        game.font.draw(game.batch, "[ESC]: Quit without Saving", 20, 40);

        game.batch.end();
        
        handleInput();
    }

    void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) isPlaying = !isPlaying;
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) saveChart();
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) snapDivisor = 4;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) snapDivisor = 8;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) snapDivisor = 16;
        
        // 開発モードからの脱出
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
             music.stop();
             game.setScreen(new DevSelectScreen(game));
        }

        if (!isPlaying) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) currentScrollY += 10;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) currentScrollY -= 10;
        }
    }

    float getSnappedTime(float y) {
        float rawTime = y / PIXELS_PER_SECOND;
        float snapInterval = beatInterval / (snapDivisor / 4f);
        return Math.round(rawTime / snapInterval) * snapInterval;
    }

    void loadExistingChart() {
        FileHandle file = Gdx.files.internal("charts/" + songName + ".json");
        // ローカル（保存先）にあればそっちを優先
        FileHandle localFile = Gdx.files.local("assets/charts/" + songName + ".json");
        if (localFile.exists()) file = localFile;

        if (file.exists()) {
            try {
                Json json = new Json();
                ChartData data = json.fromJson(ChartData.class, file);
                if (data != null && data.notes != null) {
                    notes.addAll(data.notes);
                }
            } catch (Exception e) {
                System.out.println("No existing chart or parse error");
            }
        }
    }

    void saveChart() {
        notes.sort(new Comparator<Note>() {
            @Override
            public int compare(Note o1, Note o2) {
                return Float.compare(o1.targetTime, o2.targetTime);
            }
        });
        ChartData data = new ChartData();
        data.notes = notes;
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        String text = json.prettyPrint(data);
        FileHandle file = Gdx.files.local("assets/charts/" + songName + ".json");
        file.writeString(text, false);
        System.out.println("Saved to: " + file.file().getAbsolutePath());
    }

    class EditorInputProcessor extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            // Y座標をLibGDX座標系（下から上）に変換
            float uiY = GameConfig.SCREEN_HEIGHT - screenY;

            // --- 1. UIボタン判定 (左クリックのみ) ---
            if (button == Input.Buttons.LEFT) {
                if (btnTap.contains(screenX, uiY)) {
                    currentMode = EditMode.TAP;
                    tempHoldStart = null; // モード変えたらホールド作成キャンセル
                    return true;
                }
                if (btnHold.contains(screenX, uiY)) {
                    currentMode = EditMode.HOLD;
                    return true;
                }
            }

            // --- 2. 譜面エリア判定 ---
            Vector3 touchPos = new Vector3(screenX, screenY, 0);
            camera.unproject(touchPos);

            int lane = -1;
            if (touchPos.x >= LANE_START_X && touchPos.x < LANE_START_X + 4 * LANE_WIDTH) {
                lane = (int)((touchPos.x - LANE_START_X) / LANE_WIDTH);
            }
            if (lane == -1) return false;

            float snapTime = getSnappedTime(touchPos.y);
            if (snapTime < 0) snapTime = 0;

            // 右クリック：削除
            if (button == Input.Buttons.RIGHT) {
                Note target = null;
                for (Note n : notes) {
                    // 単押し or ホールド始点の判定
                    if (n.lane == lane && Math.abs(n.targetTime - snapTime) < 0.1f) target = n;
                    // ホールド中の判定（簡易）
                    if (n.isHold && n.lane == lane && snapTime >= n.targetTime && snapTime <= n.endTime) target = n;
                }
                if (target != null) notes.removeValue(target, true);
                
                // 作成中のキャンセル
                if (tempHoldStart != null) tempHoldStart = null;
                
                return true;
            }

            // 左クリック：配置
            if (button == Input.Buttons.LEFT) {
                if (currentMode == EditMode.TAP) {
                    // タップ配置：即座に追加
                    Note newNote = new Note(snapTime, lane);
                    newNote.isHold = false;
                    newNote.endTime = snapTime;
                    notes.add(newNote);

                } else if (currentMode == EditMode.HOLD) {
                    // ホールド配置：2段階プロセス
                    if (tempHoldStart == null) {
                        // 1回目クリック：始点決定
                        tempHoldStart = new Note(snapTime, lane);
                    } else {
                        // 2回目クリック：終点決定
                        float endTime = snapTime;
                        
                        // 始点より手前ならキャンセル、または入れ替え
                        if (endTime <= tempHoldStart.targetTime) {
                            tempHoldStart = null; // キャンセル
                            System.out.println("Hold cancelled: End time must be after Start time");
                        } else {
                            // 同じレーンである必要あり（仕様によるが今回は固定）
                            if (tempHoldStart.lane != lane) {
                                tempHoldStart.lane = lane; // 終点のレーンに合わせる
                            }
                            
                            tempHoldStart.isHold = true;
                            tempHoldStart.endTime = endTime;
                            notes.add(tempHoldStart);
                            tempHoldStart = null; // リセット
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            if (!isPlaying) {
                currentScrollY -= amountY * 50; 
                if (currentScrollY < 0) currentScrollY = 0;
            }
            return true;
        }
    }

    public static class ChartData {
        public Array<Note> notes;
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        music.dispose();
    }
}