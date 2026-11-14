package com.insane.infinitygauntlet.client;

/**
 * Клиентское состояние эффектов: вспышка на весь экран (цвет + длительность)
 * и тряска камеры.
 */
public final class ClientEffects {
    private static int flashColor = 0xFFFFFFFF;
    private static long flashUntilMs = 0L;
    private static long shakeUntilMs = 0L;

    private ClientEffects() {
    }

    /** Запустить вспышку заданного цвета на указанное время (мс). */
    public static void flash(int color, int millis) {
        flashColor = color;
        flashUntilMs = System.currentTimeMillis() + millis;
    }

    /** Запустить тряску камеры на указанное время (мс). */
    public static void shake(int millis) {
        shakeUntilMs = System.currentTimeMillis() + millis;
    }

    public static boolean isFlashing() {
        return System.currentTimeMillis() < flashUntilMs;
    }

    /** Цвет вспышки в формате ARGB. */
    public static int getFlashColor() {
        return flashColor;
    }

    public static boolean isShaking() {
        return System.currentTimeMillis() < shakeUntilMs;
    }
}
