package com.kero.movieboxhooker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Constants {

    private Constants() {}

    // How long to wait after package load before first hook attempt.
    // PGL (libpglarmor) decrypts the real DEX during Application.attachBaseContext,
    // which fires before Application.onCreate. We hook Application.onCreate of the
    // stub class (jv.gp.x6i.Chjut1vLe) so the first attempt fires immediately after
    // the real classes are in the ClassLoader.
    public static final int INITIAL_DELAY_MS  = 0;
    public static final int RETRY_DELAY_MS    = 800;
    public static final int MAX_RETRIES       = 25;

    // Emergency fallback: max boolean methods hooked per class
    public static final int EMERGENCY_HOOK_METHOD_LIMIT = 20;

    // Fresh-install aggressive window
    public static final long FRESH_INSTALL_WINDOW_MS = 10L * 60L * 1000L;

    // The PGL stub Application class — we hook its onCreate to fire after decryption
    public static final String STUB_APPLICATION_CLASS = "jv.gp.x6i.Chjut1vLe";

    // Both known package names for MovieBox
    private static final Set<String> TARGET_PACKAGES;
    static {
        HashSet<String> t = new HashSet<>();
        t.add("com.community.oneroom");
        t.add("com.community.mbox.in");
        TARGET_PACKAGES = Collections.unmodifiableSet(t);
    }

    public static Set<String> getTargetPackages() {
        return TARGET_PACKAGES;
    }

    // ── Key class names ──────────────────────────────────────────────────────
    public static final String CLS_MEMBER_INFO         = "com.transsion.memberapi.MemberInfo";
    public static final String CLS_MEMBER_BRIEF_INFO   = "com.transsion.member.bean.MemberBriefInfo";
    public static final String CLS_MEMBER_PROVIDER     = "com.transsion.member.MemberProvider";
    public static final String CLS_MEMBER_CHECK_RESULT = "com.transsion.memberapi.MemberCheckResult";
    public static final String CLS_USER_INFO           = "com.transsnet.loginapi.bean.UserInfo";
    public static final String CLS_MMKV               = "com.tencent.mmkv.MMKV";

    // Download bean classes that gate downloads behind membership
    public static final String[] DOWNLOAD_CLASSES = {
        "com.transsion.baselib.db.download.DownloadBean",
        "com.transsion.moviedetailapi.DownloadItem",
        "com.transsion.moviedetailapi.bean.DownloadResolutionItem",
        "com.transsion.shorttv.bean.DownloadItem"
    };

    // MMKV keys that control VIP/ad behaviour
    public static final String[] MMKV_FORCE_TRUE_KEYS = {
        "kv_is_enable_member",
        "kv_is_pay_enable_member",
        "kv_is_skip_ad"
    };
}
