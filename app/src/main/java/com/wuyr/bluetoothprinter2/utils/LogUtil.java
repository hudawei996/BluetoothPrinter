package com.wuyr.bluetoothprinter2.utils;

import android.util.Log;

/**
 * Created by wuyr on 6/8/16 6:54 PM.
 */
public class LogUtil {

    private static boolean isDebugOn = false;
    // 1 for verbose, 2 for debug, 3 for info, 4 for warn, 5 for error
    private static int debugLevel = 2;

    public static void print(Object s) {
        if (isDebugOn)
            if (s != null) {
                StackTraceElement element = Thread.currentThread().getStackTrace()[3];
                switch (debugLevel) {
                    case 1:
                        Log.v(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.valueOf(s));
                        break;
                    case 2:
                        Log.d(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.valueOf(s));
                        break;
                    case 3:
                        Log.i(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.valueOf(s));
                        break;
                    case 4:
                        Log.w(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.valueOf(s));
                        break;
                    case 5:
                        Log.e(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.valueOf(s));
                        break;
                    default:
                        break;
                }
            }
    }

    public static void printf(String format, Object... args) {
        if (isDebugOn)
            if (format != null && args != null) {
                StackTraceElement element = Thread.currentThread().getStackTrace()[3];
                switch (debugLevel) {
                    case 1:
                        Log.v(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.format(format, args));
                        break;
                    case 2:
                        Log.d(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.format(format, args));
                        break;
                    case 3:
                        Log.i(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.format(format, args));
                        break;
                    case 4:
                        Log.w(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.format(format, args));
                        break;
                    case 5:
                        Log.e(String.format("%s-->%s", element.getClassName(), element.getMethodName()), String.format(format, args));
                        break;
                    default:
                        break;
                }
            }
    }

    public static void setDebugOn(boolean isDebugOn) {
        LogUtil.isDebugOn = isDebugOn;
    }

    public static void setDebugLevel(int l) {
        if (debugLevel > 0 && l < 6)
            debugLevel = l;
    }
}