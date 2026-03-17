package com.shiyunjin.oplus.fuckfingerlight;


import android.content.Context;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class FingerprintModule extends XposedModule {

    public FingerprintModule(XposedInterface base, XposedModuleInterface.ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        super.onPackageLoaded(param);

        // 目标在 SystemUI 进程
        if (!param.getPackageName().equals("com.android.systemui")) {
            return;
        }

        try {
            // 1. 加载目标类
            Class<?> controlClass = param.getClassLoader().loadClass("com.oplus.systemui.biometrics.finger.udfps.OnScreenHighLightControl");

            Method irisMethod = controlClass.getDeclaredMethod(
                    "updateDisplayIRISAndColorMode",
                    Context.class,
                    boolean.class,
                    String.class
            );

            hook(irisMethod, IrisColorHooker.class);
            log("成功防止指纹动我的亮度");

        } catch (ClassNotFoundException e) {

        } catch (NoSuchMethodException e) {

        } catch (Exception e) {
            log("Hook 异常: " + e.getMessage());
        }
    }

    public static class IrisColorHooker implements XposedInterface.Hooker {
        public static void before(XposedInterface.BeforeHookCallback callback) {
            callback.returnAndSkip(null);
        }
    }
}