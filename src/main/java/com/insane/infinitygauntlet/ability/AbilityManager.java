package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.registry.GauntletState;
import com.insane.infinitygauntlet.registry.StoneType;
import com.insane.infinitygauntlet.util.GauntletHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Диспетчер способностей: переключение камня/способности (V/B),
 * активация ПКМ, удержание и общие тиковые эффекты.
 */
public final class AbilityManager {
    /** Удержание ПКМ: игрок -> тик окончания удержания. */
    private static final Map<UUID, Long> HOLD_UNTIL = new HashMap<>();
    /** Телекинез: игрок -> удерживаемая сущность. */
    private static final Map<UUID, Entity> TELEKINESIS = new HashMap<>();
    /** Фазовый сдвиг: игрок -> тик окончания. */
    private static final Map<UUID, Long> PHASE_UNTIL = new HashMap<>();
    /** Контроль разума: игрок -> список UUID подконтрольных мобов. */
    private static final Map<UUID, List<UUID>> CONTROLLED = new HashMap<>();
    /** Иллюзии и призраки душ: игрок -> список (UUID сущности, тик исчезновения). */
    private static final Map<UUID, List<CloneEntry>> CLONES = new HashMap<>();
    /** Стазис: игрок -> (центр, тик окончания). */
    private static final Map<UUID, StasisEntry> STASIS = new HashMap<>();

    public record CloneEntry(UUID id, long expiresAt) {
    }

    public record StasisEntry(Vec3 center, long until) {
    }

    private AbilityManager() {
    }

    // ---------- Переключение (клавиши V и B) ----------

    public static void cycleStone(ServerPlayer player) {
        ItemStack gauntlet = GauntletHelper.getGauntletInHand(player);
        if (gauntlet.isEmpty()) {
            return;
        }
        GauntletState state = GauntletHelper.getState(gauntlet);
        if (state.stones().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.infinity_gauntlet.no_stones"), true);
            return;
        }
        int next = (state.activeStone() + 1) % state.stones().size();
        GauntletHelper.setState(gauntlet, state.withActiveStone(next));
        StoneType stone = state.stones().get(next);
        player.displayClientMessage(
                Component.literal("◆ " + stone.getDisplayName()).withStyle(stone.getFormatting()), true);
        Effects.hum((ServerLevel) player.level(), player.getEyePosition());
    }

    public static void cycleAbility(ServerPlayer player) {
        ItemStack gauntlet = GauntletHelper.getGauntletInHand(player);
        if (gauntlet.isEmpty()) {
            return;
        }
        GauntletState state = GauntletHelper.getState(gauntlet);
        StoneType active = state.activeType();
        if (active == null) {
            return;
        }
        int next = (state.abilityIndex() + 1) % 4;
        GauntletHelper.setState(gauntlet, state.withAbility(next));
        player.displayClientMessage(
                Component.translatable(active.abilityKey(next)).withStyle(net.minecraft.ChatFormatting.AQUA), true);
    }

    // ---------- Активация (ПКМ) ----------

    public static void activateAbility(ServerPlayer player) {
        ItemStack gauntlet = GauntletHelper.getGauntletInHand(player);
        if (gauntlet.isEmpty()) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        GauntletState state = GauntletHelper.getState(gauntlet);

        if (state.damagedUntil() > now) {
            player.displayClientMessage(Component.translatable("message.infinity_gauntlet.damaged"), true);
            return;
        }
        StoneType stone = state.activeType();
        if (stone == null) {
            player.displayClientMessage(Component.translatable("message.infinity_gauntlet.no_stones"), true);
            return;
        }
        int ability = state.abilityIndex();
        if (!state.isReady(stone, ability, now)) {
            long left = state.cooldownLeft(stone, ability, now);
            player.displayClientMessage(
                    Component.translatable("message.infinity_gauntlet.cooldown", String.format("%.1f", left / 20.0F)), true);
            return;
        }

        // Клиент продлевает это окно, пока удерживается ПКМ.
        HOLD_UNTIL.put(player.getUUID(), now + 12);

        long cooldown = switch (stone) {
            case SPACE -> SpaceAbilities.use(player, gauntlet, ability, now);
            case MIND -> MindAbilities.use(player, gauntlet, ability, now);
            case REALITY -> RealityAbilities.use(player, gauntlet, ability, now);
            case POWER -> PowerAbilities.use(player, gauntlet, ability, now);
            case TIME -> TimeAbilities.use(player, gauntlet, ability, now);
            case SOUL -> SoulAbilities.use(player, gauntlet, ability, now);
        };
        if (cooldown >= 0) {
            GauntletHelper.startCooldown(gauntlet, stone, ability, now, cooldown);
        }
    }

    public static boolean isHolding(ServerPlayer player) {
        Long until = HOLD_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    // ---------- Тик игрока ----------

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        ItemStack gauntlet = GauntletHelper.getGauntletInHand(player);

        if (gauntlet.isEmpty()) {
            releaseTelekinesis(player);
            return;
        }
        GauntletState state = GauntletHelper.getState(gauntlet);
        StoneType stone = state.activeType();
        boolean holding = isHolding(player);

        // Дым и трещины перчатки после Щелчка.
        if (state.damagedUntil() > now) {
            Vec3 hand = player.getEyePosition().add(Effects.lookVec(player).scale(0.5));
            Effects.burst(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, hand, 2, 0.2, 0.05);
        }

        boolean telekinesisSelected = holding && stone == StoneType.SPACE && state.abilityIndex() == 1;
        if (telekinesisSelected) {
            SpaceAbilities.telekinesisTick(player, now);
        } else {
            releaseTelekinesis(player);
        }

        if (holding && stone != null) {
            int ability = state.abilityIndex();
            if (stone == StoneType.MIND && ability == 0) {
                MindAbilities.laserTick(player);
            } else if (stone == StoneType.POWER && ability == 3) {
                PowerAbilities.streamTick(player, now);
            } else if (stone == StoneType.TIME && ability == 3) {
                TimeAbilities.accelerateTick(player);
                Vec3 wrist = player.getEyePosition().add(Effects.lookVec(player).scale(0.6));
                Effects.circle(level, Effects.dust(StoneType.TIME.getColor(), 1.2F), wrist, 0.7, 12, 0.4);
            }
        } else {
            PowerAbilities.endStream(player);
        }

        // Пассив Реальности: обезвреживаем враждебные снаряды рядом.
        if (state.hasStone(StoneType.REALITY)) {
            for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
                    player.getBoundingBox().inflate(3.0), p -> p.getOwner() != player)) {
                Effects.burst(level, ParticleTypes.WITCH, projectile.position(), 10, 0.3, 0.1);
                projectile.discard();
            }
        }

        // Фазовый сдвиг: проход сквозь блоки 5 секунд.
        Long phaseUntil = PHASE_UNTIL.get(player.getUUID());
        if (phaseUntil != null) {
            if (now < phaseUntil) {
                player.noPhysics = true;
                Effects.burst(level, Effects.dust(StoneType.SPACE.getColor(), 1.0F), player.getEyePosition(), 2, 0.4, 0.1);
            } else {
                player.noPhysics = false;
                PHASE_UNTIL.remove(player.getUUID());
            }
        }

        // Подконтрольные мобы следуют за игроком; монстры получают цель-врага.
        List<UUID> controlled = CONTROLLED.get(player.getUUID());
        if (controlled != null) {
            controlled.removeIf(id -> {
                Entity entity = level.getEntity(id);
                if (!(entity instanceof LivingEntity mob) || !mob.isAlive()) {
                    return true;
                }
                Vec3 targetPos = player.getEyePosition().add(Effects.lookVec(player).scale(3.0));
                if (mob.position().distanceTo(player.position()) > 24.0) {
                    mob.teleportTo(targetPos.x, targetPos.y, targetPos.z);
                    Effects.energyBurst(level, mob.position(), StoneType.MIND.getColor(), 8);
                } else if (mob.position().distanceTo(player.position()) > 6.0) {
                    mob.setDeltaMovement(targetPos.subtract(mob.position()).normalize().scale(0.4));
                }
                if (mob instanceof Monster monster) {
                    LivingEntity enemy = level.getEntitiesOfClass(LivingEntity.class,
                                    mob.getBoundingBox().inflate(16.0), e -> e != player && e != mob && e.isAlive()
                                            && !(e instanceof Player))
                            .stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob)))
                            .orElse(null);
                    if (enemy != null) {
                        monster.setTarget(enemy);
                    }
                }
                return false;
            });
            if (controlled.isEmpty()) {
                CONTROLLED.remove(player.getUUID());
            }
        }

        // Иллюзии растворяются через минуту.
        List<CloneEntry> clones = CLONES.get(player.getUUID());
        if (clones != null) {
            clones.removeIf(entry -> {
                Entity entity = level.getEntity(entry.id());
                if (entity == null || !entity.isAlive()) {
                    return true;
                }
                if (now >= entry.expiresAt()) {
                    Effects.energyBurst(level, entity.position(), StoneType.REALITY.getColor(), 20);
                    entity.discard();
                    return true;
                }
                return false;
            });
            if (clones.isEmpty()) {
                CLONES.remove(player.getUUID());
            }
        }

        // Стазис: заморозка мобов и снарядов в радиусе 15 блоков.
        StasisEntry stasis = STASIS.get(player.getUUID());
        if (stasis != null) {
            if (now < stasis.until()) {
                AABB area = new AABB(stasis.center().add(-15, -15, -15), stasis.center().add(15, 15, 15));
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                        e -> e != player && e.isAlive())) {
                    entity.setDeltaMovement(Vec3.ZERO);
                    entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 255));
                }
                for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area, e -> true)) {
                    projectile.setDeltaMovement(Vec3.ZERO);
                }
                Effects.circle(level, Effects.dust(StoneType.TIME.getColor(), 1.0F), stasis.center(), 3.0, 16, 0.2);
            } else {
                STASIS.remove(player.getUUID());
            }
        }
    }

    private static void releaseTelekinesis(ServerPlayer player) {
        Entity held = TELEKINESIS.remove(player.getUUID());
        if (held != null && held.isAlive()) {
            held.setNoGravity(false);
            held.setDeltaMovement(Effects.lookVec(player).scale(2.0));
        }
    }

    // ---------- Тик всех сущностей: порталы и сингулярности ----------

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();

        // Порталы: телепортация с сохранением импульса; небольшой перенос внутрь выхода исключает зацикливание.
        for (SpaceAbilities.PortalPair pair : SpaceAbilities.PORTALS) {
            if (now > pair.expiresAt() || pair.level() != level || entity == pair.owner()) {
                continue;
            }
            if (entity.position().distanceTo(pair.entry()) < 1.2) {
                Vec3 velocity = entity.getDeltaMovement();
                Vec3 offset = velocity.normalize().scale(0.8);
                Vec3 destination = pair.exit().add(offset);
                entity.teleportTo(destination.x, destination.y, destination.z);
                entity.setDeltaMovement(velocity);
                entity.resetFallDistance();
                Effects.energyBurst(level, pair.exit(), StoneType.SPACE.getColor(), 8);
            }
        }
        SpaceAbilities.PORTALS.removeIf(pair -> now > pair.expiresAt());

        // Сингулярность: тянет живых сущностей и предметы.
        for (SpaceAbilities.Singularity singularity : SpaceAbilities.SINGULARITIES) {
            if (now > singularity.until() || singularity.level() != level) {
                continue;
            }
            Vec3 center = singularity.center();
            AABB area = new AABB(center.add(-12, -12, -12), center.add(12, 12, 12));
            for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.isAlive() && e != singularity.owner())) {
                Vec3 pull = center.subtract(mob.position()).normalize().scale(0.35);
                mob.setDeltaMovement(mob.getDeltaMovement().add(pull));
            }
            for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, area, e -> true)) {
                Vec3 pull = center.subtract(item.position()).normalize().scale(0.5);
                item.setDeltaMovement(item.getDeltaMovement().add(pull));
            }
            Effects.burst(level, ParticleTypes.PORTAL, center, 4, 1.5, 0.2);
        }
        SpaceAbilities.SINGULARITIES.removeIf(s -> {
            if (now > s.until() && s.level() == level) {
                Vec3 center = s.center();
                Effects.energyBurst(level, center, StoneType.SPACE.getColor(), 40);
                Effects.sound(level, center, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 1.0F, 0.7F);
                destroyBlocks(level, center, 5.0, false);
                Player owner = s.owner() instanceof Player p ? p : null;
                for (LivingEntity enemy : nearbyEnemies(level, center, 8.0, owner)) {
                    enemy.hurt(enemy.damageSources().magic(), 20.0F);
                }
                return true;
            }
            return false;
        });
    }

    // ---------- Доступ к общим картам и helpers ----------

    public static Map<UUID, Entity> telekinesisMap() {
        return TELEKINESIS;
    }

    public static Map<UUID, Long> phaseMap() {
        return PHASE_UNTIL;
    }

    public static Map<UUID, List<UUID>> controlledMap() {
        return CONTROLLED;
    }

    public static Map<UUID, List<CloneEntry>> clonesMap() {
        return CLONES;
    }

    public static Map<UUID, StasisEntry> stasisMap() {
        return STASIS;
    }

    public static List<ServerPlayer> playersWithGauntlet(ServerLevel level) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!GauntletHelper.getGauntletInHand(player).isEmpty()) {
                result.add(player);
            }
        }
        return result;
    }

    /** Все живые сущности в радиусе от точки (кроме указанного игрока). */
    public static List<LivingEntity> nearbyEnemies(ServerLevel level, Vec3 center, double radius, Player exclude) {
        AABB area = new AABB(center.add(-radius, -radius, -radius), center.add(radius, radius, radius));
        return level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != exclude);
    }

    /** Разрушает блоки в сфере (для взрывов способностей). */
    public static void destroyBlocks(ServerLevel level, Vec3 center, double radius, boolean dropItems) {
        BlockPos centerPos = BlockPos.containing(center);
        int r = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-r, -r, -r), centerPos.offset(r, r, r))) {
            if (pos.distSqr(centerPos) <= radius * radius) {
                var state = level.getBlockState(pos);
                if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0) {
                    level.destroyBlock(pos, dropItems);
                }
            }
        }
    }
}
