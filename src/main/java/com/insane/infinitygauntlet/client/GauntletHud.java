package com.insane.infinitygauntlet.client;

import com.insane.infinitygauntlet.registry.GauntletState;
import com.insane.infinitygauntlet.registry.StoneType;
import com.insane.infinitygauntlet.util.GauntletHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * HUD над хотбаром: активный камень, текущая способность и перезарядка
 * (полоса цвета камня). Также рисует белую вспышку щелчка.
 */
public final class GauntletHud {
    private GauntletHud() {
    }

    public static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        // Вспышка на весь экран (белая — щелчок, оранжевая — воскрешение)
        if (ClientEffects.isFlashing()) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ClientEffects.getFlashColor());
        }

        ItemStack gauntlet = GauntletHelper.getGauntletInHand(mc.player);
        if (gauntlet.isEmpty()) {
            return;
        }
        GauntletState state = GauntletHelper.getState(gauntlet);
        StoneType active = state.activeType();

        int centerX = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - 52; // над хотбаром

        if (active == null) {
            Component empty = Component.translatable("hud.infinity_gauntlet.no_stones");
            graphics.drawString(mc.font, empty, centerX - mc.font.width(empty) / 2, y, 0xAAAAAA, true);
            return;
        }

        // Название активного камня цветом камня
        Component stoneName = Component.literal(active.getDisplayName()).withStyle(active.getFormatting());
        graphics.drawString(mc.font, stoneName, centerX - mc.font.width(stoneName) / 2, y, 0xFFFFFF, true);

        // Текущая способность
        Component ability = Component.translatable(active.abilityKey(state.abilityIndex()));
        graphics.drawString(mc.font, ability, centerX - mc.font.width(ability) / 2, y + 10, 0x55FFFF, true);

        // Полоса перезарядки цветом камня (ширина 120 px)
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        long left = state.cooldownLeft(active, state.abilityIndex(), now);
        int barWidth = 120;
        int barX = centerX - barWidth / 2;
        int barY = y + 22;
        graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 5, 0xFF000000);
        if (left > 0) {
            float frac = Math.min(1.0F, left / 200.0F);
            int filled = (int) (barWidth * (1.0F - frac));
            graphics.fill(barX, barY, barX + filled, barY + 4, 0xFF000000 | active.getColor());
            Component cd = Component.translatable("hud.infinity_gauntlet.cooldown", String.format("%.1f", left / 20.0F));
            graphics.drawString(mc.font, cd, centerX - mc.font.width(cd) / 2, barY + 7, 0xAAAAAA, true);
        } else {
            graphics.fill(barX, barY, barX + barWidth, barY + 4, 0xFF000000 | active.getColor());
        }

        // Индикатор «перчатка повреждена» после щелчка
        if (state.damagedUntil() > now) {
            Component damaged = Component.translatable("hud.infinity_gauntlet.damaged");
            graphics.drawString(mc.font, damaged, centerX - mc.font.width(damaged) / 2, barY + 18, 0xFF5555, true);
        }
    }
}
