package com.kero.movieboxhooker.hooks;

import com.kero.movieboxhooker.Constants;
import com.kero.movieboxhooker.Logger;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public final class DownloadsHook {

    private DownloadsHook() {}

    public static boolean apply(ClassLoader cl) {
        int found = 0;
        int hooked = 0;

        for (String className : Constants.DOWNLOAD_CLASSES) {
            try {
                Class<?> c = XposedHelpers.findClassIfExists(className, cl);
                if (c == null) continue;
                found++;

                // Remove the member-type requirement gate on downloads
                try {
                    XposedHelpers.findAndHookMethod(c, "getRequireMemberType",
                            XC_MethodReplacement.returnConstant(0));
                    hooked++;
                } catch (Throwable ignored) {}

                // Also zero out any VIP-level requirement
                try {
                    XposedHelpers.findAndHookMethod(c, "getVipLevel",
                            XC_MethodReplacement.returnConstant(0));
                } catch (Throwable ignored) {}

            } catch (Throwable ignored) {}
        }

        Logger.success("✓ DownloadsHook: " + hooked + "/" + Constants.DOWNLOAD_CLASSES.length
                + " classes hooked");
        return hooked > 0;
    }
}
