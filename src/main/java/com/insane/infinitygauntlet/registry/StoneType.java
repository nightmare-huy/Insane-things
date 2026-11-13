package com.insane.infinitygauntlet.registry;

import net.minecraft.ChatFormatting;

/**
 * Шесть камней бесконечности. Порядок важен: он используется как индекс
 * в массиве перезарядок (stone.ordinal() * 4 + abilityIndex).
 */
public enum StoneType {
    SPACE("space", 0x3B6FE0, ChatFormatting.BLUE, "Камень Пространства"),
    MIND("mind", 0xF2D13D, ChatFormatting.YELLOW, "Камень Разума"),
    REALITY("reality", 0xE03B3B, ChatFormatting.RED, "Камень Реальности"),
    POWER("power", 0x9B3BE0, ChatFormatting.LIGHT_PURPLE, "Камень Силы"),
    TIME("time", 0x3BE06F, ChatFormatting.GREEN, "Камень Времени"),
    SOUL("soul", 0xE07B2A, ChatFormatting.GOLD, "Камень Души");

    private final String id;
    private final int color;
    private final ChatFormatting formatting;
    private final String displayName;

    StoneType(String id, int color, ChatFormatting formatting, String displayName) {
        this.id = id;
        this.color = color;
        this.formatting = formatting;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    /** Цвет камня (0xRRGGBB) — используется в HUD, частицах и рендере. */
    public int getColor() {
        return color;
    }

    public ChatFormatting getFormatting() {
        return formatting;
    }

    /** Русское название камня для интерфейса. */
    public String getDisplayName() {
        return displayName;
    }

    /** Ключ перевода названия способности в lang-файле. */
    public String abilityKey(int abilityIndex) {
        return "ability.infinity_gauntlet." + id + "." + (abilityIndex + 1);
    }

    public static StoneType byId(String id) {
        for (StoneType stone : values()) {
            if (stone.id.equals(id)) {
                return stone;
            }
        }
        return null;
    }
}
