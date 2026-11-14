package com.insane.infinitygauntlet;

import com.insane.infinitygauntlet.ability.AbilityManager;
import com.insane.infinitygauntlet.ability.SnapHandler;
import com.insane.infinitygauntlet.ability.SoulAbilities;
import com.insane.infinitygauntlet.ability.TimeRecorder;
import com.insane.infinitygauntlet.network.ModPayloads;
import com.insane.infinitygauntlet.registry.ModCreativeTabs;
import com.insane.infinitygauntlet.registry.ModDataComponents;
import com.insane.infinitygauntlet.registry.ModItems;
import com.insane.infinitygauntlet.registry.StoneType;
import com.insane.infinitygauntlet.util.GauntletHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс мода «Перчатка Бесконечности».
 * Регистрирует компоненты данных, предметы, вкладки, сеть и серверные обработчики событий.
 */
@Mod(InfinityGauntletMod.MOD_ID)
public class InfinityGauntletMod {
    public static final String MOD_ID = "infinity_gauntlet";
    private static final Logger LOGGER = LoggerFactory.getLogger(InfinityGauntletMod.class);

    public InfinityGauntletMod(IEventBus modEventBus) {
        // Регистрация компонентов данных (состояние перчатки)
        ModDataComponents.COMPONENTS.register(modEventBus);
        // Регистрация предметов (перчатка и 6 камней)
        ModItems.ITEMS.register(modEventBus);
        // Регистрация творческой вкладки
        ModCreativeTabs.TABS.register(modEventBus);
        // Регистрация сетевых пакетов (PayloadRegistrar)
        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(InfinityGauntletMod::onLoadComplete);

        // Серверные обработчики: способности, щелчок, запись времени, воскрешение
        NeoForge.EVENT_BUS.addListener(AbilityManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(AbilityManager::onEntityTick);
        NeoForge.EVENT_BUS.addListener(SnapHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(TimeRecorder::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(TimeRecorder::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(TimeRecorder::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SoulAbilities::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SoulAbilities::onLivingDeath);
    }

    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            if (!BuiltInRegistries.ITEM.containsValue(ModItems.INFINITY_GAUNTLET.get())) {
                throw new IllegalStateException("Infinity Gauntlet item is not registered!");
            }
            for (StoneType stone : StoneType.values()) {
                if (!BuiltInRegistries.ITEM.containsValue(ModItems.stoneItem(stone))) {
                    throw new IllegalStateException("Missing stone item for " + stone);
                }
            }
            ItemStack full = ModCreativeTabs.createFullGauntlet();
            if (!GauntletHelper.hasAllStones(full)) {
                throw new IllegalStateException("Full gauntlet does not contain all 6 stones!");
            }
            LOGGER.info("[InfinityGauntlet] Loaded successfully: gauntlet + 6 stones + creative tab verified.");
            if (Boolean.getBoolean("infinity_gauntlet.smoke_test")) {
                System.out.println("[INFINITY_GAUNTLET_SMOKE_TEST_PASSED]");
                System.out.flush();
                Runtime.getRuntime().halt(0);
            }
        });
    }
}
