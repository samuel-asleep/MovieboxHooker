package com.kero.movieboxhooker;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.kero.movieboxhooker.core.Hooker;

/**
 * Xposed entry point — registered in assets/xposed_init.
 * Delegates to core.Hooker which handles PGL timing correctly.
 */
public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        new Hooker().handleLoadPackage(lpparam);
    }
}
