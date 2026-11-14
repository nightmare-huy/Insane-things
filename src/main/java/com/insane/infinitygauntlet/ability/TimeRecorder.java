package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.registry.StoneType;
import com.insane.infinitygauntlet.util.GauntletHelper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Запись состояния игрока, сущностей и недавних изменений блоков для Камня Времени. */
public final class TimeRecorder {
    private static final int SNAPSHOT_INTERVAL = 5;
    private static final int MAX_SNAPSHOTS = 60; // 15 секунд истории
    private static final int INVENTORY_SIZE = 41; // 36 основных + 4 брони + левая рука
    private static final double ENTITY_RADIUS = 64.0;

    private static final Map<UUID, Deque<Snapshot>> HISTORY = new HashMap<>();
    private static final Map<BlockKey, List<BlockChange>> BLOCK_CHANGES = new HashMap<>();
    private static final Map<UUID, LoopEntry> LOOPS = new HashMap<>();

    private record BlockKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record BlockChange(BlockState oldState, long tick) {
    }

    private record LoopEntry(Snapshot snapshot, long until) {
    }

    public record Snapshot(
            long tick,
            Vec3 pos,
            Vec3 velocity,
            float yRot,
            float xRot,
            float health,
            int food,
            float saturation,
            List<MobEffectInstance> effects,
            ItemStack[] inventory,
            Map<UUID, EntitySnapshot> entities
    ) {
    }

    public record EntitySnapshot(EntityType<?> type, Vec3 pos, float health) {
    }

    private TimeRecorder() {
    }

    /** Снимает состояние игроков с Камнем Времени и обслуживает временные петли. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        tickLoops(player);
        if (now % SNAPSHOT_INTERVAL != 0) {
            return;
        }

        ItemStack gauntlet = GauntletHelper.getGauntletInHand(player);
        if (gauntlet.isEmpty() || !GauntletHelper.getState(gauntlet).hasStone(StoneType.TIME)) {
            if (!LOOPS.containsKey(player.getUUID())) {
                HISTORY.remove(player.getUUID());
            }
            pruneBlockHistory(level, now - MAX_SNAPSHOTS * SNAPSHOT_INTERVAL);
            return;
        }

        Deque<Snapshot> history = HISTORY.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        history.addFirst(takeSnapshot(player, now));
        while (history.size() > MAX_SNAPSHOTS) {
            history.removeLast();
        }
        pruneBlockHistory(level, now - MAX_SNAPSHOTS * SNAPSHOT_INTERVAL);
    }

    private static Snapshot takeSnapshot(ServerPlayer player, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Map<UUID, EntitySnapshot> entities = new HashMap<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(ENTITY_RADIUS), e -> e != player && !(e instanceof Player) && e.isAlive())) {
            entities.put(entity.getUUID(), new EntitySnapshot(entity.getType(), entity.position(), entity.getHealth()));
        }

        ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            inventory[slot] = player.getInventory().getItem(slot).copy();
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            effects.add(new MobEffectInstance(effect));
        }
        return new Snapshot(now, player.position(), player.getDeltaMovement(), player.getYRot(), player.getXRot(),
                player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(),
                effects, inventory, entities);
    }

    /** Записывает прежнее состояние блока перед разрушением. */
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        recordBlockChange(level, event.getPos(), event.getState());
    }

    /** Записывает заменённое состояние блока перед установкой игроком или сущностью. */
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiPlace) {
            for (var snapshot : multiPlace.getReplacedBlockSnapshots()) {
                recordBlockChange(level, snapshot.getPos(), snapshot.getState(), now);
            }
        } else {
            recordBlockChange(level, event.getPos(), event.getBlockSnapshot().getState(), now);
        }
    }

    private static void recordBlockChange(ServerLevel level, BlockPos pos, BlockState oldState) {
        recordBlockChange(level, pos, oldState, level.getGameTime());
    }

    private static void recordBlockChange(ServerLevel level, BlockPos pos, BlockState oldState, long now) {
        if (!hasNearbyRecorder(level, pos)) {
            return;
        }
        BlockKey key = new BlockKey(level.dimension(), pos.immutable());
        BLOCK_CHANGES.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new BlockChange(oldState, now));
    }

    private static boolean hasNearbyRecorder(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double radiusSqr = ENTITY_RADIUS * ENTITY_RADIUS;
        for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            if ((HISTORY.containsKey(id) || LOOPS.containsKey(id))
                    && player.position().distanceToSqr(center) <= radiusSqr) {
                return true;
            }
        }
        return false;
    }

    /** Перематывает игрока на заданное число тиков (обычно 120 = 6 секунд). */
    public static boolean rewind(ServerPlayer player, long ticksAgo) {
        Deque<Snapshot> history = HISTORY.get(player.getUUID());
        if (history == null || history.isEmpty()) {
            return false;
        }
        long now = player.level().getGameTime();
        Snapshot target = null;
        for (Snapshot snapshot : history) { // очередь от новых к старым
            if (now - snapshot.tick() >= ticksAgo) {
                target = snapshot;
                break;
            }
        }
        if (target == null) {
            target = history.peekLast();
        }
        restore(player, target);
        restoreBlocks((ServerLevel) player.level(), target.tick());
        return true;
    }

    /** Ставит отметку временной петли; через 10 секунд игрок автоматически вернётся к ней. */
    public static void startLoop(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        LOOPS.put(player.getUUID(), new LoopEntry(takeSnapshot(player, level.getGameTime()), level.getGameTime() + 200));
    }

    private static void tickLoops(ServerPlayer player) {
        LoopEntry loop = LOOPS.get(player.getUUID());
        if (loop == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (level.getGameTime() >= loop.until()) {
            LOOPS.remove(player.getUUID());
            restore(player, loop.snapshot());
            restoreBlocks(level, loop.snapshot().tick());
        }
    }

    public static boolean hasLoop(ServerPlayer player) {
        return LOOPS.containsKey(player.getUUID());
    }

    /** Отменяет смертельный урон возвратом к отметке петли. */
    public static boolean rewindOnDeath(ServerPlayer player) {
        LoopEntry loop = LOOPS.remove(player.getUUID());
        if (loop == null) {
            return false;
        }
        restore(player, loop.snapshot());
        restoreBlocks((ServerLevel) player.level(), loop.snapshot().tick());
        return true;
    }

    private static void restore(ServerPlayer player, Snapshot snapshot) {
        ServerLevel level = (ServerLevel) player.level();
        player.teleportTo(snapshot.pos().x, snapshot.pos().y, snapshot.pos().z);
        player.setDeltaMovement(snapshot.velocity());
        player.setYRot(snapshot.yRot());
        player.setXRot(snapshot.xRot());
        player.setHealth(Math.min(player.getMaxHealth(), snapshot.health()));
        player.getFoodData().setFoodLevel(snapshot.food());
        player.getFoodData().setSaturation(snapshot.saturation());
        player.removeAllEffects();
        for (MobEffectInstance effect : snapshot.effects()) {
            player.addEffect(new MobEffectInstance(effect));
        }
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            player.getInventory().setItem(slot, snapshot.inventory()[slot].copy());
        }
        player.getInventory().setChanged();

        // Перемещение и лечение сущностей, а также повторное появление убитых мобов из снимка.
        for (Map.Entry<UUID, EntitySnapshot> entry : snapshot.entities().entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            EntitySnapshot saved = entry.getValue();
            LivingEntity mob;
            if (entity instanceof LivingEntity living && living.isAlive()) {
                mob = living;
            } else {
                mob = createRestoredMob(level, entry.getKey(), saved);
                if (mob == null) {
                    continue;
                }
            }
            mob.teleportTo(saved.pos().x, saved.pos().y, saved.pos().z);
            mob.setHealth(Math.min(mob.getMaxHealth(), saved.health()));
        }
        Effects.energyBurst(level, player.getEyePosition(), StoneType.TIME.getColor(), 30);
    }

    private static LivingEntity createRestoredMob(ServerLevel level, UUID id, EntitySnapshot saved) {
        Entity created = saved.type().create(level);
        if (!(created instanceof LivingEntity mob)) {
            return null;
        }
        mob.setUUID(id);
        mob.moveTo(saved.pos().x, saved.pos().y, saved.pos().z, mob.getYRot(), mob.getXRot());
        mob.setHealth(Math.min(mob.getMaxHealth(), saved.health()));
        if (level.addFreshEntity(mob)) {
            return mob;
        }
        return null;
    }

    /** Возвращает состояния блоков после записанного тика (от новых изменений к старым). */
    private static void restoreBlocks(ServerLevel level, long sinceTick) {
        ResourceKey<Level> dimension = level.dimension();
        List<Map.Entry<BlockKey, BlockChange>> changes = new ArrayList<>();
        for (Map.Entry<BlockKey, List<BlockChange>> entry : BLOCK_CHANGES.entrySet()) {
            if (entry.getKey().dimension().equals(dimension)) {
                for (BlockChange change : entry.getValue()) {
                    if (change.tick() >= sinceTick) {
                        changes.add(Map.entry(entry.getKey(), change));
                    }
                }
            }
        }
        changes.sort(Comparator.comparingLong((Map.Entry<BlockKey, BlockChange> e) -> e.getValue().tick()).reversed());
        for (Map.Entry<BlockKey, BlockChange> entry : changes) {
            level.setBlock(entry.getKey().pos(), entry.getValue().oldState(), 3);
            List<BlockChange> history = BLOCK_CHANGES.get(entry.getKey());
            if (history != null) {
                history.remove(entry.getValue());
                if (history.isEmpty()) {
                    BLOCK_CHANGES.remove(entry.getKey());
                }
            }
        }
    }

    private static void pruneBlockHistory(ServerLevel level, long oldestTick) {
        ResourceKey<Level> dimension = level.dimension();
        BLOCK_CHANGES.entrySet().removeIf(entry -> {
            if (!entry.getKey().dimension().equals(dimension)) {
                return false;
            }
            entry.getValue().removeIf(change -> change.tick() < oldestTick);
            return entry.getValue().isEmpty();
        });
    }
}
