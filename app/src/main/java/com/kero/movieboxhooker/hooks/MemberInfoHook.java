package com.kero.movieboxhooker.hooks;

import com.kero.movieboxhooker.Constants;
import com.kero.movieboxhooker.Logger;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public final class MemberInfoHook {

    private MemberInfoHook() {}

    public static boolean apply(ClassLoader cl) {
        try {
            Class<?> c = XposedHelpers.findClassIfExists(Constants.CLS_MEMBER_INFO, cl);
            if (c == null) {
                Logger.warn("MemberInfo class not found");
                return false;
            }

            int hooked = 0;
            hooked += hook(c, "isActive",         true);
            hooked += hook(c, "getExpiryDate",    "2099-01-01");
            hooked += hook(c, "getDaysLeft",       9999);
            hooked += hook(c, "getMemberType",     2);
            hooked += hook(c, "getNextRenewDate",  "2099-01-01");
            hooked += hook(c, "isMember",          true);
            hooked += hook(c, "isPremium",         true);
            hooked += hook(c, "isVip",             true);

            Logger.success("✓ MemberInfoHook: " + hooked + " methods hooked");
            return hooked > 0;

        } catch (Throwable t) {
            Logger.error("MemberInfoHook failed: " + t.getMessage());
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
