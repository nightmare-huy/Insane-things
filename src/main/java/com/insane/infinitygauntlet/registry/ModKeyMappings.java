package com.insane.infinitygauntlet.registry;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Клавиши (шина MOD): V — камень, B — способность, Shift+G — Щелчок. */
@EventBusSubscriber(modid = InfinityGauntletMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.infinity_gauntlet";

    public static final KeyMapping CYCLE_STONE = new KeyMapping(
            "key.infinity_gauntlet.cycle_stone", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping CYCLE_ABILITY = new KeyMapping(
            "key.infinity_gauntlet.cycle_ability", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping SNAP = new KeyMapping(
            "key.infinity_gauntlet.snap", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private ModKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_STONE);
        event.register(CYCLE_ABILITY);
        event.register(SNAP);
    }
}
