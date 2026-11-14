package com.insane.infinitygauntlet.network;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import com.insane.infinitygauntlet.ability.AbilityManager;
import com.insane.infinitygauntlet.ability.SnapHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Регистрация пакетов управления и клиентских визуальных эффектов. */
public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CycleStonePayload.TYPE, CycleStonePayload.STREAM_CODEC, CycleStonePayload::handle);
        registrar.playToServer(CycleAbilityPayload.TYPE, CycleAbilityPayload.STREAM_CODEC, CycleAbilityPayload::handle);
        registrar.playToServer(ActivateAbilityPayload.TYPE, ActivateAbilityPayload.STREAM_CODEC, ActivateAbilityPayload::handle);
        registrar.playToServer(SnapPayload.TYPE, SnapPayload.STREAM_CODEC, SnapPayload::handle);
        registrar.playToClient(ClientFlashPayload.TYPE, ClientFlashPayload.STREAM_CODEC, ClientFlashPayload::handle);
    }

    public record CycleStonePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CycleStonePayload> TYPE = createType("cycle_stone");
        public static final StreamCodec<RegistryFriendlyByteBuf, CycleStonePayload> STREAM_CODEC =
                StreamCodec.unit(new CycleStonePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static void handle(CycleStonePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> AbilityManager.cycleStone((ServerPlayer) context.player()));
        }
    }

    public record CycleAbilityPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CycleAbilityPayload> TYPE = createType("cycle_ability");
        public static final StreamCodec<RegistryFriendlyByteBuf, CycleAbilityPayload> STREAM_CODEC =
                StreamCodec.unit(new CycleAbilityPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static void handle(CycleAbilityPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> AbilityManager.cycleAbility((ServerPlayer) context.player()));
        }
    }

    public record ActivateAbilityPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ActivateAbilityPayload> TYPE = createType("activate_ability");
        public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> STREAM_CODEC =
                StreamCodec.unit(new ActivateAbilityPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static void handle(ActivateAbilityPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> AbilityManager.activateAbility((ServerPlayer) context.player()));
        }
    }

    public record SnapPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SnapPayload> TYPE = createType("snap");
        public static final StreamCodec<RegistryFriendlyByteBuf, SnapPayload> STREAM_CODEC =
                StreamCodec.unit(new SnapPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static void handle(SnapPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> SnapHandler.trySnap((ServerPlayer) context.player()));
        }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(InfinityGauntletMod.MOD_ID, path));
    }
}
