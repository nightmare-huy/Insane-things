package com.insane.infinitygauntlet.client;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import com.insane.infinitygauntlet.registry.ModItems;
import com.insane.infinitygauntlet.registry.StoneType;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Клиентская настройка (шина MOD): 3D-рендерер предмета, слой модели перчатки и HUD. */
@EventBusSubscriber(modid = InfinityGauntletMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            InfinityGauntletModel model = new InfinityGauntletModel(
                    InfinityGauntletModel.createBodyLayer().bakeRoot());
            for (StoneType stone : StoneType.values()) {
                if (model.stonePart(stone) == null) {
                    throw new IllegalStateException("Missing model part for stone: " + stone);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private InfinityGauntletRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new InfinityGauntletRenderer();
                }
                return renderer;
            }
        }, ModItems.INFINITY_GAUNTLET.get());
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
