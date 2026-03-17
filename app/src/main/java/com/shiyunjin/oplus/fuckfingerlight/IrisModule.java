package com.shiyunjin.oplus.fuckfingerlight;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

@SuppressLint("PrivateApi")
public class IrisModule extends XposedModule {

    private static final String TAG = "IrisModule";
    private Class<?> irisHelperClass;

    public IrisModule(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onSystemServerLoaded(@NonNull SystemServerLoadedParam param) {
        log(TAG + ": System Server Loaded");

        ClassLoader classLoader = param.getClassLoader();

        try {
            irisHelperClass = classLoader.loadClass("com.android.server.display.util.OplusIrisConfigHelper");

            // 2. Hook App 切换监听 (ActivityRecord.setState)
            Class<?> activityRecordClass = classLoader.loadClass("com.android.server.wm.ActivityRecord");
            Class<?> stateEnumClass = classLoader.loadClass("com.android.server.wm.ActivityRecord$State");
            Method setStateMethod = activityRecordClass.getDeclaredMethod("setState", stateEnumClass, String.class);
            hook(setStateMethod, ActivitySwitchHooker.class);

            // 3. 寻找时机在 system_server 中注册 BroadcastReceiver
            // 我们 Hook ActivityManagerService 的 systemReady 方法，这是注册接收器的最佳且最安全的时机
            Class<?> amsClass = classLoader.loadClass("com.android.server.am.ActivityManagerService");
            for (Method m : amsClass.getDeclaredMethods()) {
                if (m.getName().equals("systemReady")) {
                    hook(m, SystemReadyHooker.class);
                    break;
                }
            }
        } catch (Throwable t) {
            log(TAG + ": Initialization Error - " + t.getMessage());
        }
    }

    public static void sendIrisCmd(ClassLoader classLoader, String value) {
        try {
            Class<?> irisClass = classLoader.loadClass("com.android.server.display.util.OplusIrisConfigHelper");
            Method setCommand = irisClass.getDeclaredMethod("setIrisCommand", String.class);
            setCommand.invoke(null, value);
        } catch (Throwable ignored) {
            Log.e(TAG, "sendIrisCmd Error - " + ignored.getMessage());
        }
    }

    public static void sendColorMode(ClassLoader classLoader) {
        sendIrisCmd(classLoader,"515-0");
    }

    public static class SystemReadyHooker implements XposedInterface.Hooker {
        public static void before(@NonNull XposedInterface.BeforeHookCallback callback) {
            try {
                Object ams = callback.getThisObject();
                Field contextField = ams.getClass().getDeclaredField("mContext");
                contextField.setAccessible(true);
                Context systemContext = (Context) contextField.get(ams);

                // 注册系统级广播接收器来接收 QuickTile 的指令
                IntentFilter filter = new IntentFilter(TileService.ACTION_TOGGLE_LIGHT);
                systemContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        try {
                            sendColorMode(systemContext.getClassLoader());
                        } catch (Throwable t) {
                            // 忽略反射异常
                        }
                    }
                }, filter, Context.RECEIVER_EXPORTED);
            } catch (Throwable ignored) {
            }
        }
    }

    public static class ActivitySwitchHooker implements XposedInterface.Hooker {
        private static String mLastPackageName = "";

        public static void after(@NonNull XposedInterface.AfterHookCallback callback) {
            try {
                Object stateEnum = callback.getArgs()[0];
                if (stateEnum != null && "RESUMED".equals(stateEnum.toString())) {
                    Object activityRecord = callback.getThisObject();
                    Field pkgField = activityRecord.getClass().getDeclaredField("packageName");
                    pkgField.setAccessible(true);
                    String currentPackage = (String) pkgField.get(activityRecord);

                    if (currentPackage != null && !currentPackage.equals(mLastPackageName)) {
                        mLastPackageName = currentPackage;

                        sendColorMode(activityRecord.getClass().getClassLoader());
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}