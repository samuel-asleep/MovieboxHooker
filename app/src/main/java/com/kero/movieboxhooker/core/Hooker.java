package com.kero.movieboxhooker.core;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import com.kero.movieboxhooker.*;
import com.kero.movieboxhooker.hooks.*;

/**
 * Main entry point.
 *
 * MovieBox is packed with ByteDance's PGL (libpglarmor). The stub Application class
 * (jv.gp.x6i.Chjut1vLe) calls System.loadLibrary("pglarmor") in attachBaseContext,
 * then libpglarmor decrypts the real multi-DEX payload and replaces the ClassLoader.
 * The real classes are only available AFTER this decryption chain completes.
 *
 * Strategy:
 *   1. From handleLoadPackage, hook the stub Application's onCreate (not attachBaseContext,
 *      which fires before the real classes exist).
 *   2. Inside that hook, start a retry loop on a background thread polling for the
 *      real membership classes.
 *   3. Never use Looper.getMainLooper() at module load time — it may not exist yet.
 */
public class Hooker implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static volatile boolean hooksApplied = false;
    private static volatile long lastInstallTime  = 0;

    @Override
    public void initZygote(StartupParam startupParam) {
        Logger.info("🚀 Module initialized in Zygote");
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        Set<String> targets = Constants.getTargetPackages();
        if (!targets.contains(lpparam.packageName)) return;

        Logger.success("🎯 Target loaded: " + lpparam.packageName);
        hooksApplied = false;
        checkAppInstallTime(lpparam);

        // Hook the PGL stub Application.onCreate so we fire AFTER libpglarmor
        // has decrypted and loaded the real DEX into the ClassLoader.
        hookStubApplicationOnCreate(lpparam);
    }

    // ── Stub Application hook ────────────────────────────────────────────────

    private void hookStubApplicationOnCreate(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // The stub class extends Application, so we can hook Application.onCreate
            // directly — it will fire for both the stub and any real delegation.
            // We hook the stub by name; if not found, fall back to android.app.Application.
            Class<?> stubClass = XposedHelpers.findClassIfExists(
                    Constants.STUB_APPLICATION_CLASS, lpparam.classLoader);

            Class<?> targetClass = stubClass != null
                    ? stubClass
                    : android.app.Application.class;

            Logger.info("🔗 Hooking onCreate on: " + targetClass.getName());

            XposedHelpers.findAndHookMethod(targetClass, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Logger.info("📱 Application.onCreate fired — starting hook attempt");
                    // Run on background thread: Looper is now ready but we don't
                    // need it; background thread is safer against ANR.
                    startRetryThread(lpparam.classLoader, 0);
                }
            });

            Logger.success("✅ Stub Application.onCreate hooked");

        } catch (Throwable t) {
            Logger.error("Failed to hook stub Application: " + t.getMessage());
            // Last-resort fallback: start a blind retry thread immediately
            startRetryThread(lpparam.classLoader, 0);
        }
    }

    // ── Retry loop ───────────────────────────────────────────────────────────

    private void startRetryThread(final ClassLoader cl, final int attempt) {
        new Thread(() -> {
            if (attempt > 0) {
                HookUtils.safeSleep(Constants.RETRY_DELAY_MS);
            }
            attemptHooks(cl, attempt);
        }, "rea-hook-attempt-" + attempt).start();
    }

    private void attemptHooks(final ClassLoader cl, final int attempt) {
        if (hooksApplied) return;

        try {
            Logger.info("🔄 Hook attempt " + (attempt + 1) + "/" + Constants.MAX_RETRIES);

            // Gate: wait until at least one key class is visible
            if (!checkKeyClasses(cl)) {
                throw new ClassNotFoundException("Key classes not yet in ClassLoader");
            }

            Logger.success("📚 Classes available — applying hooks");
            applyAllHooks(cl);

        } catch (Throwable e) {
            Logger.warn("⚠️ Attempt " + (attempt + 1) + " failed: " + e.getMessage());
            if (attempt < Constants.MAX_RETRIES - 1) {
                startRetryThread(cl, attempt + 1);
            } else {
                Logger.error("❌ Max retries reached — emergency fallback");
                EmergencyHooker.apply(cl, Constants.EMERGENCY_HOOK_METHOD_LIMIT);
                hooksApplied = true;
            }
        }
    }

    private void applyAllHooks(ClassLoader cl) {
        int ok    = 0;
        int total = 7;

        ok += MemberInfoHook.apply(cl)         ? 1 : 0;
        ok += MemberBriefInfoHook.apply(cl)    ? 1 : 0;
        ok += MemberProviderHook.apply(cl)     ? 1 : 0;
        ok += MemberCheckResultHook.apply(cl)  ? 1 : 0;
        ok += UserInfoHook.apply(cl)           ? 1 : 0;
        ok += DownloadsHook.apply(cl)          ? 1 : 0;
        ok += SkipAdHook.apply(cl)             ? 1 : 0;

        if (ok == total) {
            Logger.success("🎉 ALL " + ok + "/" + total + " hooks applied");
            hooksApplied = true;
        } else if (ok >= 4) {
            Logger.success("✅ Partial success: " + ok + "/" + total + " hooks applied");
            hooksApplied = true;
        } else {
            throw new RuntimeException("Only " + ok + "/" + total + " hooks succeeded — retrying");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean checkKeyClasses(ClassLoader cl) {
        return HookUtils.findClassIfExists(Constants.CLS_MEMBER_INFO, cl) != null
            || HookUtils.findClassIfExists(Constants.CLS_MEMBER_BRIEF_INFO, cl) != null
            || HookUtils.findClassIfExists(Constants.CLS_MMKV, cl) != null;
    }

    private void checkAppInstallTime(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Context ctx = AndroidAppHelper.currentApplication();
            if (ctx == null) return;
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(lpparam.packageName, 0);
            long newest = Math.max(pi.firstInstallTime, pi.lastUpdateTime);
            if (System.currentTimeMillis() - newest < 5 * 60 * 1000) {
                Logger.warn("📱 Fresh install detected");
                lastInstallTime = newest;
            }
        } catch (Throwable ignored) {}
    }
}
