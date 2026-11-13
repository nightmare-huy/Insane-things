package com.insane.infinitygauntlet;

import com.insane.infinitygauntlet.ability.AbilityManager;
import com.insane.infinitygauntlet.ability.SnapHandler;
import com.insane.infinitygauntlet.ability.SoulAbilities;
import com.insane.infinitygauntlet.ability.TimeRecorder;
import com.insane.infinitygauntlet.network.ModPayloads;
import com.insane.infinitygauntlet.registry.ModCreativeTabs;
import com.insane.infinitygauntlet.registry.ModDataComponents;
import com.insane.infinitygauntlet.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Главный класс мода «Перчатка Бесконечности».
 * Регистрирует предметы, компоненты данных, сеть, клавиши и обработчики событий.
 */
@Mod(InfinityGauntletMod.MOD_ID)
public class InfinityGauntletMod {
    public static final String MOD_ID = "infinity_gauntlet";

    public InfinityGauntletMod(IEventBus modEventBus) {
        // Регистрация предметов (перчатка и 6 камней)
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        // Регистрация компонентов данных (состояние перчатки)
        ModDataComponents.COMPONENTS.register(modEventBus);
        // Регистрация сетевых пакетов (PayloadRegistrar)
        modEventBus.addListener(ModPayloads::register);
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
}
