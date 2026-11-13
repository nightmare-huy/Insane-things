package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.registry.StoneType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Способности Камня Реальности (красный):
 * 1) Трансмутация врага, 2) Трансмутация мира, 3) Иллюзия, 4) Гравитация/ландшафт.
 * Пассив: входящие снаряды превращаются в мыльные пузыри (см. AbilityManager).
 */
public final class RealityAbilities {
    private RealityAbilities() {
    }

    public static long use(ServerPlayer player, ItemStack gauntlet, int ability, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        int color = StoneType.REALITY.getColor();
        switch (ability) {
            case 0 -> {
                // Трансмутация врага: мирное животное или парящие кубы
                HitResult hit = player.pick(20.0, 1.0F, false);
                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity mob
                        && mob != player && mob.isAlive()) {
                    Vec3 pos = mob.position();
                    mob.discard();
                    if (Math.random() < 0.5) {
                        Animal animal = Math.random() < 0.5
                                ? EntityType.CHICKEN.create(level)
                                : EntityType.SHEEP.create(level);
                        if (animal != null) {
                            animal.moveTo(pos.x, pos.y, pos.z);
                            level.addFreshEntity(animal);
                        }
                    } else {
                        // Парящие кубы-ленты
                        for (int i = 0; i < 5; i++) {
                            FallingBlockEntity cube = FallingBlockEntity.fall(level,
                                    BlockPos.containing(pos).offset((int) (Math.random() * 3) - 1, i, (int) (Math.random() * 3) - 1),
                                    Blocks.RED_WOOL.defaultBlockState());
                            cube.setNoGravity(true);
                            cube.setDeltaMovement(0, 0.3 + Math.random() * 0.4, 0);
                        }
                    }
                    Effects.energyBurst(level, pos, color, 30);
                    Effects.sound(level, eye, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.5F);
                }
                return 100;
            }
            case 1 -> {
                // Трансмутация мира в радиусе 6 блоков
                BlockPos center = player.blockPosition();
                int r = 6;
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
                    if (pos.distSqr(center) > r * r) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.LAVA)) {
                        level.setBlock(pos, Math.random() < 0.5 ? Blocks.OBSIDIAN.defaultBlockState()
                                : Blocks.WATER.defaultBlockState(), 3);
                    } else if (state.is(Blocks.DIRT) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE)) {
                        double roll = Math.random();
                        var ore = roll < 0.4 ? Blocks.QUARTZ_BLOCK.defaultBlockState()
                                : roll < 0.6 ? Blocks.IRON_ORE.defaultBlockState()
                                : roll < 0.8 ? Blocks.GOLD_ORE.defaultBlockState()
                                : Blocks.DIAMOND_ORE.defaultBlockState();
                        level.setBlock(pos, ore, 3);
                    }
                }
                // Стены раскалываются: разрушение внешнего кольца
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 2, r))) {
                    double dist = Math.sqrt(pos.distSqr(center));
                    if (dist > r - 1.5 && dist <= r && Math.random() < 0.3) {
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0) {
                            level.destroyBlock(pos, false);
                        }
                    }
                }
                Effects.energyBurst(level, eye, color, 40);
                Effects.sound(level, eye, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.4F);
                return 400;
            }
            case 2 -> {
                // Иллюзия: невидимость + 3 клона-танка
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0));
                List<AbilityManager.CloneEntry> clones = AbilityManager.clonesMap()
                        .computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
                for (int i = 0; i < 3; i++) {
                    var golem = EntityType.IRON_GOLEM.create(level);
                    if (golem != null) {
                        Vec3 offset = new Vec3((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);
                        golem.moveTo(player.getX() + offset.x, player.getY(), player.getZ() + offset.z);
                        golem.setCustomName(net.minecraft.network.chat.Component.literal("Иллюзия"));
                        level.addFreshEntity(golem);
                        clones.add(new AbilityManager.CloneEntry(golem.getUUID(), now + 600));
                        Effects.energyBurst(level, golem.position(), color, 20);
                    }
                }
                Effects.sound(level, eye, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                return 600;
            }
            case 3 -> {
                // Гравитация/ландшафт: волны земли + притяжение врага к руке
                Vec3 front = Effects.aimPoint(player, 8.0);
                BlockPos frontPos = BlockPos.containing(front);
                for (BlockPos pos : BlockPos.betweenClosed(frontPos.offset(-4, 0, -4), frontPos.offset(4, 0, 4))) {
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0) {
                        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
                        block.setDeltaMovement((Math.random() - 0.5) * 0.5, 1.2 + Math.random(), (Math.random() - 0.5) * 0.5);
                        level.removeBlock(pos, false);
                    }
                }
                for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, front, 6.0, player)) {
                    enemy.hurt(enemy.damageSources().magic(), 8.0F);
                    enemy.setDeltaMovement(enemy.getDeltaMovement().add(0, 1.5, 0));
                }
                // Притяжение далёкого врага к руке игрока
                HitResult hit = player.pick(30.0, 1.0F, false);
                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity mob && mob != player) {
                    Vec3 hand = Effects.aimPoint(player, 2.0);
                    mob.teleportTo(hand.x, hand.y, hand.z);
                    Effects.energyBurst(level, hand, color, 24);
                }
                Effects.sound(level, eye, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.7F);
                return 200;
            }
            default -> {
                return -1;
            }
        }
    }
}
