package com.insane.infinitygauntlet.registry;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Вкладка творческого режима с перчаткой и шестью камнями. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InfinityGauntletMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INFINITY_GAUNTLET =
            TABS.register("infinity_gauntlet", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.infinity_gauntlet"))
                    .icon(() -> ModItems.INFINITY_GAUNTLET.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.INFINITY_GAUNTLET.get());
                        output.accept(ModItems.SPACE_STONE.get());
                        output.accept(ModItems.MIND_STONE.get());
                        output.accept(ModItems.REALITY_STONE.get());
                        output.accept(ModItems.POWER_STONE.get());
                        output.accept(ModItems.TIME_STONE.get());
                        output.accept(ModItems.SOUL_STONE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
