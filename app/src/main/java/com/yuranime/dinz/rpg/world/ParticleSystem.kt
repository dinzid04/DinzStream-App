package com.yuranime.dinz.rpg.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.yuranime.dinz.rpg.animation.RpgAssetManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var gravity: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float,
    var textureKey: String,
    var tint: Color = Color.WHITE
)

class ParticleSystem {
    private val particles = mutableListOf<Particle>()

    fun spawnWoodChips(x: Float, y: Float, count: Int = 8) {
        repeat(count) {
            val angle = Random.nextFloat() * 3.14159f
            val speed = 18f + Random.nextFloat() * 28f
            particles.add(
                Particle(
                    x = x + Random.nextFloat() * 6f - 3f,
                    y = y + Random.nextFloat() * 6f,
                    vx = cos(angle) * speed * (if (Random.nextBoolean()) 1f else -1f),
                    vy = 20f + sin(angle) * speed,
                    gravity = -95f,
                    life = 0.55f + Random.nextFloat() * 0.25f,
                    maxLife = 0.8f,
                    size = 6f,
                    textureKey = "wood_chip"
                )
            )
        }
    }

    fun spawnLeaves(x: Float, y: Float, count: Int = 10, isSakura: Boolean = false) {
        val tint = if (isSakura) Color(1f, 0.78f, 0.86f, 1f) else Color.WHITE
        repeat(count) {
            particles.add(
                Particle(
                    x = x + Random.nextFloat() * 18f - 9f,
                    y = y + Random.nextFloat() * 14f,
                    vx = Random.nextFloat() * 18f - 9f,
                    vy = -6f - Random.nextFloat() * 10f,
                    gravity = -8f,
                    life = 0.9f + Random.nextFloat() * 0.6f,
                    maxLife = 1.5f,
                    size = 6f,
                    textureKey = "leaf_particle",
                    tint = tint
                )
            )
        }
    }

    fun spawnWaterDrops(x: Float, y: Float, count: Int = 9) {
        repeat(count) {
            val angle = Random.nextFloat() * 3.14159f
            val speed = 14f + Random.nextFloat() * 24f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed * (if (Random.nextBoolean()) 1f else -1f),
                    vy = 18f + sin(angle) * speed,
                    gravity = -90f,
                    life = 0.45f + Random.nextFloat() * 0.2f,
                    maxLife = 0.65f,
                    size = 5f,
                    textureKey = "water_drop"
                )
            )
        }
    }

    fun spawnHarvestSparkles(x: Float, y: Float, count: Int = 10) {
        repeat(count) {
            particles.add(
                Particle(
                    x = x + Random.nextFloat() * 12f - 6f,
                    y = y + Random.nextFloat() * 10f - 5f,
                    vx = Random.nextFloat() * 14f - 7f,
                    vy = 16f + Random.nextFloat() * 20f,
                    gravity = -12f,
                    life = 0.65f + Random.nextFloat() * 0.3f,
                    maxLife = 0.95f,
                    size = 7f,
                    textureKey = "sparkle_harvest"
                )
            )
        }
    }

    fun spawnRain(camX: Float, camY: Float, viewW: Float, viewH: Float) {
        repeat(3) {
            particles.add(
                Particle(
                    x = camX - viewW * 0.6f + Random.nextFloat() * viewW * 1.3f,
                    y = camY + viewH * 0.55f,
                    vx = -38f,
                    vy = -175f - Random.nextFloat() * 35f,
                    gravity = 0f,
                    life = 0.95f,
                    maxLife = 0.95f,
                    size = 7f,
                    textureKey = "rain_drop",
                    tint = Color(0.8f, 0.92f, 1f, 0.7f)
                )
            )
        }
    }

    fun update(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt
            if (p.life <= 0f) {
                iter.remove()
                continue
            }
            p.vy += p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
        }
    }

    fun render(batch: SpriteBatch, assets: RpgAssetManager) {
        for (p in particles) {
            val tex = assets.particleTextures[p.textureKey] ?: continue
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f) * p.tint.a
            batch.setColor(p.tint.r, p.tint.g, p.tint.b, alpha)
            batch.draw(tex, p.x - p.size * 0.5f, p.y - p.size * 0.5f, p.size, p.size)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }
}
