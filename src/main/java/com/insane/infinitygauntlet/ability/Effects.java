package com.insane.infinitygauntlet.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Общие помощники эффектов: частицы, молнии, звуки, урон, кольца и лучи.
 */
public final class Effects {
    private Effects() {
    }

    /** Цветные частицы пыли заданного цвета (0xRRGGBB). */
    public static DustParticleOptions dust(int color, float size) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        return new DustParticleOptions(new Vector3f(r, g, b), size);
    }

    /** Вспышка частиц в точке. */
    public static void burst(ServerLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    /** Линия частиц между двумя точками (луч/нить/поток). */
    public static void beam(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
        double dist = from.distanceTo(to);
        int steps = Math.max(1, (int) (dist / step));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, (double) i / steps);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Кольцо частиц вокруг точки (горизонтальное). */
    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Вертикальный круг частиц (руны времени вокруг запястья). */
    public static void circle(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points, double tilt) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + Math.sin(angle) * radius * tilt;
            double z = center.z + Math.sin(angle) * radius * 0.3;
            level.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Молния в точке (визуальная, без урона). */
    public static void lightning(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(x, y, z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    /** Звук в точке мира. */
    public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, BlockPos.containing(pos), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Перегрузка для звуков типа Holder<SoundEvent> (например, GENERIC_EXPLODE). */
    public static void sound(ServerLevel level, Vec3 pos, net.minecraft.core.Holder<SoundEvent> sound, float volume, float pitch) {
        sound(level, pos, sound.value(), volume, pitch);
    }

    /** Чистый урон, игнорирующий броню и щиты. */
    public static void pureDamage(Entity target, Player attacker, float amount) {
        target.hurt(target.damageSources().magic(), amount);
    }

    /** Взрыв частиц энергии цвета камня. */
    public static void energyBurst(ServerLevel level, Vec3 pos, int color, int count) {
        burst(level, dust(color, 1.5F), pos, count, 0.6, 0.4);
        burst(level, ParticleTypes.ELECTRIC_SPARK, pos, count / 2, 0.5, 0.3);
    }

    /** Направление взгляда игрока (нормализованный вектор). */
    public static Vec3 lookVec(Player player) {
        return player.getLookAngle();
    }

    /** Точка на расстоянии dist от глаз игрока по направлению взгляда. */
    public static Vec3 aimPoint(Player player, double dist) {
        Vec3 eye = player.getEyePosition();
        return eye.add(lookVec(player).scale(dist));
    }

    /** Гул энергии (звук маяка). */
    public static void hum(ServerLevel level, Vec3 pos) {
        sound(level, pos, SoundEvents.BEACON_ACTIVATE, 0.7F, 1.4F);
        sound(level, pos, SoundEvents.BEACON_AMBIENT, 0.5F, 1.8F);
    }
}
