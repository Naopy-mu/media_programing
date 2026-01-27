package com.example.game.logic;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.Gdx;
import java.util.Iterator;

public class EffectManager {
    // 内部クラス定義（publicにして外から見えるようにする）
    public static class Particle {
        public float x, y, vx, vy, life, maxLife;
        public Color color;
        Particle(float x, float y, Color c) {
            this.x = x; this.y = y;
            double angle = Math.random() * Math.PI * 2;
            float speed = (float)(Math.random() * 150 + 100);
            this.vx = (float)Math.cos(angle) * speed;
            this.vy = (float)Math.sin(angle) * speed;
            this.maxLife = (float)(Math.random() * 0.4 + 0.2);
            this.life = this.maxLife;
            this.color = c;
        }
    }

    public static class Ripple {
        public float x, y, radius, maxRadius, life;
        public Color color;
        Ripple(float x, float y, float width, Color c) {
            this.x = x; this.y = y;
            this.radius = 5;
            this.maxRadius = width / 1.5f;
            this.life = 0.4f;
            this.color = c;
        }
    }

    public Array<Particle> particles = new Array<>();
    public Array<Ripple> ripples = new Array<>();

    public void spawn(float x, float y, float width, Color color) {
        ripples.add(new Ripple(x, y, width, color));
        for (int i = 0; i < 20; i++) particles.add(new Particle(x, y, color));
    }

    public void update(float dt) {
        // Ripple更新
        Iterator<Ripple> rIter = ripples.iterator();
        while (rIter.hasNext()) {
            Ripple r = rIter.next();
            r.radius += (r.maxRadius - r.radius) * 8.0f * dt;
            r.life -= dt;
            if (r.life <= 0) rIter.remove();
        }
        // Particle更新
        Iterator<Particle> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle p = pIter.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;
            if (p.life <= 0) pIter.remove();
        }
    }
}