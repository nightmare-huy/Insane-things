package com.insane.infinitygauntlet.client;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import com.insane.infinitygauntlet.network.ModPayloads.ActivateAbilityPayload;
import com.insane.infinitygauntlet.network.ModPayloads.CycleAbilityPayload;
import com.insane.infinitygauntlet.network.ModPayloads.CycleStonePayload;
import com.insane.infinitygauntlet.network.ModPayloads.SnapPayload;
import com.insane.infinitygauntlet.registry.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Клиентский тик: клавиши и отправка команд способности на сервер. */
@EventBusSubscriber(modid = InfinityGauntletMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientTickHandler {
    private static int holdCounter;

    private ClientTickHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            holdCounter = 0;
            return;
        }

        while (ModKeyMappings.CYCLE_STONE.consumeClick()) {
            PacketDistributor.sendToServer(new CycleStonePayload());
        }
        while (ModKeyMappings.CYCLE_ABILITY.consumeClick()) {
            PacketDistributor.sendToServer(new CycleAbilityPayload());
        }
        while (ModKeyMappings.SNAP.consumeClick()) {
            if (mc.player.isShiftKeyDown()) {
                PacketDistributor.sendToServer(new SnapPayload());
            }
        }

        // Для лучей/телекинеза/ускорения времени посылаем heartbeat удержания.
        if (mc.options.keyUse.isDown() && mc.screen == null) {
            if (++holdCounter % 5 == 0) {
                PacketDistributor.sendToServer(new ActivateAbilityPayload());
            }
        } else {
            holdCounter = 0;
        }

        if (ClientEffects.isShaking()) {
            mc.player.setXRot(mc.player.getXRot() + (float) (Math.random() - 0.5) * 2.0F);
            mc.player.setYRot(mc.player.getYRot() + (float) (Math.random() - 0.5) * 2.0F);
        }
    }
}
