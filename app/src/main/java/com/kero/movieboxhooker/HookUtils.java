package com.kero.movieboxhooker;

import de.robv.android.xposed.XposedHelpers;

public final class HookUtils {

    private HookUtils() {}

    public static Class<?> findClassIfExists(String name, ClassLoader cl) {
        try {
            return XposedHelpers.findClassIfExists(name, cl);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void safeSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
