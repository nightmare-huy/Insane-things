package com.insane.infinitygauntlet.client;

import com.insane.infinitygauntlet.registry.GauntletState;
import com.insane.infinitygauntlet.registry.StoneType;
import com.insane.infinitygauntlet.util.GauntletHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Рендерер перчатки; отображает вставленные камни отдельными светящимися мешами. */
public class InfinityGauntletRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation GAUNTLET_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("infinity_gauntlet", "textures/entity/gauntlet.png");

    public InfinityGauntletRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        GauntletState state = GauntletHelper.getState(stack);
        InfinityGauntletModel model = new InfinityGauntletModel(
                Minecraft.getInstance().getEntityModels().bakeLayer(InfinityGauntletModel.LAYER));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyDisplayTransform(displayContext, poseStack);

        model.hideStones();
        model.root.render(poseStack, buffer.getBuffer(RenderType.entityCutout(GAUNTLET_TEXTURE)), packedLight, packedOverlay);

        float time = (System.currentTimeMillis() % 100000L) / 1000.0F;
        for (StoneType stone : state.stones()) {
            var stonePart = model.stonePart(stone);
            stonePart.visible = true;
            boolean active = stone == state.activeType();
            poseStack.pushPose();
            if (active) {
                float pulse = 1.0F + 0.12F * (float) Math.sin(time * 6.0F);
                poseStack.scale(pulse, pulse, pulse);
            }
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                    "infinity_gauntlet", "textures/entity/stone_" + stone.getId() + ".png");
            stonePart.render(poseStack, buffer.getBuffer(RenderType.entityCutout(texture)),
                    active ? 0xF000F0 : packedLight, packedOverlay);
            poseStack.popPose();
            stonePart.visible = false;
        }
        poseStack.popPose();
    }

    private static void applyDisplayTransform(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.05F, -0.25F, 0.15F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-10.0F));
                poseStack.scale(1.1F, 1.1F, 1.1F);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(-0.05F, -0.25F, 0.15F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(10.0F));
                poseStack.scale(1.1F, 1.1F, 1.1F);
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.0F, -0.15F, 0.15F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, -0.15F, 0.15F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case GUI -> {
                poseStack.translate(0.0F, -0.25F, 0.25F);
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(1.25F, 1.25F, 1.25F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, -0.1F, 0.15F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.scale(0.8F, 0.8F, 0.8F);
            }
            case FIXED -> {
                poseStack.translate(0.0F, -0.2F, 0.2F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(1.0F, 1.0F, 1.0F);
            }
            case HEAD -> {
                poseStack.translate(0.0F, -0.35F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.scale(0.8F, 0.8F, 0.8F);
            }
            default -> poseStack.scale(1.0F, 1.0F, 1.0F);
        }
    }
}
