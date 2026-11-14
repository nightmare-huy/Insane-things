package com.insane.infinitygauntlet.item;

import com.insane.infinitygauntlet.ability.InsertionEffects;
import com.insane.infinitygauntlet.network.ModPayloads.ActivateAbilityPayload;
import com.insane.infinitygauntlet.registry.GauntletState;
import com.insane.infinitygauntlet.registry.ModItems;
import com.insane.infinitygauntlet.registry.StoneType;
import com.insane.infinitygauntlet.util.GauntletHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Перчатка Бесконечности.
 * Правая кнопка с камнем во второй руке — вставка камня (каноничный эффект).
 * Shift+правая кнопка — извлечь активный камень.
 * Рендерится объёмной 3D-моделью через BlockEntityWithoutLevelRenderer (см. ClientSetup).
 */
public class InfinityGauntletItem extends Item {
    public InfinityGauntletItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gauntlet = player.getItemInHand(hand);
        ItemStack offhand = player.getOffhandItem();

        // Вставка камня: камень лежит во второй руке
        if (offhand.getItem() instanceof StoneItem stoneItem) {
            StoneType type = stoneItem.getStoneType();
            if (level.isClientSide) {
                return InteractionResultHolder.sidedSuccess(gauntlet, true);
            }
            if (GauntletHelper.insertStone(gauntlet, type)) {
                offhand.shrink(1);
                InsertionEffects.play((ServerLevel) level, player, type);
                return InteractionResultHolder.sidedSuccess(gauntlet, false);
            }
            player.displayClientMessage(Component.translatable("message.infinity_gauntlet.stone_already_inserted"), true);
            return InteractionResultHolder.fail(gauntlet);
        }

        // Извлечение активного камня: Shift + ПКМ без камня во второй руке
        if (player.isShiftKeyDown()) {
            GauntletState state = GauntletHelper.getState(gauntlet);
            StoneType active = state.activeType();
            if (active != null && !level.isClientSide) {
                GauntletHelper.removeStone(gauntlet, state.activeStone());
                ItemStack stoneStack = new ItemStack(ModItems.stoneItem(active));
                if (!player.getInventory().add(stoneStack)) {
                    player.drop(stoneStack, false);
                }
                player.displayClientMessage(
                        Component.translatable("message.infinity_gauntlet.stone_removed", active.getDisplayName()), true);
                return InteractionResultHolder.sidedSuccess(gauntlet, false);
            }
            return InteractionResultHolder.fail(gauntlet);
        }

        // Активация способности: клиент отправляет пакет на сервер
        if (level.isClientSide) {
            PacketDistributor.sendToServer(new ActivateAbilityPayload());
            return InteractionResultHolder.sidedSuccess(gauntlet, true);
        }
        return InteractionResultHolder.sidedSuccess(gauntlet, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GauntletState state = GauntletHelper.getState(stack);
        tooltip.add(Component.translatable("tooltip.infinity_gauntlet.hint").withStyle(ChatFormatting.GRAY));
        if (state.stones().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.infinity_gauntlet.empty").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.infinity_gauntlet.stones", state.stones().size(), 6)
                    .withStyle(ChatFormatting.GOLD));
            for (StoneType stone : state.stones()) {
                tooltip.add(Component.literal("◆ " + stone.getDisplayName()).withStyle(stone.getFormatting()));
            }
            StoneType active = state.activeType();
            if (active != null) {
                tooltip.add(Component.translatable("tooltip.infinity_gauntlet.active", active.getDisplayName())
                        .withStyle(ChatFormatting.WHITE));
                tooltip.add(Component.translatable(active.abilityKey(state.abilityIndex()))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
