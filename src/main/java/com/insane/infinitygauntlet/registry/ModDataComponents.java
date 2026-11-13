package com.insane.infinitygauntlet.registry;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Реестр компонентов данных (новая система DataComponents, заменившая NBT на ItemStack).
 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(InfinityGauntletMod.MOD_ID);

    /** Состояние перчатки: вставленные камни, активный камень, перезарядки, души. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GauntletState>> GAUNTLET_STATE =
            COMPONENTS.registerComponentType(
                    "gauntlet_state",
                    builder -> builder
                            .persistent(GauntletState.CODEC)
                            .networkSynchronized(GauntletState.STREAM_CODEC)
            );

    private ModDataComponents() {
    }
}
