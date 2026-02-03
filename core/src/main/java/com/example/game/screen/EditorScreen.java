package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
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
    
    Sound hitSound;
    Sound deleteSound; 

    ShapeRenderer shapeRenderer;
    OrthographicCamera camera;

    final float LANE_WIDTH = 100f;
    final float LANE_START_X = (GameConfig.SCREEN_WIDTH - LANE_WIDTH * 4) / 2;
    final float PIXELS_PER_SECOND = 300f; 
    
    int snapDivisor = 4;

    Array<BpmEvent> bpmEvents = new Array<>();
    float offset = 0f;

    Array<Note> notes = new Array<>();
    float currentScrollY = 0;
    boolean isPlaying = false;
    
    float smoothTime = 0;
    float lastHitCheckTime = 0;
    float lastFrameTime = 0;

    boolean isMetronomeOn = false;

    enum EditMode { TAP, HOLD }
    EditMode currentMode = EditMode.TAP;
    Note tempHoldStart = null;
    
    Rectangle btnTap, btnHold;

    public EditorScreen(Main game, String songName) {
        this.game = game;
        this.songName = songName;

        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

        music = Gdx.audio.newMusic(Gdx.files.internal(songName + ".mp3"));
        
        if (game.assetManager.isLoaded("hit.mp3")) {
            hitSound = game.assetManager.get("hit.mp3", Sound.class);
        } else {
            try { hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.mp3")); } catch(Exception e){}
        }
        
        if (game.assetManager.isLoaded("count.mp3")) {
            deleteSound = game.assetManager.get("count.mp3", Sound.class);
        } else {
            try {
                deleteSound = Gdx.audio.newSound(Gdx.files.internal("count.mp3"));
            } catch (Exception e) {
                deleteSound = hitSound; 
            }
        }
        
        btnTap = new Rectangle(20, 140, 100, 50);
        btnHold = new Rectangle(140, 140, 100, 50);

        loadExistingChart();

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
            if (!music.isPlaying()) {
                isPlaying = false;
            } else {
                smoothTime += delta;
                float rawPosition = music.getPosition();
                
                if (Math.abs(smoothTime - rawPosition) > 0.05f) {
                    smoothTime = rawPosition;
                }

                // ノーツ音再生
                // lastHitCheckTime(前回) と smoothTime(今回) の間にあるノーツを探す
                // ※ノーツの時間は「オフセットが引かれた状態（譜面時間）」なので、
                //   判定する際は 再生時間からオフセットを引いて比較する必要があります。
                float currentChartTime = smoothTime - offset;
                float prevChartTime = lastHitCheckTime - offset;

                for (Note note : notes) {
                    // note.targetTime は譜面時間 (0秒~)
                    if (note.targetTime > prevChartTime && note.targetTime <= currentChartTime) {
                        if (hitSound != null) hitSound.play(0.5f);
                    }
                }
                
                // メトロノーム再生
                if (isMetronomeOn) {
                    playMetronome(lastFrameTime, smoothTime);
                }
                
                lastHitCheckTime = smoothTime;
                lastFrameTime = smoothTime;
                currentScrollY = smoothTime * PIXELS_PER_SECOND;
            }
        } else {
            if (music.isPlaying()) music.pause();
            smoothTime = currentScrollY / PIXELS_PER_SECOND;
            lastHitCheckTime = smoothTime;
            lastFrameTime = smoothTime;
        }

        camera.position.set(GameConfig.SCREEN_WIDTH / 2f, currentScrollY + GameConfig.SCREEN_HEIGHT / 2f - 200, 0);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        // グリッドとレーン
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        for (int i = 0; i <= 4; i++) {
            float x = LANE_START_X + i * LANE_WIDTH;
            shapeRenderer.line(x, currentScrollY - 1000, x, currentScrollY + GameConfig.SCREEN_HEIGHT + 1000);
        }
        drawDynamicGrid();

        // 現在位置バー
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.line(0, currentScrollY, GameConfig.SCREEN_WIDTH, currentScrollY);
        shapeRenderer.end();

        // ノーツ
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Note note : notes) {
            float x = LANE_START_X + note.lane * LANE_WIDTH;
            
            // ノーツの描画位置： (ターゲット時間 + オフセット) * スケール
            // 譜面上の 0秒 は、画面上では offset秒 の位置にあるべき
            float y = (note.targetTime + offset) * PIXELS_PER_SECOND;
            
            if (note.isHold) {
                float endY = (note.endTime + offset) * PIXELS_PER_SECOND;
                shapeRenderer.setColor(0, 1, 1, 0.5f);
                shapeRenderer.rect(x + 5, y, LANE_WIDTH - 10, endY - y);
            }
            shapeRenderer.setColor(Color.CYAN);
            shapeRenderer.rect(x + 5, y, LANE_WIDTH - 10, 20);
        }
        
        if (tempHoldStart != null) {
            float x = LANE_START_X + tempHoldStart.lane * LANE_WIDTH;
            float startY = (tempHoldStart.targetTime + offset) * PIXELS_PER_SECOND;
            
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            
            // スナップ計算時はオフセットを引いて譜面時間を算出する
            float snapChartTime = getSnappedChartTime(touchPos.y);
            float currentY = (snapChartTime + offset) * PIXELS_PER_SECOND;
            
            if (currentY < startY) currentY = startY;
            shapeRenderer.setColor(0, 1, 1, 0.3f);
            shapeRenderer.rect(x + 5, startY, LANE_WIDTH - 10, currentY - startY);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.rect(x + 5, startY, LANE_WIDTH - 10, 20);
        }
        
        // BPMライン
        for (BpmEvent e : bpmEvents) {
            float y = (e.time + offset) * PIXELS_PER_SECOND;
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.rect(LANE_START_X - 20, y - 2, LANE_WIDTH * 4 + 40, 4);
        }
        shapeRenderer.end();
        
        // UI
        shapeRenderer.setProjectionMatrix(game.batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        if (currentMode == EditMode.TAP) shapeRenderer.setColor(Color.GREEN); else shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(btnTap.x, btnTap.y, btnTap.width, btnTap.height);
        
        if (currentMode == EditMode.HOLD) shapeRenderer.setColor(Color.GREEN); else shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(btnHold.x, btnHold.y, btnHold.width, btnHold.height);
        
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.batch.begin();
        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(1.5f);
        game.font.draw(game.batch, "TAP", btnTap.x + 30, btnTap.y + 35);
        game.font.draw(game.batch, "HOLD", btnHold.x + 25, btnHold.y + 35);

        float currentMusicTime = currentScrollY / PIXELS_PER_SECOND;
        float currentChartTime = currentMusicTime - offset; // 譜面上の時間
        BpmEvent currentBpm = getBpmAt(currentChartTime);
        
        float uiTop = GameConfig.SCREEN_HEIGHT - 20;
        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "EDIT MODE: " + songName, 20, uiTop);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, String.format("Music Time: %.3f", currentMusicTime), 20, uiTop - 30);
        game.font.draw(game.batch, String.format("Chart Time: %.3f", currentChartTime), 20, uiTop - 60);
        game.font.draw(game.batch, "BPM: " + (int)currentBpm.bpm, 20, uiTop - 90);
        game.font.draw(game.batch, "Snap: 1/" + snapDivisor, 20, uiTop - 120);
        game.font.draw(game.batch, String.format("Offset: %.3f", offset), 20, uiTop - 150);
        
        if (isMetronomeOn) {
            game.font.setColor(Color.GREEN);
            game.font.draw(game.batch, "Metronome: ON [M]", 20, uiTop - 180);
        } else {
            game.font.setColor(Color.GRAY);
            game.font.draw(game.batch, "Metronome: OFF [M]", 20, uiTop - 180);
        }

        float helpX = GameConfig.SCREEN_WIDTH - 450;
        game.font.setColor(Color.YELLOW);
        game.font.draw(game.batch, "[Left/Right]: Rewind/Forward (1s)", helpX, uiTop);
        game.font.draw(game.batch, "[Shift + L/R]: Fast Seek (5s)", helpX, uiTop - 30);
        game.font.draw(game.batch, "[O / P]: Adjust Offset (+/- 0.01)", helpX, uiTop - 60);
        game.font.draw(game.batch, "[Shift + Up/Down]: Adjust BPM", helpX, uiTop - 90);
        game.font.draw(game.batch, "[B]: Add BPM Change", helpX, uiTop - 120);

        game.font.setColor(Color.LIGHT_GRAY);
        game.font.draw(game.batch, "[Space]: Play/Pause   [S]: Save   [ESC]: Quit", 20, 100);
        game.font.draw(game.batch, "[1/2/3]: Change Snap   [Right Click]: Delete", 20, 70);
        game.font.draw(game.batch, "[W / S] or [Wheel]: Scroll", 20, 40);

        for (BpmEvent e : bpmEvents) {
            Vector3 worldPos = new Vector3(LANE_START_X - 80, (e.time + offset) * PIXELS_PER_SECOND, 0);
            camera.project(worldPos); 
            if (worldPos.y > 0 && worldPos.y < GameConfig.SCREEN_HEIGHT) {
                game.font.setColor(Color.GREEN);
                game.font.getData().setScale(1.2f);
                game.font.draw(game.batch, "BPM " + (int)e.bpm, worldPos.x, worldPos.y);
            }
        }
        
        if (tempHoldStart != null) {
            game.font.setColor(Color.ORANGE);
            game.font.draw(game.batch, ">> CLICK TO END HOLD <<", 300, 170);
        }

        game.batch.end();
        
        handleInput();
    }

    // ★修正: メトロノーム同期ロジック（完全版）
    void playMetronome(float prevTime, float currTime) {
        // 現在の「譜面上の時間」に変換してから計算する
        float currChartTime = currTime - offset;
        float prevChartTime = prevTime - offset;

        // オフセットより前（曲は流れているが譜面は始まっていない）なら鳴らさない
        if (currChartTime < 0) return;

        BpmEvent event = getBpmAt(currChartTime);
        float beatDuration = 60f / event.bpm;
        
        // BPM変更点からの経過時間
        float timeFromEvent = currChartTime - event.time;
        float prevTimeFromEvent = prevChartTime - event.time;
        
        if (timeFromEvent < 0) return;

        // 拍インデックスの変化を検知
        int currentBeatIndex = (int)(timeFromEvent / beatDuration);
        int prevBeatIndex = (int)(prevTimeFromEvent / beatDuration);

        // 拍が進んだら音を鳴らす
        if (currentBeatIndex > prevBeatIndex) {
            if (hitSound != null) {
                hitSound.play(0.3f, 2.0f, 0); 
            }
        }
    }

    // ★修正: グリッド描画もオフセットを基準にする
    void drawDynamicGrid() {
        float screenBottomY = currentScrollY - 200;
        float screenTopY = currentScrollY + GameConfig.SCREEN_HEIGHT + 200;

        // 譜面時間でのループ開始位置
        float chartTimeIterator = 0;
        int eventIndex = 0;

        // 譜面の終わりまで（十分大きな値まで）ループ
        // 実際には画面外は描画しないのでループは回るが描画負荷は低い
        // ただし無限ループ防止のため画面上端の時間+予備までを上限とする
        float maxChartTime = (screenTopY / PIXELS_PER_SECOND) - offset;
        if (maxChartTime < 0) return; // まだ譜面エリアが見えてない

        while (chartTimeIterator < maxChartTime) {
            BpmEvent currentEvent = bpmEvents.get(eventIndex);
            float currentBpm = currentEvent.bpm;
            
            float nextChangeChartTime = Float.MAX_VALUE;
            if (eventIndex + 1 < bpmEvents.size) {
                nextChangeChartTime = bpmEvents.get(eventIndex + 1).time;
            }

            float beatDuration = 60f / currentBpm;
            float gridInterval = beatDuration / (snapDivisor / 4f);

            // このイベントの開始時間からスタート
            if (chartTimeIterator < currentEvent.time) chartTimeIterator = currentEvent.time;

            while (chartTimeIterator < nextChangeChartTime && chartTimeIterator < maxChartTime) {
                // 描画位置Y = (譜面時間 + オフセット) * スケール
                float y = (chartTimeIterator + offset) * PIXELS_PER_SECOND;

                if (y > screenBottomY) {
                    // 1拍判定
                    // イベント開始からの経過時間を1拍で割って整数に近いか
                    double beatsFromEvent = (chartTimeIterator - currentEvent.time) / beatDuration;
                    boolean isBeat = Math.abs(beatsFromEvent - Math.round(beatsFromEvent)) < 0.01;

                    if (isBeat) shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0.8f);
                    else shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.3f);

                    shapeRenderer.line(LANE_START_X, y, LANE_START_X + 4 * LANE_WIDTH, y);
                }
                chartTimeIterator += gridInterval;
            }
            eventIndex++;
            if (eventIndex >= bpmEvents.size) break; 
        }
    }

    void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            isPlaying = !isPlaying;
            if (isPlaying) {
                float seekPos = currentScrollY / PIXELS_PER_SECOND;
                if (seekPos < 0) seekPos = 0;
                music.setPosition(seekPos);
                music.play();
                
                smoothTime = seekPos;
                lastHitCheckTime = seekPos;
                lastFrameTime = seekPos;
            } else {
                music.pause();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            isMetronomeOn = !isMetronomeOn;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) saveChart();
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) snapDivisor = 4;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) snapDivisor = 8;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) snapDivisor = 16;
        
        // オフセット変更
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) offset -= 0.01f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) offset += 0.01f;

        float seekAmount = 0;
        boolean shiftPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) seekAmount = shiftPressed ? -5.0f : -1.0f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) seekAmount = shiftPressed ? 5.0f : 1.0f;

        if (seekAmount != 0) {
            seekMusic(seekAmount);
        }

        // BPM調整 (カーソル位置のChartTimeを取得)
        if (shiftPressed) {
            float chartTime = (currentScrollY / PIXELS_PER_SECOND) - offset;
            BpmEvent e = getBpmAt(chartTime);
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) e.bpm += 1;
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) e.bpm -= 1;
        }

        // BPM変更点追加
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            float nowChartTime = (currentScrollY / PIXELS_PER_SECOND) - offset;
            if (nowChartTime < 0) nowChartTime = 0; // 負の時間は0に丸める

            BpmEvent existing = null;
            for(BpmEvent e : bpmEvents) {
                if (Math.abs(e.time - nowChartTime) < 0.1f) existing = e;
            }
            if (existing == null) {
                float prevBpm = getBpmAt(nowChartTime).bpm;
                bpmEvents.add(new BpmEvent(nowChartTime, prevBpm));
                sortBpmEvents();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
             music.stop();
             game.setScreen(new DevSelectScreen(game));
        }

        if (!isPlaying) {
            float scrollAmt = 10;
            if (shiftPressed) scrollAmt = 50;

            if (Gdx.input.isKeyPressed(Input.Keys.W)) currentScrollY += scrollAmt;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) currentScrollY -= scrollAmt;
            
            if (currentScrollY < 0) currentScrollY = 0;
        }
    }

    void seekMusic(float amount) {
        float currentPos = currentScrollY / PIXELS_PER_SECOND;
        float newPos = currentPos + amount;
        if (newPos < 0) newPos = 0;
        currentScrollY = newPos * PIXELS_PER_SECOND;
        
        smoothTime = newPos;
        lastHitCheckTime = newPos;
        lastFrameTime = newPos;

        if (isPlaying) {
            music.setPosition(newPos);
        }
    }

    BpmEvent getBpmAt(float chartTime) {
        BpmEvent target = bpmEvents.first();
        for (BpmEvent e : bpmEvents) {
            if (e.time <= chartTime) target = e;
            else break;
        }
        return target;
    }

    // ★修正: クリック座標を譜面時間に変換
    float getSnappedChartTime(float y) {
        // まず画面Y座標を音楽時間(MusicTime)に変換
        float musicTime = y / PIXELS_PER_SECOND;
        
        // そこからオフセットを引いて譜面時間(ChartTime)にする
        float chartTime = musicTime - offset;
        if (chartTime < 0) chartTime = 0;

        BpmEvent currentEvent = getBpmAt(chartTime);
        float beatDuration = 60f / currentEvent.bpm;
        float snapInterval = beatDuration / (snapDivisor / 4f);

        float timeFromEvent = chartTime - currentEvent.time;
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

            // スナップされた譜面時間を取得
            float snapChartTime = getSnappedChartTime(touchPos.y);

            // 削除 (判定も譜面時間で行う)
            if (button == Input.Buttons.RIGHT) {
                Note target = null;
                for (Note n : notes) {
                    if (n.lane == lane && Math.abs(n.targetTime - snapChartTime) < 0.05f) target = n;
                    if (n.isHold && n.lane == lane && snapChartTime >= n.targetTime && snapChartTime <= n.endTime) target = n;
                }
                if (target != null) {
                    notes.removeValue(target, true);
                    if (deleteSound != null) deleteSound.play(0.5f, 0.8f, 0); 
                }
                if (tempHoldStart != null) tempHoldStart = null;
                return true;
            }

            // 配置
            if (button == Input.Buttons.LEFT) {
                if (currentMode == EditMode.TAP) {
                    Note newNote = new Note(snapChartTime, lane);
                    newNote.isHold = false;
                    newNote.endTime = snapChartTime;
                    notes.add(newNote);
                    if (hitSound != null) hitSound.play(0.5f);

                } else if (currentMode == EditMode.HOLD) {
                    if (tempHoldStart == null) {
                        tempHoldStart = new Note(snapChartTime, lane);
                        if (hitSound != null) hitSound.play(0.5f);
                    } else {
                        float endTime = snapChartTime;
                        if (endTime <= tempHoldStart.targetTime) {
                            tempHoldStart = null; 
                        } else {
                            if (tempHoldStart.lane != lane) tempHoldStart.lane = lane;
                            tempHoldStart.isHold = true;
                            tempHoldStart.endTime = endTime;
                            notes.add(tempHoldStart);
                            tempHoldStart = null;
                            if (hitSound != null) hitSound.play(0.5f);
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            if (!isPlaying) {
                float scrollAmt = amountY * 0.5f; 
                seekMusic(scrollAmt); 
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