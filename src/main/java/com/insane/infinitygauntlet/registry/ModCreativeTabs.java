package com.insane.infinitygauntlet.registry;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import com.insane.infinitygauntlet.util.GauntletHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Вкладка творческого режима с перчаткой и шестью камнями + добавление в ванильные вкладки. */
@EventBusSubscriber(modid = InfinityGauntletMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InfinityGauntletMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INFINITY_GAUNTLET =
            TABS.register("infinity_gauntlet", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.infinity_gauntlet"))
                    .icon(ModCreativeTabs::createFullGauntlet)
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.INFINITY_GAUNTLET.get());
                        output.accept(createFullGauntlet());
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

    /** Создаёт Перчатку Бесконечности со всеми 6 вставленными камнями. */
    public static ItemStack createFullGauntlet() {
        ItemStack full = new ItemStack(ModItems.INFINITY_GAUNTLET.get());
        for (StoneType stone : StoneType.values()) {
            GauntletHelper.insertStone(full, stone);
        }
        return full;
    }

    /** Дублируем предметы также в стандартные вкладки Бой, Инструменты и Ингредиенты. */
    @SubscribeEvent
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT
                || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.INFINITY_GAUNTLET.get());
            event.accept(createFullGauntlet());
            event.accept(ModItems.SPACE_STONE.get());
            event.accept(ModItems.MIND_STONE.get());
            event.accept(ModItems.REALITY_STONE.get());
            event.accept(ModItems.POWER_STONE.get());
            event.accept(ModItems.TIME_STONE.get());
            event.accept(ModItems.SOUL_STONE.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.SPACE_STONE.get());
            event.accept(ModItems.MIND_STONE.get());
            event.accept(ModItems.REALITY_STONE.get());
            event.accept(ModItems.POWER_STONE.get());
            event.accept(ModItems.TIME_STONE.get());
            event.accept(ModItems.SOUL_STONE.get());
        }
    }
}
