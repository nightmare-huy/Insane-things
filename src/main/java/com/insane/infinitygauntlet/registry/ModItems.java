package com.insane.infinitygauntlet.registry;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import com.insane.infinitygauntlet.item.InfinityGauntletItem;
import com.insane.infinitygauntlet.item.StoneItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Реестр предметов: перчатка и шесть камней бесконечности.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(InfinityGauntletMod.MOD_ID);

    /** Перчатка Бесконечности — носится в руке, рендерится объёмной 3D-моделью. */
    public static final DeferredItem<InfinityGauntletItem> INFINITY_GAUNTLET =
            ITEMS.register("infinity_gauntlet", InfinityGauntletItem::new);

    public static final DeferredItem<StoneItem> SPACE_STONE =
            ITEMS.register("space_stone", () -> new StoneItem(StoneType.SPACE));
    public static final DeferredItem<StoneItem> MIND_STONE =
            ITEMS.register("mind_stone", () -> new StoneItem(StoneType.MIND));
    public static final DeferredItem<StoneItem> REALITY_STONE =
            ITEMS.register("reality_stone", () -> new StoneItem(StoneType.REALITY));
    public static final DeferredItem<StoneItem> POWER_STONE =
            ITEMS.register("power_stone", () -> new StoneItem(StoneType.POWER));
    public static final DeferredItem<StoneItem> TIME_STONE =
            ITEMS.register("time_stone", () -> new StoneItem(StoneType.TIME));
    public static final DeferredItem<StoneItem> SOUL_STONE =
            ITEMS.register("soul_stone", () -> new StoneItem(StoneType.SOUL));

    private ModItems() {
    }

    /** Предмет камня по его типу. */
    public static Item stoneItem(StoneType type) {
        return switch (type) {
            case SPACE -> SPACE_STONE.get();
            case MIND -> MIND_STONE.get();
            case REALITY -> REALITY_STONE.get();
            case POWER -> POWER_STONE.get();
            case TIME -> TIME_STONE.get();
            case SOUL -> SOUL_STONE.get();
        };
    }
}
