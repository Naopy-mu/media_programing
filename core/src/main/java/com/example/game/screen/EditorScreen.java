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
import com.example.game.BpmEvent;
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

    // エディタ設定
    final float LANE_WIDTH = 100f;
    final float LANE_START_X = (GameConfig.SCREEN_WIDTH - LANE_WIDTH * 4) / 2;
    final float PIXELS_PER_SECOND = 300f; 
    
    int snapDivisor = 4; // 4=4分, 8=8分, 16=16分

    Array<BpmEvent> bpmEvents = new Array<>();
    float offset = 0f;

    Array<Note> notes = new Array<>();
    float currentScrollY = 0;
    boolean isPlaying = false;

    enum EditMode { TAP, HOLD }
    EditMode currentMode = EditMode.TAP;
    Note tempHoldStart = null;
    
    // UIボタン（配置を左下に変更）
    Rectangle btnTap, btnHold;

    public EditorScreen(Main game, String songName) {
        this.game = game;
        this.songName = songName;

        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

        music = Gdx.audio.newMusic(Gdx.files.internal(songName + ".mp3"));
        
        // ★UI配置変更: 左下にボタンを配置
        btnTap = new Rectangle(20, 140, 100, 50);
        btnHold = new Rectangle(140, 140, 100, 50);

        loadExistingChart();

        // BPM初期化
        if (bpmEvents.size == 0) {
            float initialBpm = 120f;
            if (songName.equals("Link Layer")) initialBpm = 156;
            else if (songName.equals("Pop!Stack!")) initialBpm = 160;
            else if (songName.equals("Eigenstate")) initialBpm = 174;
            bpmEvents.add(new BpmEvent(0f, initialBpm));
        }
        sortBpmEvents();

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

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        // --- 1. グリッドとレーンの描画 ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        for (int i = 0; i <= 4; i++) {
            float x = LANE_START_X + i * LANE_WIDTH;
            shapeRenderer.line(x, currentScrollY - 1000, x, currentScrollY + GameConfig.SCREEN_HEIGHT + 1000);
        }

        drawDynamicGrid();

        // 現在位置バー
        float nowY = music.getPosition() * PIXELS_PER_SECOND;
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.line(0, nowY, GameConfig.SCREEN_WIDTH, nowY);
        shapeRenderer.end();

        // --- 2. ノーツ描画 ---
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
        
        if (tempHoldStart != null) {
            float x = LANE_START_X + tempHoldStart.lane * LANE_WIDTH;
            float startY = tempHoldStart.targetTime * PIXELS_PER_SECOND;
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            float snapTime = getSnappedTime(touchPos.y);
            float currentY = snapTime * PIXELS_PER_SECOND;
            if (currentY < startY) currentY = startY;
            shapeRenderer.setColor(0, 1, 1, 0.3f);
            shapeRenderer.rect(x + 5, startY, LANE_WIDTH - 10, currentY - startY);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.rect(x + 5, startY, LANE_WIDTH - 10, 20);
        }
        
        // BPMライン
        for (BpmEvent e : bpmEvents) {
            float y = e.time * PIXELS_PER_SECOND;
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.rect(LANE_START_X - 20, y - 2, LANE_WIDTH * 4 + 40, 4);
        }

        shapeRenderer.end();
        
        // --- 3. UI描画 (カメラリセット) ---
        shapeRenderer.setProjectionMatrix(game.batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // ボタン背景
        if (currentMode == EditMode.TAP) shapeRenderer.setColor(Color.GREEN); else shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(btnTap.x, btnTap.y, btnTap.width, btnTap.height);
        
        if (currentMode == EditMode.HOLD) shapeRenderer.setColor(Color.GREEN); else shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(btnHold.x, btnHold.y, btnHold.width, btnHold.height);
        
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 文字情報
        game.batch.begin();
        
        // ボタン文字
        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(1.5f);
        game.font.draw(game.batch, "TAP", btnTap.x + 30, btnTap.y + 35);
        game.font.draw(game.batch, "HOLD", btnHold.x + 25, btnHold.y + 35);

        // --- 左上: 基本情報 ---
        float currentTime = music.getPosition();
        BpmEvent currentBpm = getBpmAt(currentTime);
        
        float uiTop = GameConfig.SCREEN_HEIGHT - 20;
        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "EDIT MODE: " + songName, 20, uiTop);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, String.format("Time: %.3f", currentTime), 20, uiTop - 30);
        game.font.draw(game.batch, "BPM: " + (int)currentBpm.bpm, 20, uiTop - 60);
        game.font.draw(game.batch, "Snap: 1/" + snapDivisor, 20, uiTop - 90);
        game.font.draw(game.batch, String.format("Offset: %.3f", offset), 20, uiTop - 120);

        // --- 右上: 調整操作ガイド ---
        float helpX = GameConfig.SCREEN_WIDTH - 450;
        game.font.setColor(Color.YELLOW);
        game.font.draw(game.batch, "[Left/Right]: Adjust Offset (+/- 0.01)", helpX, uiTop);
        game.font.draw(game.batch, "[Shift + Up/Down]: Adjust BPM (+/- 1)", helpX, uiTop - 30);
        game.font.draw(game.batch, "[B]: Add BPM Change (at current time)", helpX, uiTop - 60);

        // --- 下部: 一般操作ガイド ---
        game.font.setColor(Color.LIGHT_GRAY);
        game.font.draw(game.batch, "[Space]: Play/Pause   [S]: Save   [ESC]: Quit", 20, 100);
        game.font.draw(game.batch, "[1/2/3]: Change Snap (4/8/16)   [Right Click]: Delete", 20, 70);
        game.font.draw(game.batch, "[W / S] or [Wheel]: Scroll", 20, 40);

        // BPM変更点の数値をワールド座標に表示
        for (BpmEvent e : bpmEvents) {
            Vector3 worldPos = new Vector3(LANE_START_X - 80, e.time * PIXELS_PER_SECOND, 0);
            camera.project(worldPos); 
            if (worldPos.y > 0 && worldPos.y < GameConfig.SCREEN_HEIGHT) {
                game.font.setColor(Color.GREEN);
                game.font.getData().setScale(1.2f);
                game.font.draw(game.batch, "BPM " + (int)e.bpm, worldPos.x, worldPos.y);
            }
        }
        
        // ホールド作成中のメッセージ
        if (tempHoldStart != null) {
            game.font.setColor(Color.ORANGE);
            game.font.draw(game.batch, ">> CLICK TO END HOLD <<", 300, 170);
        }

        game.batch.end();
        
        handleInput();
    }

    void drawDynamicGrid() {
        float screenBottomTime = (currentScrollY - 200) / PIXELS_PER_SECOND;
        float screenTopTime = (currentScrollY + GameConfig.SCREEN_HEIGHT + 200) / PIXELS_PER_SECOND;

        float timeIterator = offset;
        int eventIndex = 0;

        while (timeIterator < screenTopTime) {
            BpmEvent currentEvent = bpmEvents.get(eventIndex);
            float currentBpm = currentEvent.bpm;
            
            float nextChangeTime = Float.MAX_VALUE;
            if (eventIndex + 1 < bpmEvents.size) {
                nextChangeTime = bpmEvents.get(eventIndex + 1).time;
            }

            float beatDuration = 60f / currentBpm;
            float gridInterval = beatDuration / (snapDivisor / 4f);

            if (timeIterator < currentEvent.time) timeIterator = currentEvent.time;

            while (timeIterator < nextChangeTime && timeIterator < screenTopTime) {
                if (timeIterator > screenBottomTime) {
                    float y = timeIterator * PIXELS_PER_SECOND;
                    double beatsFromStart = (timeIterator - offset) / beatDuration;
                    boolean isBeat = Math.abs(beatsFromStart - Math.round(beatsFromStart)) < 0.01;

                    if (isBeat) shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0.8f);
                    else shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.3f);

                    shapeRenderer.line(LANE_START_X, y, LANE_START_X + 4 * LANE_WIDTH, y);
                }
                timeIterator += gridInterval;
            }
            eventIndex++;
            if (eventIndex >= bpmEvents.size) break; 
        }
    }

    void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) isPlaying = !isPlaying;
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) saveChart();
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) snapDivisor = 4;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) snapDivisor = 8;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) snapDivisor = 16;
        
        // Offset調整 (左右キー)
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) offset -= 0.01f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) offset += 0.01f;

        // BPM調整 (Shift + 上下キー)
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            BpmEvent e = getBpmAt(music.getPosition());
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) e.bpm += 1;
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) e.bpm -= 1;
        }

        // BPM変更点の追加 (Bキー)
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            float now = music.getPosition();
            BpmEvent existing = null;
            for(BpmEvent e : bpmEvents) {
                if (Math.abs(e.time - now) < 0.1f) existing = e;
            }
            
            if (existing == null) {
                float prevBpm = getBpmAt(now).bpm;
                bpmEvents.add(new BpmEvent(now, prevBpm));
                sortBpmEvents();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
             music.stop();
             game.setScreen(new DevSelectScreen(game));
        }

        // ★修正: 上下キーでのスクロールを廃止し、W/Sキーに変更
        if (!isPlaying) {
            float scrollAmt = 10;
            if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) scrollAmt = 50;

            // 上下矢印は除外しました。代わりにW/Sでスクロール
            if (Gdx.input.isKeyPressed(Input.Keys.W)) currentScrollY += scrollAmt;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) currentScrollY -= scrollAmt;
            
            if (currentScrollY < 0) currentScrollY = 0;
        }
    }

    BpmEvent getBpmAt(float time) {
        BpmEvent target = bpmEvents.first();
        for (BpmEvent e : bpmEvents) {
            if (e.time <= time) target = e;
            else break;
        }
        return target;
    }

    float getSnappedTime(float y) {
        float time = y / PIXELS_PER_SECOND;
        if (time < offset) return offset;

        BpmEvent currentEvent = getBpmAt(time);
        float beatDuration = 60f / currentEvent.bpm;
        float snapInterval = beatDuration / (snapDivisor / 4f);

        float timeFromEvent = time - currentEvent.time;
        float snappedDelta = Math.round(timeFromEvent / snapInterval) * snapInterval;
        
        return currentEvent.time + snappedDelta;
    }
    
    void sortBpmEvents() {
        bpmEvents.sort((o1, o2) -> Float.compare(o1.time, o2.time));
    }

    void loadExistingChart() {
        FileHandle file = Gdx.files.internal("charts/" + songName + ".json");
        FileHandle localFile = Gdx.files.local("assets/charts/" + songName + ".json");
        if (localFile.exists()) file = localFile;

        if (file.exists()) {
            try {
                Json json = new Json();
                ChartData data = json.fromJson(ChartData.class, file);
                if (data != null) {
                    if (data.notes != null) notes.addAll(data.notes);
                    if (data.bpmEvents != null && data.bpmEvents.size > 0) {
                        bpmEvents.clear();
                        bpmEvents.addAll(data.bpmEvents);
                    }
                    offset = data.offset;
                }
            } catch (Exception e) {
                System.out.println("Load error: " + e.getMessage());
            }
        }
    }

    void saveChart() {
        notes.sort((o1, o2) -> Float.compare(o1.targetTime, o2.targetTime));
        sortBpmEvents();

        ChartData data = new ChartData();
        data.notes = notes;
        data.bpmEvents = bpmEvents;
        data.offset = offset;

        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        String text = json.prettyPrint(data);
        FileHandle file = Gdx.files.local("assets/charts/" + songName + ".json");
        file.writeString(text, false);
        System.out.println("Saved with BPM events & Offset.");
    }

    class EditorInputProcessor extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            float uiY = GameConfig.SCREEN_HEIGHT - screenY;
            if (button == Input.Buttons.LEFT) {
                if (btnTap.contains(screenX, uiY)) { currentMode = EditMode.TAP; tempHoldStart = null; return true; }
                if (btnHold.contains(screenX, uiY)) { currentMode = EditMode.HOLD; return true; }
            }

            Vector3 touchPos = new Vector3(screenX, screenY, 0);
            camera.unproject(touchPos);
            int lane = -1;
            if (touchPos.x >= LANE_START_X && touchPos.x < LANE_START_X + 4 * LANE_WIDTH) {
                lane = (int)((touchPos.x - LANE_START_X) / LANE_WIDTH);
            }
            if (lane == -1) return false;

            float snapTime = getSnappedTime(touchPos.y);

            if (button == Input.Buttons.RIGHT) {
                Note target = null;
                for (Note n : notes) {
                    if (n.lane == lane && Math.abs(n.targetTime - snapTime) < 0.05f) target = n;
                    if (n.isHold && n.lane == lane && snapTime >= n.targetTime && snapTime <= n.endTime) target = n;
                }
                if (target != null) notes.removeValue(target, true);
                if (tempHoldStart != null) tempHoldStart = null;
                return true;
            }

            if (button == Input.Buttons.LEFT) {
                if (currentMode == EditMode.TAP) {
                    Note newNote = new Note(snapTime, lane);
                    newNote.isHold = false;
                    newNote.endTime = snapTime;
                    notes.add(newNote);
                } else if (currentMode == EditMode.HOLD) {
                    if (tempHoldStart == null) {
                        tempHoldStart = new Note(snapTime, lane);
                    } else {
                        float endTime = snapTime;
                        if (endTime <= tempHoldStart.targetTime) {
                            tempHoldStart = null;
                        } else {
                            if (tempHoldStart.lane != lane) tempHoldStart.lane = lane;
                            tempHoldStart.isHold = true;
                            tempHoldStart.endTime = endTime;
                            notes.add(tempHoldStart);
                            tempHoldStart = null;
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
        public Array<BpmEvent> bpmEvents;
        public float offset = 0;
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        music.dispose();
    }
}