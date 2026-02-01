package com.example.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.example.game.GameConfig;
import com.example.game.Main;
import com.example.game.Note;
import com.example.game.logic.EffectManager;
import com.example.game.logic.JudgeSystem;
import com.example.game.logic.NoteManager;

public class GameScreen extends ScreenAdapter {
    final Main game;
    String songName; 
    
    NoteManager noteManager;
    JudgeSystem judgeSystem;
    EffectManager effectManager;

    ShapeRenderer shapeRenderer;
    Texture noteImg;
    Music music;
    Sound hitSound;
    Sound countSound; 

    float songPosition = 0;

    // 3D設定
    final float VANISHING_POINT_Y = 1000;
    final float JUDGEMENT_LINE_Y = 150;
    final float LANE_BOTTOM_Y = 50f;
    final float CAMERA_DEPTH = 1.0f;
    final float NEAR_WIDTH_TOTAL = 1500;
    final float CENTER_X = 1920 / 2f;
    
    float scrollSpeed; 
    float userOffset;

    // カウントダウン用
    boolean isPlaying = false; 
    float countdownTimer = 0;  
    int countIndex = 0;        
    float bpm = 120;           
    float beatDuration;        
    float countInterval;       

    ObjectMap<String, Float> bpmMap = new ObjectMap<>();

    // ポーズ機能用の変数
    boolean isPaused = false;
    String[] pauseItems = {"RESUME", "RESTART", "QUIT"};
    int pauseIndex = 0;

    // フェードイン演出用の変数
    private ShapeRenderer fadeRenderer;
    private float fadeInAlpha = 1.0f;
    private boolean isFadingIn = true;
    final float FADE_SPEED = 0.8f;

    private boolean isQuitting = false;

    public GameScreen(Main game, String songName) {
        this.game = game;
        this.songName = songName;

        fadeRenderer = new ShapeRenderer();
        fadeInAlpha = 1.0f;
        isFadingIn = true;
        
        this.scrollSpeed = GameConfig.getScrollSpeed();
        this.userOffset = GameConfig.getOffset();

        bpmMap.put("Link Layer", 156f);
        bpmMap.put("Pop!Stack!", 160f);
        bpmMap.put("Timepiece Tower", 140f);
        bpmMap.put("Eigenstate", 174f);

        this.bpm = bpmMap.get(songName, 140f);
        this.beatDuration = 60f / this.bpm;
        this.countInterval = this.beatDuration; 

        shapeRenderer = new ShapeRenderer();
        noteImg = new Texture("notes-UI.png");
        
        noteManager = new NoteManager(songName);
        judgeSystem = new JudgeSystem(noteManager.getTotalNotes());
        effectManager = new EffectManager();

        music = Gdx.audio.newMusic(Gdx.files.internal(songName + ".mp3"));
        music.setVolume(0.3f);
        music.setOnCompletionListener(m -> {
            game.setScreen(new ResultScreen(game, (int)judgeSystem.score, judgeSystem.getRank()));
            dispose();
        });
        
        try { hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.mp3")); } catch (Exception e) {}
        try { countSound = Gdx.audio.newSound(Gdx.files.internal("count.mp3")); } catch (Exception e) {}

        isPlaying = false;
        countdownTimer = -0.5f;
        countIndex = 0;
        isQuitting = false;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        if (isPaused) {
            handlePauseInput();
        } else {
            // ゲーム進行
            if (!isPlaying) {
                updateCountdown(delta);
            } else {
                songPosition = music.getPosition();
                judgeSystem.update(delta);
                effectManager.update(delta);
                
                // ミス判定（通り過ぎたかチェック）
                noteManager.checkMiss(songPosition - userOffset, judgeSystem);

                // ★追加：ホールド中の判定更新（押し続けているかチェック）
                updateHolds(delta);

                // タップ判定（始点判定）
                for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
                    if (Gdx.input.isKeyJustPressed(GameConfig.KEY_MAPPING[i])) {
                        processHit(i);
                    }
                }
            }
            
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                pauseGame();
            }
        }

        // --- 描画処理 ---
        drawLanes();
        drawEffects();
        
        // ★ホールドの「長い帯」を先に描画（ノーツの下に表示させるため）
        drawHoldBodies();

        drawNotes(); // 単押しノーツとホールドの「頭」を描画
        drawUI();

        if (isPaused) {
            drawPauseMenu();
        }

        // フェードイン演出
        if (isFadingIn) {
            fadeInAlpha -= delta * FADE_SPEED;
            if (fadeInAlpha <= 0f) {
                fadeInAlpha = 0f;
                isFadingIn = false;
            }
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            fadeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            fadeRenderer.setColor(1f, 1f, 1f, fadeInAlpha);
            fadeRenderer.rect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
            fadeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    // ★追加：ホールド状態の更新ロジック
    void updateHolds(float delta) {
        float currentDisplayTime = songPosition - userOffset;

        for (Note note : noteManager.notes) {
            // ホールド中でなければスキップ
            if (!note.isHold || !note.active) continue;

            // 1. すでに判定エリアに入ってホールド中の場合
            if (note.isHolding) {
                // 終了時間を過ぎたら完了
                if (currentDisplayTime >= note.endTime) {
                    note.isHolding = false;
                    note.active = false;
                    // ホールド完了エフェクトなどを出しても良い
                    judgeSystem.combo++;
                    judgeSystem.score += 500; // 完走ボーナス
                } else {
                    // まだ途中：キーが押され続けているかチェック
                    if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[note.lane])) {
                        // 押されているならスコア加算（Arcaeaのように継続的に加算）
                        // ここでは簡易的にコンボなどは増やさず、エフェクトだけ維持する例
                        float scale = getScale(0);
                        // エフェクトを出し続ける
                        if (Math.random() < 0.3) { // 毎回出すと重いので確率で
                            effectManager.spawn(getLaneCenterX(note.lane, scale), JUDGEMENT_LINE_Y, getLaneWidth(scale), Color.CYAN);
                        }
                    } else {
                        // 離してしまったらホールド中断（ArcaeaならここでLost判定だが、今回は判定終了にする）
                        note.isHolding = false;
                        // note.active = false; // これを有効にすると消える。残したいならfalseにしない
                        // ミス扱いにするならここでcomboリセットなど
                        judgeSystem.resetCombo();
                    }
                }
            }
        }
    }

    // ★修正：ヒット処理（始点の判定）
    void processHit(int lane) {
        float currentDisplayTime = songPosition - userOffset;
        
        for (Note note : noteManager.notes) {
            if (note.lane != lane || !note.active) continue;
            
            // すでにホールド中のノーツは「始点判定」の対象外
            if (note.isHold && note.isHolding) continue;

            // 判定
            Color resultColor = judgeSystem.checkHit(note.targetTime + userOffset, songPosition);
            if (resultColor != null) {
                if (hitSound != null) hitSound.play();
                
                float scale = getScale(0);
                effectManager.spawn(getLaneCenterX(lane, scale), JUDGEMENT_LINE_Y, getLaneWidth(scale), resultColor);

                if (note.isHold) {
                    // ★ホールドの場合：アクティブなまま「ホールド中」フラグを立てる
                    note.isHolding = true;
                } else {
                    // 単押しの場合：即座に消す
                    note.active = false;
                }
                return; // 1回のキーで1個だけ判定
            }
        }
    }

    // ★追加：ホールドの「帯（ボディ）」を描画
    // ★修正：ホールドの「帯（ボディ）」を描画
    void drawHoldBodies() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float currentDisplayTime = songPosition - userOffset;

        // レーンの描画限界距離（drawLanesで使っている値と同じにする）
        final float MAX_DRAW_Z = 10.0f; 

        for (Note note : noteManager.notes) {
            if (note.active && note.isHold) {
                // 始点位置（Z距離）
                float startZ;
                if (note.isHolding) {
                    startZ = 0; // ホールド中は手前に張り付き
                } else {
                    float timeToStart = note.targetTime - currentDisplayTime;
                    startZ = timeToStart * scrollSpeed;
                }

                // 終点位置（Z距離）
                float timeToEnd = note.endTime - currentDisplayTime;
                float endZ = timeToEnd * scrollSpeed;

                // --- 修正ポイント ---
                
                // 1. 全体が描画範囲外ならスキップ
                if (endZ < 0 || startZ > MAX_DRAW_Z) continue;

                // 2. 始点が手前すぎる場合の補正（判定ラインより手前は描かない）
                if (startZ < 0) startZ = 0;

                // 3. ★重要：終点が奥すぎる場合の補正（レーンの上限で切り取る）
                if (endZ > MAX_DRAW_Z) endZ = MAX_DRAW_Z;

                // --------------------

                // 3D座標計算
                float scaleStart = getScale(startZ);
                float scaleEnd = getScale(endZ);

                float x1 = getLaneCenterX(note.lane, scaleStart) - getLaneWidth(scaleStart)/2;
                float x2 = getLaneCenterX(note.lane, scaleStart) + getLaneWidth(scaleStart)/2;
                float y1 = getScreenY(scaleStart); // 始点のY

                float x3 = getLaneCenterX(note.lane, scaleEnd) + getLaneWidth(scaleEnd)/2;
                float x4 = getLaneCenterX(note.lane, scaleEnd) - getLaneWidth(scaleEnd)/2;
                float y2 = getScreenY(scaleEnd);   // 終点のY
                
                // 色の設定
                if (note.isHolding) {
                    shapeRenderer.setColor(0f, 1f, 1f, 0.6f); // 発光シアン
                } else {
                    shapeRenderer.setColor(0f, 0.5f, 0.5f, 0.4f); // 暗いシアン
                }

                // 描画
                shapeRenderer.triangle(x1, y1, x2, y1, x3, y2);
                shapeRenderer.triangle(x1, y1, x3, y2, x4, y2);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void drawNotes() {
        game.batch.begin();
        game.batch.setColor(Color.WHITE);

        for (Note note : noteManager.notes) {
            if (note.active) {
                // ホールド中で、始点を過ぎている場合は「頭」を描画しない（帯だけにする）
                // もしプロセカのように「押しっぱなしでも判定ラインに光るノーツを残したい」なら
                // ここで if (note.isHolding) の処理を変えます。今回はArcaea風なので消します。
                if (note.isHolding) continue;

                float timeRemains = (note.targetTime + userOffset) - songPosition;
                float zDistance = timeRemains * scrollSpeed;
                
                if (zDistance < -0.2f || zDistance > 10.0f) continue;

                float scale = getScale(zDistance);
                float drawY = getScreenY(scale);
                float drawH = 40f * scale; 

                if (drawY + drawH < LANE_BOTTOM_Y) continue;

                float laneWidth = getLaneWidth(scale);
                float widthScale = 1.0f; 
                float drawW = laneWidth * widthScale;
                float drawX = getLaneCenterX(note.lane, scale);

                game.batch.draw(noteImg, drawX - drawW/2, drawY, drawW, drawH);
            }
        }
        game.batch.end();
    }

    // --- その他（既存コードと同じ） ---
    void pauseGame() {
        isPaused = true;
        if (music.isPlaying()) music.pause(); 
        pauseIndex = 0; 
    }

    void resumeGame() {
        isPaused = false;
        if (isPlaying) music.play();
    }

    void restartGame() {
        music.setOnCompletionListener(null);
        if (music.isPlaying()) music.stop(); 
        else music.stop();

        music.setOnCompletionListener(m -> {
            game.setScreen(new ResultScreen(game, (int)judgeSystem.score, judgeSystem.getRank()));
            dispose();
        });
        
        isPaused = false;
        isPlaying = false;
        countdownTimer = -0.5f; 
        countIndex = 0;
        songPosition = 0;
        isQuitting = false;
        
        noteManager = new NoteManager(songName);
        judgeSystem = new JudgeSystem(noteManager.getTotalNotes());
        effectManager = new EffectManager();
    }

    private void returnToSongSelect() {
        if (isQuitting) return;
        isQuitting = true;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                dispose(); 
                System.gc();
                game.setScreen(new SongSelectScreen(game));
            }
        });
    }

    void handlePauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            pauseIndex--;
            if (pauseIndex < 0) pauseIndex = pauseItems.length - 1;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            pauseIndex++;
            if (pauseIndex >= pauseItems.length) pauseIndex = 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            switch (pauseIndex) {
                case 0: resumeGame(); break;
                case 1: restartGame(); break;
                case 2: returnToSongSelect(); break;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) resumeGame();
    }

    void drawPauseMenu() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f); 
        shapeRenderer.rect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.batch.begin();
        game.font.setColor(Color.CYAN);
        game.font.getData().setScale(4.0f);
        game.font.draw(game.batch, "PAUSED", CENTER_X - 150, 800);
        for (int i = 0; i < pauseItems.length; i++) {
            float y = 600 - (i * 120);
            if (i == pauseIndex) {
                game.font.setColor(Color.YELLOW);
                game.font.getData().setScale(2.5f);
                game.font.draw(game.batch, "> " + pauseItems[i] + " <", CENTER_X - 150, y);
            } else {
                game.font.setColor(Color.GRAY);
                game.font.getData().setScale(2.0f);
                game.font.draw(game.batch, pauseItems[i], CENTER_X - 100, y);
            }
        }
        game.batch.end();
    }

    void updateCountdown(float delta) {
        countdownTimer += delta;
        if (countdownTimer < 0) return;
        if (countIndex < 4) {
            if (countdownTimer >= countIndex * countInterval) {
                if (countSound != null) countSound.play();
                countIndex++;
            }
        } else {
            float startTime = (4 * countInterval) + (2 * beatDuration);
            if (countdownTimer >= startTime) {
                music.play();
                isPlaying = true;
            }
        }
    }
    
    void drawLanes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float laneDrawBottomY = LANE_BOTTOM_Y;
        float scaleStart = (VANISHING_POINT_Y - laneDrawBottomY) / (VANISHING_POINT_Y - JUDGEMENT_LINE_Y);
        for (int i = 0; i < GameConfig.LANE_COUNT; i++) {
            if (Gdx.input.isKeyPressed(GameConfig.KEY_MAPPING[i])) shapeRenderer.setColor(1, 1, 0, 0.3f);
            else shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.5f);
            float x1 = getLaneCenterX(i, scaleStart) - getLaneWidth(scaleStart)/2;
            float x2 = getLaneCenterX(i, scaleStart) + getLaneWidth(scaleStart)/2;
            float scaleFar = getScale(10.0f);
            float x3 = getLaneCenterX(i, scaleFar) + getLaneWidth(scaleFar)/2;
            float x4 = getLaneCenterX(i, scaleFar) - getLaneWidth(scaleFar)/2;
            float y2 = getScreenY(scaleFar);
            shapeRenderer.triangle(x1, laneDrawBottomY, x2, laneDrawBottomY, x3, y2);
            shapeRenderer.triangle(x1, laneDrawBottomY, x3, y2, x4, y2);
        }
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.CYAN); 
        float lineHeight = 4.0f;
        float lineY = JUDGEMENT_LINE_Y - lineHeight/2;
        float scaleJust = getScale(0); 
        float leftX = getLaneCenterX(0, scaleJust) - getLaneWidth(scaleJust)/2;
        float rightX = getLaneCenterX(GameConfig.LANE_COUNT-1, scaleJust) + getLaneWidth(scaleJust)/2;
        shapeRenderer.rect(leftX, lineY, rightX - leftX, lineHeight);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        for (int i = 0; i <= GameConfig.LANE_COUNT; i++) {
            float xNear = (CENTER_X - (NEAR_WIDTH_TOTAL * scaleStart)/2) + ((NEAR_WIDTH_TOTAL * scaleStart)/4)*i;
            float scaleFar = getScale(10.0f); 
            float totalWFar = NEAR_WIDTH_TOTAL * scaleFar;
            float xFar = (CENTER_X - totalWFar/2) + (totalWFar/4)*i;
            float yFar = getScreenY(scaleFar);
            shapeRenderer.line(xNear, laneDrawBottomY, xFar, yFar);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void drawEffects() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (EffectManager.Ripple r : effectManager.ripples) {
            shapeRenderer.setColor(r.color.r, r.color.g, r.color.b, r.life * 2.5f);
            shapeRenderer.circle(r.x, r.y, r.radius);
        }
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (EffectManager.Particle p : effectManager.particles) {
            float size = (p.life / p.maxLife) * 10.0f;
            shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, p.life);
            shapeRenderer.rect(p.x - size/2, p.y - size/2, size, size);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void drawUI() {
        game.batch.begin();
        if (!isPlaying && !isPaused) { 
            game.font.setColor(Color.YELLOW);
            game.font.getData().setScale(3.0f);
            if (countdownTimer >= 0) {
                if (countIndex < 4) {
                    game.font.draw(game.batch, "READY...", CENTER_X - 100, 600);
                } else {
                    game.font.draw(game.batch, "GO!", CENTER_X - 50, 600);
                }
            }
        }
        if (judgeSystem.messageTimer > 0) {
            game.font.setColor(judgeSystem.messageColor);
            game.font.getData().setScale(2.5f);
            game.font.draw(game.batch, judgeSystem.message, CENTER_X - 80, 450);
            if (!judgeSystem.timingMessage.isEmpty()) {
                if (judgeSystem.timingMessage.equals("FAST")) game.font.setColor(Color.RED);
                else game.font.setColor(Color.BLUE);
                game.font.getData().setScale(1.5f);
                game.font.draw(game.batch, judgeSystem.timingMessage, CENTER_X - 40, 400);
            }
        }
        game.font.setColor(Color.WHITE);
        game.font.getData().setScale(2.0f);
        game.font.draw(game.batch, "Music: " + songName, 20, 1050);
        game.font.draw(game.batch, "BPM: " + (int)bpm, 20, 1010); 
        game.font.draw(game.batch, "Speed: " + String.format("%.1f", scrollSpeed), 20, 970);
        game.font.draw(game.batch, "Score: " + (int)judgeSystem.score, 20, 930);
        game.font.draw(game.batch, "Combo: " + judgeSystem.combo, 20, 890);
        game.batch.end();
    }

    float getScale(float zDistance) { return CAMERA_DEPTH / (CAMERA_DEPTH + zDistance); }
    float getScreenY(float scale) { return VANISHING_POINT_Y - (VANISHING_POINT_Y - JUDGEMENT_LINE_Y) * scale; }
    float getLaneWidth(float scale) { return NEAR_WIDTH_TOTAL * scale / 4.0f; }
    float getLaneCenterX(int lane, float scale) {
        float totalW = NEAR_WIDTH_TOTAL * scale;
        float startX = CENTER_X - (totalW / 2.0f);
        float oneLaneW = totalW / 4.0f;
        return startX + (oneLaneW * lane) + (oneLaneW / 2.0f);
    }

    @Override
    public void dispose() {
        try {
            if (shapeRenderer != null) shapeRenderer.dispose();
            if (noteImg != null) noteImg.dispose();
        } catch (Exception e) { }
        try {
            if (hitSound != null) hitSound.dispose();
            if (countSound != null) countSound.dispose();
        } catch (Exception e) { }
        try {
            if (music != null) {
                music.setOnCompletionListener(null);
                music.stop(); 
                music.dispose();
                music = null;
            }
        } catch (Exception e) { }
        if (fadeRenderer != null) {
            fadeRenderer.dispose();
        }
    }
}