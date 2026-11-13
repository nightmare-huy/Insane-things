package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.registry.StoneType;
import com.insane.infinitygauntlet.util.GauntletHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Способности Камня Силы (фиолетовый):
 * 1) Рывковый удар, 2) Планетарная ударная волна, 3) Лунопад (метеорный дождь),
 * 4) Поток разрушения (удержание).
 */
public final class PowerAbilities {
    /** Начало удержания потока разрушения: игрок -> тик начала. */
    private static final Map<UUID, Long> STREAM_START = new HashMap<>();

    private PowerAbilities() {
    }

    public static long use(ServerPlayer player, ItemStack gauntlet, int ability, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        int color = StoneType.POWER.getColor();
        switch (ability) {
            case 0 -> {
                // Рывковый удар: рывок на 10 блоков, туннель 3x3, 50+ урона
                Vec3 look = Effects.lookVec(player);
                Vec3 start = player.position();
                Vec3 end = start.add(look.scale(10.0));
                // Туннель 3x3
                for (int i = 1; i <= 10; i++) {
                    Vec3 p = start.add(look.scale(i));
                    BlockPos pos = BlockPos.containing(p);
                    for (BlockPos tunnel : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                        var state = level.getBlockState(tunnel);
                        if (!state.isAir() && state.getDestroySpeed(level, tunnel) >= 0) {
                            level.destroyBlock(tunnel, false);
                        }
                    }
                }
                player.teleportTo(end.x, end.y, end.z);
                player.setDeltaMovement(look.scale(1.5));
                Effects.burst(level, ParticleTypes.FLAME, end, 40, 1.0, 0.5);
                Effects.energyBurst(level, end, color, 30);
                // Урон по всем на пути
                for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, end, 4.0, player)) {
                    enemy.hurt(enemy.damageSources().magic(), 50.0F);
                    enemy.setDeltaMovement(look.scale(3.0).add(0, 0.5, 0));
                }
                Effects.sound(level, eye, SoundEvents.WITHER_SHOOT, 1.0F, 0.5F);
                Effects.sound(level, eye, SoundEvents.ANVIL_LAND, 0.8F, 0.8F);
                return 100;
            }
            case 1 -> {
                // Планетарная ударная волна: кольцо радиусом 18
                Vec3 center = player.position();
                Effects.ring(level, Effects.dust(color, 2.0F), center, 6.0, 48);
                Effects.ring(level, ParticleTypes.FLAME, center, 12.0, 48);
                Effects.ring(level, Effects.dust(color, 2.0F), center, 18.0, 64);
                // Подброс блоков
                BlockPos centerPos = BlockPos.containing(center);
                for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-10, 0, -10), centerPos.offset(10, 0, 10))) {
                    var state = level.getBlockState(pos);
                    if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0 && Math.random() < 0.4) {
                        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
                        block.setDeltaMovement((Math.random() - 0.5) * 0.5, 1.5 + Math.random(), (Math.random() - 0.5) * 0.5);
                        level.removeBlock(pos, false);
                    }
                }
                for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, center, 18.0, player)) {
                    enemy.hurt(enemy.damageSources().magic(), 25.0F);
                    Vec3 push = enemy.position().subtract(center).normalize().scale(2.5);
                    enemy.setDeltaMovement(push.add(0, 1.5, 0));
                }
                Effects.sound(level, eye, SoundEvents.ANVIL_LAND, 1.0F, 0.6F);
                Effects.sound(level, eye, SoundEvents.WITHER_SHOOT, 1.0F, 0.7F);
                return 400;
            }
            case 2 -> {
                // Лунопад: метеорный дождь из обсидиана и магмы
                Vec3 target = Effects.aimPoint(player, 30.0);
                boolean spaceSynergy = GauntletHelper.getState(gauntlet).hasStone(StoneType.SPACE);
                for (int i = 0; i < 10; i++) {
                    Vec3 spawn = target.add((Math.random() - 0.5) * 16, 20 + Math.random() * 10, (Math.random() - 0.5) * 16);
                    var meteorBlock = Math.random() < 0.5 ? Blocks.OBSIDIAN.defaultBlockState()
                            : Blocks.MAGMA_BLOCK.defaultBlockState();
                    FallingBlockEntity meteor = FallingBlockEntity.fall(level, BlockPos.containing(spawn), meteorBlock);
                    meteor.setDeltaMovement(0, -2.5, 0);
                    Effects.burst(level, ParticleTypes.FLAME, spawn, 8, 0.5, 0.2);
                    // Синергия с Камнем Пространства: метеоры оставляют портальные следы
                    if (spaceSynergy) {
                        Effects.burst(level, ParticleTypes.PORTAL, spawn, 10, 0.5, 0.1);
                    }
                }
                // Кратеры в точке прицеливания
                AbilityManager.destroyBlocks(level, target, 4.0, false);
                for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, target, 10.0, player)) {
                    enemy.hurt(enemy.damageSources().magic(), 20.0F);
                    enemy.setRemainingFireTicks(60);
                }
                Effects.sound(level, eye, SoundEvents.WITHER_SPAWN, 0.8F, 0.9F);
                return 600;
            }
            case 3 -> {
                // Поток разрушения: удержание (см. streamTick)
                STREAM_START.putIfAbsent(player.getUUID(), now);
                return -1;
            }
            default -> {
                return -1;
            }
        }
    }

    /** Тик потока разрушения: широкий фиолетовый луч, отброс, отражение снарядов, взрыв при долгом удержании. */
    public static void streamTick(ServerPlayer player, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 end = Effects.aimPoint(player, 30.0);
        int color = StoneType.POWER.getColor();
        long heldFor = now - STREAM_START.getOrDefault(player.getUUID(), now);

        Effects.beam(level, Effects.dust(color, 2.0F), eye, end, 0.4);
        Effects.burst(level, ParticleTypes.FLAME, end, 4, 0.5, 0.2);

        // Отброс врагов в конусе луча
        for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, end, 5.0, player)) {
            enemy.setDeltaMovement(Effects.lookVec(player).scale(1.2));
            enemy.hurt(enemy.damageSources().magic(), 2.0F);
            // Долгое удержание: цель взрывается
            if (heldFor > 60) {
                enemy.hurt(enemy.damageSources().magic(), 30.0F);
                Effects.energyBurst(level, enemy.position(), color, 40);
                Effects.sound(level, enemy.position(), SoundEvents.GENERIC_EXPLODE, 1.0F, 0.8F);
            }
        }

        // Отражение снарядов обратно
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().inflate(8.0), p -> p.getOwner() != player)) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.5).add(0, 0.5, 0));
        }
        Effects.sound(level, eye, SoundEvents.BEACON_AMBIENT, 0.4F, 0.7F);
    }

    /** Сброс таймера удержания потока разрушения при отпускании ПКМ. */
    public static void endStream(ServerPlayer player) {
        STREAM_START.remove(player.getUUID());
    }
}
