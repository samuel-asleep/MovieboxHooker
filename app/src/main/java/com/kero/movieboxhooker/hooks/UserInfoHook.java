package com.kero.movieboxhooker.hooks;

import com.kero.movieboxhooker.Constants;
import com.kero.movieboxhooker.Logger;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class UserInfoHook {

    private static final String NICKNAME = "Hooked with ❤️ by Kero309x";

    private UserInfoHook() {}

    public static boolean apply(ClassLoader cl) {
        try {
            Class<?> c = XposedHelpers.findClassIfExists(Constants.CLS_USER_INFO, cl);
            if (c == null) {
                Logger.warn("UserInfo class not found");
                return false;
            }

            // Override nickname getter
            try {
                XposedHelpers.findAndHookMethod(c, "getNickname",
                        XC_MethodReplacement.returnConstant(NICKNAME));
            } catch (Throwable ignored) {}

            // Patch field via constructor so even direct field reads see the value
            XposedBridge.hookAllConstructors(c, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        XposedHelpers.setObjectField(param.thisObject, "nickname", NICKNAME);
                    } catch (Throwable ignored) {}
                }
            });

            Logger.success("✓ UserInfoHook applied");
            return true;

        } catch (Throwable t) {
            Logger.error("UserInfoHook failed: " + t.getMessage());
            return false;
        }
    }
}
