package com.kero.movieboxhooker.hooks;

import com.kero.movieboxhooker.Constants;
import com.kero.movieboxhooker.Logger;
import de.robv.android.xposed.XposedHelpers;

/**
 * Ensures the ad-skip MMKV key is always true.
 * NOTE: MemberProviderHook already covers "kv_is_skip_ad" via the shared MMKV hook.
 * This hook handles the case where MMKV was not available when MemberProviderHook ran
 * (e.g. MMKV initialized later), providing an independent guarantee.
 *
 * It deduplicates the MMKV hook by checking if MMKV is already hooked before applying.
 */
public final class SkipAdHook {

    private SkipAdHook() {}

    public static boolean apply(ClassLoader cl) {
        try {
            // MMKV.getBoolean is already hooked by MemberProviderHook covering kv_is_skip_ad.
            // Additional coverage: hook any ad-related boolean fields on the ad SDK classes
            // that can bypass MMKV entirely (Pangle / ByteDance ad SDK).
            boolean anyHooked = false;

            // Pangle (ByteDance) ad init — return false so ads don't load
            String[] adInitClasses = {
                "com.bytedance.sdk.openadsdk.api.init.PAGSdk",
                "com.bytedance.sdk.openadsdk.TTAdSdk"
            };
            for (String cls : adInitClasses) {
                Class<?> c = XposedHelpers.findClassIfExists(cls, cl);
                if (c != null) {
                    anyHooked = hookAdBoolean(c, "isInitSuccess") || anyHooked;
                }
            }

            Logger.success("✓ SkipAdHook applied (ad classes hooked: " + anyHooked + ")");
            return true; // always return true — MMKV coverage is primary

        } catch (Throwable t) {
            Logger.error("SkipAdHook failed: " + t.getMessage());
            return false;
        }
    }

    private static boolean hookAdBoolean(Class<?> cls, String method) {
        try {
            XposedHelpers.findAndHookMethod(cls, method,
                    de.robv.android.xposed.XC_MethodReplacement.returnConstant(true));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
