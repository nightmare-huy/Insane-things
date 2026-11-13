package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.registry.StoneType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Способности Камня Разума (жёлтый):
 * 1) Непрерывный лазер, 2) Псионический взрыв, 3) Контроль разума, 4) Подавление.
 */
public final class MindAbilities {
    private MindAbilities() {
    }

    public static long use(ServerPlayer player, ItemStack gauntlet, int ability, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        int color = StoneType.MIND.getColor();
        switch (ability) {
            case 0 -> {
                // Лазер активируется удержанием (см. laserTick)
                return -1;
            }
            case 1 -> {
                // Псионический взрыв: 20 блоков, урон + отброс, без разрушения построек
                Vec3 center = Effects.aimPoint(player, 6.0);
                Effects.energyBurst(level, center, color, 40);
                Effects.ring(level, Effects.dust(color, 1.5F), center, 4.0, 32);
                Effects.sound(level, center, SoundEvents.WITHER_SHOOT, 1.0F, 0.8F);
                for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, center, 20.0, player)) {
                    enemy.hurt(enemy.damageSources().magic(), 12.0F);
                    Vec3 push = enemy.position().subtract(center).normalize().scale(2.0);
                    enemy.setDeltaMovement(push.add(0, 0.8, 0));
                }
                return 200;
            }
            case 2 -> {
                // Контроль разума: моб светится и становится телохранителем
                HitResult hit = player.pick(15.0, 1.0F, false);
                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity mob
                        && mob != player && mob.isAlive()) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0));
                    List<UUID> controlled = AbilityManager.controlledMap().computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
                    controlled.add(mob.getUUID());
                    Effects.energyBurst(level, mob.position(), color, 24);
                    Effects.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 0.9F, 1.8F);
                }
                return 300;
            }
            case 3 -> {
                // Подавление: паралич + слепота, мобы роняют оружие, подсветка всех сквозь стены
                for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, eye, 20.0, player)) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 255));
                    enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                    ItemStack held = enemy.getMainHandItem();
                    if (!held.isEmpty()) {
                        ItemEntity drop = new ItemEntity(level, enemy.getX(), enemy.getY(), enemy.getZ(), held.copy());
                        level.addFreshEntity(drop);
                        enemy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    }
                }
                // Подсветка всех сущностей сквозь стены
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(30.0), e -> e.isAlive() && e != player)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                }
                Effects.energyBurst(level, eye, color, 30);
                Effects.sound(level, eye, SoundEvents.WITHER_SPAWN, 0.6F, 1.8F);
                return 600;
            }
            default -> {
                return -1;
            }
        }
    }

    /** Тик непрерывного лазера (40 блоков): урон, поджог, трансмутация блоков. */
    public static void laserTick(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 end = Effects.aimPoint(player, 40.0);
        int color = StoneType.MIND.getColor();

        // Луч частиц
        Effects.beam(level, Effects.dust(color, 1.2F), eye, end, 0.5);
        Effects.burst(level, ParticleTypes.ELECTRIC_SPARK, end, 2, 0.2, 0.1);

        // Урон по сущности
        HitResult hit = player.pick(40.0, 1.0F, false);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity mob && mob != player) {
            mob.hurt(mob.damageSources().magic(), 4.0F);
            mob.setRemainingFireTicks(40);
            Effects.burst(level, ParticleTypes.FLAME, mob.position(), 4, 0.3, 0.1);
        }

        // Воздействие на блоки в конце луча
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
                level.setBlock(pos, Blocks.GLASS.defaultBlockState(), 3);
            } else if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE)) {
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
            } else if (state.getDestroySpeed(level, pos) >= 0 && (state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.DIRT) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                    || state.is(net.minecraft.tags.BlockTags.LEAVES) || state.is(Blocks.VINE))) {
                level.destroyBlock(pos, false);
            }
            // Поджог земли
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir() && state.isSolid()) {
                level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
        Effects.sound(level, eye, SoundEvents.BEACON_AMBIENT, 0.3F, 2.0F);
    }
}
