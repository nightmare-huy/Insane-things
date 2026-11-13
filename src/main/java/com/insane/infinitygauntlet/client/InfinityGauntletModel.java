package com.insane.infinitygauntlet.client;

import com.insane.infinitygauntlet.registry.StoneType;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Воксельная модель золотой перчатки с отдельными гнёздами шести камней. */
public final class InfinityGauntletModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("infinity_gauntlet", "infinity_gauntlet"), "main");

    private static final String[] STONE_PARTS = {
            "stone_space", "stone_mind", "stone_reality", "stone_power", "stone_time", "stone_soul"
    };

    public final ModelPart root;

    public InfinityGauntletModel(ModelPart root) {
        this.root = root;
        hideStones();
    }

    public ModelPart stonePart(StoneType type) {
        return root.getChild(STONE_PARTS[type.ordinal()]);
    }

    /** Скрывает гнёзда от основного прохода рендера — они рисуются отдельными текстурами. */
    public void hideStones() {
        for (String partName : STONE_PARTS) {
            root.getChild(partName).visible = false;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Манжета и ладонь.
        root.addOrReplaceChild("cuff",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 3.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("cuff_rim",
                CubeListBuilder.create().texOffs(0, 8).addBox(-3.2F, 3.0F, -0.2F, 6.4F, 1.0F, 4.4F), PartPose.ZERO);
        root.addOrReplaceChild("palm",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.6F, 3.0F, -5.0F, 5.2F, 2.2F, 5.0F), PartPose.ZERO);
        root.addOrReplaceChild("back_plate",
                CubeListBuilder.create().texOffs(16, 16).addBox(-2.6F, 5.2F, -4.5F, 5.2F, 1.0F, 4.5F), PartPose.ZERO);
        root.addOrReplaceChild("knuckle_bar",
                CubeListBuilder.create().texOffs(32, 16).addBox(-2.6F, 5.2F, -5.6F, 5.2F, 1.4F, 1.2F), PartPose.ZERO);

        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("ridge_" + i,
                    CubeListBuilder.create().texOffs(48, 16).addBox(-2.0F + i * 2.0F, 6.2F, -4.2F, 0.5F, 0.4F, 3.6F),
                    PartPose.ZERO);
        }

        float[] fingerX = {-1.95F, -0.65F, 0.65F, 1.95F};
        for (int i = 0; i < fingerX.length; i++) {
            float x = fingerX[i];
            root.addOrReplaceChild("finger_" + i + "_prox",
                    CubeListBuilder.create().texOffs(0, 32 + i * 4).addBox(x, 3.2F, -8.0F, 1.1F, 1.6F, 3.0F), PartPose.ZERO);
            root.addOrReplaceChild("finger_" + i + "_mid",
                    CubeListBuilder.create().texOffs(16, 32 + i * 4).addBox(x, 3.2F, -10.5F, 1.1F, 1.4F, 2.5F), PartPose.ZERO);
            root.addOrReplaceChild("finger_" + i + "_dist",
                    CubeListBuilder.create().texOffs(32, 32 + i * 4).addBox(x, 3.3F, -12.5F, 1.1F, 1.2F, 2.0F), PartPose.ZERO);
            root.addOrReplaceChild("knuckle_" + i,
                    CubeListBuilder.create().texOffs(48, 32 + i * 4).addBox(x - 0.1F, 4.8F, -7.2F, 1.3F, 0.8F, 1.4F), PartPose.ZERO);
        }

        PartDefinition thumb = root.addOrReplaceChild("thumb",
                CubeListBuilder.create().texOffs(0, 48).addBox(-0.6F, -0.7F, -3.0F, 1.2F, 1.5F, 3.0F),
                PartPose.offsetAndRotation(-3.4F, 3.6F, -2.0F, 0.0F, -0.55F, 0.35F));
        thumb.addOrReplaceChild("thumb_tip",
                CubeListBuilder.create().texOffs(16, 48).addBox(-0.55F, -0.6F, -2.2F, 1.1F, 1.3F, 2.2F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.15F, 0.0F, 0.0F));

        // Гнёзда камней. Части рисуются отдельно с собственными цветными текстурами.
        root.addOrReplaceChild("stone_space",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.4F, 0.0F, -0.4F, 0.8F, 0.5F, 0.8F),
                PartPose.offset(-1.4F, 5.6F, -6.5F));
        root.addOrReplaceChild("stone_mind",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.7F, 0.0F, -0.7F, 1.4F, 0.7F, 1.4F),
                PartPose.offset(0.0F, 6.6F, -2.2F));
        root.addOrReplaceChild("stone_reality",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.4F, 0.0F, -0.4F, 0.8F, 0.5F, 0.8F),
                PartPose.offset(-0.1F, 5.6F, -6.5F));
        root.addOrReplaceChild("stone_power",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.4F, 0.0F, -0.4F, 0.8F, 0.5F, 0.8F),
                PartPose.offset(1.2F, 5.6F, -6.5F));
        root.addOrReplaceChild("stone_time",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.4F, 0.0F, -0.4F, 0.8F, 0.5F, 0.8F),
                PartPose.offset(2.5F, 5.6F, -6.5F));
        root.addOrReplaceChild("stone_soul",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.4F, 0.0F, -0.4F, 0.8F, 0.5F, 0.8F),
                PartPose.offset(-3.8F, 5.0F, -3.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
