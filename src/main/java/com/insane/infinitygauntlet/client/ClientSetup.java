package com.insane.infinitygauntlet.client;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Клиентская настройка: слой модели перчатки и HUD над игровым интерфейсом. */
@EventBusSubscriber(modid = InfinityGauntletMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(InfinityGauntletModel.LAYER, InfinityGauntletModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        LayeredDraw.Layer hud = GauntletHud::render;
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(InfinityGauntletMod.MOD_ID, "gauntlet_hud"), hud);
    }
}
