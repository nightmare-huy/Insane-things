package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.registry.StoneType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Способности Камня Пространства (синий):
 * 1) Парные порталы, 2) Телекинез, 3) Фазовый сдвиг, 4) Сингулярность.
 */
public final class SpaceAbilities {
    /** Активные пары порталов (вход -> выход). */
    public static final List<PortalPair> PORTALS = new ArrayList<>();
    /** Активные сингулярности (чёрные дыры). */
    public static final List<Singularity> SINGULARITIES = new ArrayList<>();

    public record PortalPair(Vec3 entry, Vec3 exit, long expiresAt, ServerLevel level, Entity owner) {
    }

    public record Singularity(Vec3 center, long until, ServerLevel level, Entity owner) {
    }

    private SpaceAbilities() {
    }

    /** Возвращает перезарядку в тиках (-1 — удержание без перезарядки). */
    public static long use(ServerPlayer player, ItemStack gauntlet, int ability, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        switch (ability) {
            case 0 -> {
                // Парные порталы: синий снаряд создаёт вход, выход — по взгляду игрока
                Vec3 exit = Effects.aimPoint(player, 24.0);
                Vec3 entry = findSurface(level, eye, Effects.lookVec(player), 40.0);
                PORTALS.add(new PortalPair(entry, exit, now + 1200, level, player));
                Effects.energyBurst(level, entry, StoneType.SPACE.getColor(), 24);
                Effects.energyBurst(level, exit, StoneType.SPACE.getColor(), 24);
                Effects.sound(level, eye, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.8F);
                Effects.sound(level, exit, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.2F);
                return 100;
            }
            case 1 -> {
                // Телекинез: захват сущности (удержание ПКМ)
                HitResult hit = player.pick(20.0, 1.0F, false);
                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity mob
                        && mob != player) {
                    AbilityManager.telekinesisMap().put(player.getUUID(), mob);
                    mob.setNoGravity(true);
                    mob.setDeltaMovement(Vec3.ZERO);
                    Effects.energyBurst(level, mob.position(), StoneType.SPACE.getColor(), 16);
                    Effects.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
                }
                return -1;
            }
            case 2 -> {
                // Фазовый сдвиг
                if (player.isShiftKeyDown()) {
                    // Себя: проход сквозь стены 5 секунд
                    AbilityManager.phaseMap().put(player.getUUID(), now + 100);
                    Effects.energyBurst(level, eye, StoneType.SPACE.getColor(), 30);
                    Effects.sound(level, eye, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.5F);
                } else {
                    // Враги тонут на 2-3 блока в блоках
                    for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, eye, 10.0, player)) {
                        Vec3 sunk = enemy.position().add(0, -2.5, 0);
                        enemy.teleportTo(sunk.x, sunk.y, sunk.z);
                        enemy.hurt(enemy.damageSources().inWall(), 6.0F);
                        Effects.energyBurst(level, enemy.position(), StoneType.SPACE.getColor(), 12);
                    }
                    Effects.sound(level, eye, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.6F);
                }
                return 400;
            }
            case 3 -> {
                // Сингулярность: чёрная дыра на 5 секунд
                Vec3 center = findSurface(level, eye, Effects.lookVec(player), 18.0);
                SINGULARITIES.add(new Singularity(center, now + 100, level, player));
                Effects.sound(level, center, SoundEvents.WITHER_SPAWN, 0.7F, 1.5F);
                return 600;
            }
            default -> {
                return -1;
            }
        }
    }

    /** Тик телекинеза: удерживаемая сущность следует за рукой. Shift+ПКМ — раздавить. */
    public static void telekinesisTick(ServerPlayer player, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Entity held = AbilityManager.telekinesisMap().get(player.getUUID());
        if (held == null || !held.isAlive()) {
            AbilityManager.telekinesisMap().remove(player.getUUID());
            return;
        }
        Vec3 target = Effects.aimPoint(player, 5.0);
        Vec3 delta = target.subtract(held.position());
        held.setDeltaMovement(delta.scale(0.25));
        Effects.burst(level, ParticleTypes.PORTAL, held.position(), 2, 0.3, 0.1);

        // Shift+ПКМ: раздавить удерживаемую сущность
        if (player.isShiftKeyDown()) {
            held.hurt(held.damageSources().magic(), 15.0F);
            Effects.energyBurst(level, held.position(), StoneType.SPACE.getColor(), 24);
            Effects.sound(level, held.position(), SoundEvents.ANVIL_LAND, 0.8F, 1.2F);
            held.setNoGravity(false);
            AbilityManager.telekinesisMap().remove(player.getUUID());
        }
    }

    /** Точка на поверхности блока по лучу взгляда (или конец луча). */
    static Vec3 findSurface(ServerLevel level, Vec3 from, Vec3 direction, double maxDist) {
        Vec3 end = from.add(direction.scale(maxDist));
        BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(
                from, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            return new Vec3(pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5);
        }
        return end;
    }
}
