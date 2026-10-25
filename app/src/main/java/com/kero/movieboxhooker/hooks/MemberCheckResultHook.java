package com.kero.movieboxhooker.hooks;

import com.kero.movieboxhooker.Constants;
import com.kero.movieboxhooker.Logger;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public final class MemberCheckResultHook {

    private MemberCheckResultHook() {}

    public static boolean apply(ClassLoader cl) {
        try {
            Class<?> c = XposedHelpers.findClassIfExists(Constants.CLS_MEMBER_CHECK_RESULT, cl);
            if (c == null) {
                Logger.warn("MemberCheckResult class not found");
                return false;
            }

            String[] boolMethods = {
                "getVipEnable",
                "getVipPayEnable",
                "isPassed",
                "isEnabled",
                "isVipEnabled"
            };

            int hooked = 0;
            for (String m : boolMethods) {
                hooked += hook(c, m, true);
            }

            Logger.success("✓ MemberCheckResultHook: " + hooked + " methods hooked");
            return hooked > 0;

        } catch (Throwable t) {
            Logger.error("MemberCheckResultHook failed: " + t.getMessage());
            return false;
        }
    }

    private static int hook(Class<?> cls, String method, Object value) {
        try {
            XposedHelpers.findAndHookMethod(cls, method,
                    XC_MethodReplacement.returnConstant(value));
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
