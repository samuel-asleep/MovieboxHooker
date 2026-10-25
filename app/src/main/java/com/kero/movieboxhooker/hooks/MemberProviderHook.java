package com.kero.movieboxhooker.hooks;

import com.kero.movieboxhooker.Constants;
import com.kero.movieboxhooker.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hooks MMKV.getBoolean(String key, boolean defaultValue) so that VIP/member/ad keys
 * always return true. This is resilient to code obfuscation because it targets the
 * underlying storage layer rather than obfuscated wrapper methods.
 */
public final class MemberProviderHook {

    private MemberProviderHook() {}

    public static boolean apply(ClassLoader cl) {
        try {
            Class<?> mmkvClass = XposedHelpers.findClassIfExists(Constants.CLS_MMKV, cl);
            if (mmkvClass == null) {
                Logger.warn("MemberProviderHook: MMKV class not found");
                return false;
            }

            final Set<String> forceTrueKeys = new HashSet<>(
                    Arrays.asList(Constants.MMKV_FORCE_TRUE_KEYS));

            XposedHelpers.findAndHookMethod(
                    mmkvClass,
                    "getBoolean",
                    String.class,
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String key = (String) param.args[0];
                            if (key != null && forceTrueKeys.contains(key)) {
                                Logger.success("MMKV force TRUE → " + key);
                                param.setResult(true);
                            }
                        }
                    });

            Logger.success("✓ MemberProviderHook applied via MMKV.getBoolean() "
                    + "(" + forceTrueKeys.size() + " keys)");
            return true;

        } catch (Throwable t) {
            Logger.error("MemberProviderHook failed: " + t.getMessage());
            return false;
        }
    }
}
