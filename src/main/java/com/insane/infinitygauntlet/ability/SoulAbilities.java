package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.network.ClientFlashPayload;
import com.insane.infinitygauntlet.registry.GauntletState;
import com.insane.infinitygauntlet.registry.StoneType;
import com.insane.infinitygauntlet.util.GauntletHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Способности Камня Души (оранжевый):
 * 1) Астральный удар, 2) Сбор душ, 3) Повелитель душ (призраки на 45с),
 * 4) Зов Мира Душ (+ пассивное воскрешение при смертельном уроне с перезарядкой 5 минут).
 */
public final class SoulAbilities {
    /** Перезарядка воскрешения: 5 минут (6000 тиков). */
    private static final long RESURRECTION_COOLDOWN = 6000L;

    private SoulAbilities() {
    }

    public static long use(ServerPlayer player, ItemStack gauntlet, int ability, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        int color = StoneType.SOUL.getColor();
        switch (ability) {
            case 0 -> {
                // Астральный удар: проекция души, чистый урон сквозь броню и щиты + оглушение
                HitResult hit = player.pick(25.0, 1.0F, false);
                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity mob && mob != player) {
                    Vec3 projection = mob.position().add(0, 1.5, 0);
                    Effects.burst(level, ParticleTypes.SOUL_FIRE_FLAME, projection, 30, 0.5, 0.2);
                    Effects.burst(level, ParticleTypes.SOUL, projection, 20, 0.5, 0.1);
                    Effects.pureDamage(mob, player, 20.0F);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 255));
                    Effects.sound(level, projection, SoundEvents.WITHER_SPAWN, 0.8F, 1.4F);
                }
                for (LivingEntity entity : AbilityManager.nearbyEnemies(level, eye, 15.0, player)) {
                    if (entity instanceof Phantom || entity.hasEffect(MobEffects.INVISIBILITY)) {
                        entity.hurt(entity.damageSources().magic(), 1000.0F);
                        Effects.energyBurst(level, entity.position(), color, 20);
                    }
                }
                return 100;
            }
            case 1 -> {
                // Сбор душ: оранжевые нити к мобам в радиусе 12, вытягивание HP и сытости
                int drained = 0;
                for (LivingEntity mob : AbilityManager.nearbyEnemies(level, eye, 12.0, player)) {
                    Effects.beam(level, Effects.dust(color, 1.2F), eye, mob.position().add(0, 1, 0), 0.4);
                    mob.hurt(mob.damageSources().magic(), 4.0F);
                    drained++;
                }
                if (drained > 0) {
                    player.heal(drained * 1.5F);
                    player.getFoodData().eat(2 * drained, 1.0F);
                    Effects.sound(level, eye, SoundEvents.WITHER_SPAWN, 0.7F, 1.6F);
                }
                return 200;
            }
            case 2 -> {
                // Повелитель душ: армия огненных призраков из запомненных душ на 45 секунд (900 тиков)
                GauntletState state = GauntletHelper.getState(gauntlet);
                List<String> souls = state.souls();
                if (souls.isEmpty()) {
                    player.displayClientMessage(Component.translatable("message.infinity_gauntlet.no_souls"), true);
                    return 60;
                }
                List<AbilityManager.CloneEntry> activeSummons = AbilityManager.clonesMap()
                        .computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
                List<UUID> controlled = AbilityManager.controlledMap()
                        .computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
                for (String soulId : souls) {
                    if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(soulId))) {
                        continue;
                    }
                    var ghost = EntityType.BLAZE.create(level);
                    if (ghost != null) {
                        Vec3 offset = new Vec3((Math.random() - 0.5) * 6, 1, (Math.random() - 0.5) * 6);
                        ghost.moveTo(player.getX() + offset.x, player.getY() + offset.y, player.getZ() + offset.z);
                        ghost.setCustomName(Component.literal("Призрак Души"));
                        ghost.addEffect(new MobEffectInstance(MobEffects.GLOWING, 900, 0));
                        level.addFreshEntity(ghost);
                        activeSummons.add(new AbilityManager.CloneEntry(ghost.getUUID(), now + 900));
                        controlled.add(ghost.getUUID());

                        LivingEntity enemy = AbilityManager.nearbyEnemies(level, ghost.position(), 20.0, player)
                                .stream()
                                .filter(e -> !controlled.contains(e.getUUID()))
                                .min(Comparator.comparingDouble(e -> e.distanceToSqr(ghost)))
                                .orElse(null);
                        if (enemy != null) {
                            ghost.setTarget(enemy);
                        }
                        Effects.burst(level, ParticleTypes.SOUL_FIRE_FLAME, ghost.position(), 16, 0.4, 0.2);
                    }
                }
                Effects.sound(level, eye, SoundEvents.WITHER_SPAWN, 0.9F, 1.2F);
                return 900;
            }
            case 3 -> {
                // Зов Мира Душ: волна Огня Душ, щит душ владельцу и статус пассивного воскрешения
                Effects.ring(level, ParticleTypes.SOUL_FIRE_FLAME, player.position(), 5.0, 36);
                Effects.ring(level, Effects.dust(color, 1.6F), player.position(), 10.0, 48);
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 2));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, eye, 12.0, player)) {
                    enemy.setRemainingFireTicks(140);
                    enemy.hurt(enemy.damageSources().magic(), 8.0F);
                    Effects.burst(level, ParticleTypes.SOUL, enemy.position().add(0, 1, 0), 12, 0.3, 0.1);
                }
                Effects.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.9F);
                return 400;
            }
            default -> {
                return -1;
            }
        }
    }

    /** Смертельный урон: отмена смерти, полное лечение, оранжевый фильтр, поджог врагов (пассив 5 мин). */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Временная петля перехватывает смерть первой
        if (TimeRecorder.hasLoop(player) && event.getAmount() >= player.getHealth()) {
            event.setCanceled(true);
            TimeRecorder.rewindOnDeath(player);
            return;
        }
        ItemStack gauntlet = GauntletHelper.getGauntletInHand(player);
        if (gauntlet.isEmpty()) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        GauntletState state = GauntletHelper.getState(gauntlet);
        if (!state.hasStone(StoneType.SOUL) || state.soulReadyAt() > now) {
            return;
        }
        if (event.getAmount() < player.getHealth()) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
        PacketDistributor.sendToPlayer(player, new ClientFlashPayload(0xFFE07B2A, 2000, 0));
        for (LivingEntity enemy : AbilityManager.nearbyEnemies(level, player.getEyePosition(), 10.0, player)) {
            enemy.setRemainingFireTicks(100);
            Effects.burst(level, ParticleTypes.SOUL_FIRE_FLAME, enemy.position(), 12, 0.4, 0.2);
        }
        Effects.sound(level, player.getEyePosition(), SoundEvents.WITHER_SPAWN, 1.0F, 1.0F);
        GauntletHelper.setState(gauntlet, state.withSoulReadyAt(now + RESURRECTION_COOLDOWN));
    }

    /** Запоминание душ убитых мобов (до 10) для Повелителя душ. */
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack gauntlet = GauntletHelper.getGauntletInHand(player);
        if (gauntlet.isEmpty()) {
            return;
        }
        GauntletState state = GauntletHelper.getState(gauntlet);
        if (!state.hasStone(StoneType.SOUL)) {
            return;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
        List<String> souls = new ArrayList<>(state.souls());
        souls.add(id);
        while (souls.size() > 10) {
            souls.remove(0);
        }
        GauntletHelper.setState(gauntlet, state.withSouls(souls));
    }
}
