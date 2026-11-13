package com.insane.infinitygauntlet.network;

import com.insane.infinitygauntlet.InfinityGauntletMod;
import com.insane.infinitygauntlet.client.ClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.Payload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Сервер -> клиент: вспышка на весь экран (цвет + длительность) и тряска камеры.
 * Используется щелчком (белая вспышка 1с) и воскрешением (оранжевый фильтр 2с).
 */
public record ClientFlashPayload(int color, int durationMs, int shakeMs) implements Payload {
    public static final StreamCodec<FriendlyByteBuf, ClientFlashPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ClientFlashPayload::color,
                    ByteBufCodecs.VAR_INT, ClientFlashPayload::durationMs,
                    ByteBufCodecs.VAR_INT, ClientFlashPayload::shakeMs,
                    ClientFlashPayload::new);
    public static final Type<ClientFlashPayload> TYPE = new Type<>(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(InfinityGauntletMod.MOD_ID, "client_flash"));

    @Override
    public Type<? extends Payload> type() {
        return TYPE;
    }

    public static void handle(ClientFlashPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (payload.durationMs() > 0) {
                ClientEffects.flash(payload.color(), payload.durationMs());
            }
            if (payload.shakeMs() > 0) {
                ClientEffects.shake(payload.shakeMs());
            }
        });
    }
}
