package com.insane.infinitygauntlet.util;

import com.insane.infinitygauntlet.registry.GauntletState;
import com.insane.infinitygauntlet.registry.ModDataComponents;
import com.insane.infinitygauntlet.registry.ModItems;
import com.insane.infinitygauntlet.registry.StoneType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Утилиты для работы с перчаткой: поиск в руке, чтение/запись состояния,
 * проверка готовности способностей.
 */
public final class GauntletHelper {
    private GauntletHelper() {
    }

    /** Перчатка в главной руке игрока или пустой стек. */
    public static ItemStack getGauntletInHand(Player player) {
        ItemStack stack = player.getMainHandItem();
        return stack.is(ModItems.INFINITY_GAUNTLET.get()) ? stack : ItemStack.EMPTY;
    }

    /** Состояние перчатки из компонента данных (или пустое). */
    public static GauntletState getState(ItemStack gauntlet) {
        GauntletState state = gauntlet.get(ModDataComponents.GAUNTLET_STATE.get());
        return state != null ? state : GauntletState.empty();
    }

    /** Записывает новое состояние в компонент данных перчатки. */
    public static void setState(ItemStack gauntlet, GauntletState state) {
        gauntlet.set(ModDataComponents.GAUNTLET_STATE.get(), state);
    }

    /** Текущий игровой тик на сервере (0 на клиенте). */
    public static long now(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getGameTime();
        }
        return 0L;
    }

    /** Перчатка «повреждена» после щелчка: способности на перезарядке. */
    public static boolean isDamaged(ItemStack gauntlet, long now) {
        return getState(gauntlet).damagedUntil() > now;
    }

    /** Все 6 камней вставлены? */
    public static boolean hasAllStones(ItemStack gauntlet) {
        GauntletState state = getState(gauntlet);
        return state.stones().size() >= StoneType.values().length;
    }

    /** Вставляет камень в перчатку. Возвращает true при успехе. */
    public static boolean insertStone(ItemStack gauntlet, StoneType type) {
        GauntletState state = getState(gauntlet);
        if (state.hasStone(type) || state.stones().size() >= StoneType.values().length) {
            return false;
        }
        List<StoneType> stones = new ArrayList<>(state.stones());
        stones.add(type);
        int active = state.activeStone() < 0 ? stones.size() - 1 : state.activeStone();
        setState(gauntlet, state.withStones(stones, active));
        return true;
    }

    /** Извлекает камень из перчатки (по индексу). Возвращает тип или null. */
    public static StoneType removeStone(ItemStack gauntlet, int index) {
        GauntletState state = getState(gauntlet);
        if (index < 0 || index >= state.stones().size()) {
            return null;
        }
        List<StoneType> stones = new ArrayList<>(state.stones());
        StoneType removed = stones.remove(index);
        int active = state.activeStone();
        if (active == index) {
            active = stones.isEmpty() ? -1 : Math.min(active, stones.size() - 1);
        } else if (active > index) {
            active--;
        }
        setState(gauntlet, state.withStones(stones, active));
        return removed;
    }

    /** Ставит перезарядку на способность активного камня. */
    public static void startCooldown(ItemStack gauntlet, StoneType stone, int ability, long now, long ticks) {
        setState(gauntlet, getState(gauntlet).withCooldown(stone, ability, now, ticks));
    }

    /** Способность готова к использованию? */
    public static boolean isReady(ItemStack gauntlet, StoneType stone, int ability, long now) {
        return getState(gauntlet).isReady(stone, ability, now);
    }
}
