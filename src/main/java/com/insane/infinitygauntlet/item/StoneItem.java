package com.insane.infinitygauntlet.item;

import com.insane.infinitygauntlet.registry.StoneType;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Камень бесконечности. Вставляется в перчатку правой кнопкой.
 */
public class StoneItem extends Item {
    private final StoneType stoneType;

    public StoneItem(StoneType stoneType) {
        super(new Item.Properties().stacksTo(64));
        this.stoneType = stoneType;
    }

    public StoneType getStoneType() {
        return stoneType;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.infinity_gauntlet.stone_hint").withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < 4; i++) {
            tooltip.add(Component.translatable(stoneType.abilityKey(i)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
