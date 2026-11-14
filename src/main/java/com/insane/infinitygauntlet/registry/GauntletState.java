package com.insane.infinitygauntlet.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Полное состояние перчатки, хранящееся в DataComponent.
 * Все поля сохраняются в NBT (через Codec) и синхронизируются по сети (StreamCodec).
 */
public record GauntletState(
        List<StoneType> stones,      // вставленные камни (до 6)
        int activeStone,             // индекс активного камня в списке, -1 — нет
        int abilityIndex,            // текущая способность 0..3
        List<Long> cooldowns,        // 24 значения: stone.ordinal()*4+ability -> игровой тик готовности
        long snapReadyAt,            // тик, когда щелчок снова доступен
        long soulReadyAt,            // тик, когда воскрешение снова доступно
        List<String> souls,          // запомненные души мобов (id типов сущностей, до 10)
        long damagedUntil            // перчатка дымится/трескается до этого тика (после щелчка)
) {
    public static final int COOLDOWN_COUNT = 24;

    public static final Codec<GauntletState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("stones").forGetter(GauntletState::stonesRaw),
            Codec.INT.fieldOf("active_stone").forGetter(GauntletState::activeStone),
            Codec.INT.fieldOf("ability_index").forGetter(GauntletState::abilityIndex),
            Codec.LONG.listOf().fieldOf("cooldowns").forGetter(GauntletState::cooldowns),
            Codec.LONG.fieldOf("snap_ready_at").forGetter(GauntletState::snapReadyAt),
            Codec.LONG.fieldOf("soul_ready_at").forGetter(GauntletState::soulReadyAt),
            Codec.STRING.listOf().fieldOf("souls").forGetter(GauntletState::souls),
            Codec.LONG.fieldOf("damaged_until").forGetter(GauntletState::damagedUntil)
    ).apply(instance, (stones, active, ability, cooldowns, snap, soul, souls, damaged) -> {
        List<StoneType> parsed = new ArrayList<>();
        for (String id : stones) {
            StoneType type = StoneType.byId(id);
            if (type != null) {
                parsed.add(type);
            }
        }
        return new GauntletState(parsed, active, ability, cooldowns, snap, soul, souls, damaged);
    }));

    public static final StreamCodec<FriendlyByteBuf, GauntletState> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GauntletState decode(FriendlyByteBuf buf) {
            int stoneCount = buf.readVarInt();
            List<StoneType> stones = new ArrayList<>();
            for (int i = 0; i < stoneCount; i++) {
                StoneType type = StoneType.byId(buf.readUtf());
                if (type != null) {
                    stones.add(type);
                }
            }
            int active = buf.readVarInt();
            int ability = buf.readVarInt();
            List<Long> cooldowns = new ArrayList<>();
            for (int i = 0; i < COOLDOWN_COUNT; i++) {
                cooldowns.add(buf.readLong());
            }
            long snap = buf.readLong();
            long soul = buf.readLong();
            int soulCount = buf.readVarInt();
            List<String> souls = new ArrayList<>();
            for (int i = 0; i < soulCount; i++) {
                souls.add(buf.readUtf());
            }
            long damaged = buf.readLong();
            return new GauntletState(stones, active, ability, cooldowns, snap, soul, souls, damaged);
        }

        @Override
        public void encode(FriendlyByteBuf buf, GauntletState state) {
            buf.writeVarInt(state.stones.size());
            for (StoneType stone : state.stones) {
                buf.writeUtf(stone.getId());
            }
            buf.writeVarInt(state.activeStone);
            buf.writeVarInt(state.abilityIndex);
            for (int i = 0; i < COOLDOWN_COUNT; i++) {
                buf.writeLong(i < state.cooldowns.size() ? state.cooldowns.get(i) : 0L);
            }
            buf.writeLong(state.snapReadyAt);
            buf.writeLong(state.soulReadyAt);
            buf.writeVarInt(state.souls.size());
            for (String soul : state.souls) {
                buf.writeUtf(soul);
            }
            buf.writeLong(state.damagedUntil);
        }
    };

    /** Сырые id камней для сериализации в NBT. */
    public List<String> stonesRaw() {
        List<String> raw = new ArrayList<>();
        for (StoneType stone : stones) {
            raw.add(stone.getId());
        }
        return raw;
    }

    /** Начальное пустое состояние новой перчатки. */
    public static GauntletState empty() {
        List<Long> cooldowns = new ArrayList<>();
        for (int i = 0; i < COOLDOWN_COUNT; i++) {
            cooldowns.add(0L);
        }
        return new GauntletState(new ArrayList<>(), -1, 0, cooldowns, 0L, 0L, new ArrayList<>(), 0L);
    }

    /** Активный камень или null, если камни не вставлены. */
    public StoneType activeType() {
        if (activeStone < 0 || activeStone >= stones.size()) {
            return null;
        }
        return stones.get(activeStone);
    }

    public boolean hasStone(StoneType type) {
        return stones.contains(type);
    }

    /** Остаток перезарядки способности в тиках (0 — готова). */
    public long cooldownLeft(StoneType stone, int ability, long now) {
        int idx = stone.ordinal() * 4 + ability;
        long until = idx < cooldowns.size() ? cooldowns.get(idx) : 0L;
        return Math.max(0L, until - now);
    }

    public boolean isReady(StoneType stone, int ability, long now) {
        return cooldownLeft(stone, ability, now) <= 0;
    }

    /** Возвращает копию состояния с установленной перезарядкой. */
    public GauntletState withCooldown(StoneType stone, int ability, long now, long ticks) {
        List<Long> copy = new ArrayList<>(cooldowns);
        while (copy.size() < COOLDOWN_COUNT) {
            copy.add(0L);
        }
        copy.set(stone.ordinal() * 4 + ability, now + ticks);
        return new GauntletState(stones, activeStone, abilityIndex, copy, snapReadyAt, soulReadyAt, souls, damagedUntil);
    }

    public GauntletState withActiveStone(int index) {
        return new GauntletState(stones, index, 0, cooldowns, snapReadyAt, soulReadyAt, souls, damagedUntil);
    }

    public GauntletState withAbility(int index) {
        return new GauntletState(stones, activeStone, index, cooldowns, snapReadyAt, soulReadyAt, souls, damagedUntil);
    }

    public GauntletState withStones(List<StoneType> newStones, int newActive) {
        return new GauntletState(newStones, newActive, abilityIndex, cooldowns, snapReadyAt, soulReadyAt, souls, damagedUntil);
    }

    public GauntletState withSnapReadyAt(long value) {
        return new GauntletState(stones, activeStone, abilityIndex, cooldowns, value, soulReadyAt, souls, damagedUntil);
    }

    public GauntletState withSoulReadyAt(long value) {
        return new GauntletState(stones, activeStone, abilityIndex, cooldowns, snapReadyAt, value, souls, damagedUntil);
    }

    public GauntletState withDamagedUntil(long value) {
        return new GauntletState(stones, activeStone, abilityIndex, cooldowns, snapReadyAt, soulReadyAt, souls, value);
    }

    public GauntletState withSouls(List<String> newSouls) {
        return new GauntletState(stones, activeStone, abilityIndex, cooldowns, snapReadyAt, soulReadyAt, newSouls, damagedUntil);
    }
}
