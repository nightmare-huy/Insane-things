package com.insane.infinitygauntlet.ability;

import com.insane.infinitygauntlet.network.ClientFlashPayload;
import com.insane.infinitygauntlet.registry.StoneType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Каноничный эффект вставки камня: цветные молнии-частицы, гул энергии,
 * тряска игрока и 2 единицы урона.
 */
public final class InsertionEffects {
    private InsertionEffects() {
    }

    public static void play(ServerLevel level, Player player, StoneType stone) {
        Vec3 center = player.getEyePosition();
        int color = stone.getColor();

        // Цветные молнии-частицы вокруг игрока
        for (int i = 0; i < 3; i++) {
            Vec3 offset = new Vec3((Math.random() - 0.5) * 2.0, (Math.random() - 0.5) * 2.0, (Math.random() - 0.5) * 2.0);
            Effects.burst(level, Effects.dust(color, 2.0F), center.add(offset), 12, 0.3, 0.2);
        }
        Effects.burst(level, ParticleTypes.ELECTRIC_SPARK, center, 30, 0.8, 0.5);
        Effects.burst(level, ParticleTypes.END_ROD, center, 16, 0.6, 0.3);

        // Настоящая молния рядом (визуальная)
        Effects.lightning(level, center.x + 1.5, center.y + 2.0, center.z);

        // Гул энергии + разряд
        Effects.hum(level, center);
        Effects.sound(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.2F);
        Effects.sound(level, center, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 0.7F);

        // Тряска камеры на клиенте
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ClientFlashPayload(0, 0, 350));
        }

        // 2 единицы урона (1 сердце) — цена силы
        player.hurt(player.damageSources().magic(), 2.0F);
    }
}
