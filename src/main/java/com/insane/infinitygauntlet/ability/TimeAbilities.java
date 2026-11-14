package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.registry.StoneType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Способности Камня Времени (зелёный):
 * 1) Перемотка на 6 секунд, 2) Временная петля, 3) Стазис, 4) Ускорение времени (удержание).
 * Во время использования вокруг запястья вращаются зелёные руны (см. AbilityManager).
 */
public final class TimeAbilities {
    private TimeAbilities() {
    }

    public static long use(ServerPlayer player, ItemStack gauntlet, int ability, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        int color = StoneType.TIME.getColor();
        switch (ability) {
            case 0 -> {
                // Перемотка: вернуть мир на 6 секунд назад
                if (TimeRecorder.rewind(player, 120)) {
                    Effects.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.6F);
                    Effects.ring(level, Effects.dust(color, 1.5F), player.position(), 2.0, 24);
                }
                return 1200;
            }
            case 1 -> {
                // Временная петля: отметка состояния, авто-возврат через 10с или при смерти
                TimeRecorder.startLoop(player);
                Effects.energyBurst(level, eye, color, 24);
                Effects.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.2F);
                return 1200;
            }
            case 2 -> {
                // Стазис: заморозка мобов, гравитации и снарядов в радиусе 15 на 10 секунд
                AbilityManager.stasisMap().put(player.getUUID(),
                        new AbilityManager.StasisEntry(eye, now + 200));
                Effects.ring(level, Effects.dust(color, 1.5F), eye, 5.0, 32);
                Effects.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.8F);
                return 600;
            }
            case 3 -> {
                // Ускорение времени: удержание (см. accelerateTick)
                return -1;
            }
            default -> {
                return -1;
            }
        }
    }

    /** Тик ускорения времени: рост культур, мгновенная плавка, взросление, смена дня/ночи при взгляде в небо. */
    public static void accelerateTick(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        int r = 10;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock) {
                state.randomTick(level, pos, level.random);
            } else if (state.getBlock() instanceof FurnaceBlock
                    && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
                // Мгновенная плавка: ускоряем серверный тик печи
                for (int i = 0; i < 8; i++) {
                    net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
                }
            }
        }
        // Детёныши растут
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(16.0), e -> e instanceof AgeableMob)) {
            ((AgeableMob) entity).ageUp(60);
        }
        // Взгляд в небо: ускорение цикла дня/ночи
        if (Effects.lookVec(player).y > 0.5) {
            level.setDayTime(level.getDayTime() + 40);
        }
        Effects.burst(level, ParticleTypes.HAPPY_VILLAGER, player.position(), 2, 0.5, 0.1);
    }
}
