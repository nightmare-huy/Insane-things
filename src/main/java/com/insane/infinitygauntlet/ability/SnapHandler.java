package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.network.ClientFlashPayload;
import com.insane.infinitygauntlet.registry.GauntletState;
import com.insane.infinitygauntlet.registry.StoneType;
import com.insane.infinitygauntlet.util.GauntletHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Щелчок (Shift+G): требует все 6 камней.
 * Подъём перчатки, молнии шести цветов, металлический щелчок, белая вспышка 1с,
 * затем ровно 50% живых сущностей в радиусе 150 блоков (кроме владельца и прирученных)
 * медленно растворяются в пепел за 4 секунды.
 * Перчатка дымится 2 минуты, владелец остаётся с 2 сердцами + слабость/замедление 30с.
 */
public final class SnapHandler {
    /** Жертвы щелчка: UUID сущности -> тик исчезновения. */
    private static final List<Victim> VICTIMS = new ArrayList<>();

    private record Victim(UUID id, long vanishAt, ServerLevel level) {
    }

    private SnapHandler() {
    }

    public static void trySnap(ServerPlayer player) {
        ItemStack gauntlet = GauntletHelper.getGauntletInHand(player);
        if (gauntlet.isEmpty()) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        GauntletState state = GauntletHelper.getState(gauntlet);

        if (!GauntletHelper.hasAllStones(gauntlet)) {
            player.displayClientMessage(Component.translatable("message.infinity_gauntlet.snap_need_stones"), true);
            return;
        }
        if (state.snapReadyAt() > now || state.damagedUntil() > now) {
            player.displayClientMessage(Component.translatable("message.infinity_gauntlet.damaged"), true);
            return;
        }

        Vec3 eye = player.getEyePosition();

        // Подъём перчатки: молнии шести цветов
        for (StoneType stone : StoneType.values()) {
            Vec3 offset = new Vec3((Math.random() - 0.5) * 3, Math.random() * 2, (Math.random() - 0.5) * 3);
            Effects.lightning(level, eye.x + offset.x, eye.y + offset.y, eye.z + offset.z);
            Effects.energyBurst(level, eye.add(offset), stone.getColor(), 16);
        }
        // Металлический щелчок
        Effects.sound(level, eye, SoundEvents.ANVIL_LAND, 1.0F, 0.7F);
        Effects.sound(level, eye, SoundEvents.FIREWORK_ROCKET_BLAST, 1.0F, 0.8F);
        Effects.sound(level, eye, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 1.0F);

        // Белая вспышка 1 секунда + тряска камеры
        PacketDistributor.sendToPlayer(player, new ClientFlashPayload(0xFFFFFFFF, 1000, 1000));

        // Ровно 50% живых сущностей в радиусе 150 блоков (кроме владельца и прирученных питомцев)
        AABB box = player.getBoundingBox().inflate(150.0);
        List<LivingEntity> candidates = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive())) {
            if (entity == player) {
                continue;
            }
            if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
                continue; // прирученные питомцы защищены
            }
            candidates.add(entity);
        }
        Collections.shuffle(candidates);
        int victimCount = candidates.size() / 2;
        for (int i = 0; i < victimCount; i++) {
            VICTIMS.add(new Victim(candidates.get(i).getUUID(), now + 80, level)); // 4 секунды
        }

        // Перчатка дымится и трескается 2 минуты
        GauntletHelper.setState(gauntlet, state
                .withDamagedUntil(now + 2400)
                .withSnapReadyAt(now + 2400));

        // Владелец: 2 сердца + слабость и замедление 30 секунд
        player.setHealth(4.0F);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 1));
        Effects.sound(level, eye, SoundEvents.WITHER_SPAWN, 0.8F, 0.8F);
    }

    /** Тик жертв: замедление, дрожь, пепел — затем исчезновение. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (VICTIMS.isEmpty() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        VICTIMS.removeIf(victim -> {
            if (victim.level() != level) {
                return false;
            }
            var entity = level.getEntity(victim.id());
            if (!(entity instanceof LivingEntity mob) || !mob.isAlive()) {
                return true;
            }
            if (now >= victim.vanishAt()) {
                // Исчезновение в пепел
                Effects.burst(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, mob.position().add(0, 1, 0), 40, 0.6, 0.1);
                Effects.burst(level, Effects.dust(0x8B7355, 1.5F), mob.position().add(0, 1, 0), 20, 0.5, 0.1);
                mob.discard();
                return true;
            }
            // Замедление и дрожь
            mob.setDeltaMovement(Vec3.ZERO);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 255));
            Vec3 shakePos = mob.position().add((Math.random() - 0.5) * 0.2, 0, (Math.random() - 0.5) * 0.2);
            Effects.burst(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, shakePos, 3, 0.3, 0.05);
            return false;
        });
    }
}
