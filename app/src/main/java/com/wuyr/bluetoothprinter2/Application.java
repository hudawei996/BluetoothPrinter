package com.wuyr.bluetoothprinter2;


import android.content.Context;

import com.wuyr.bluetoothprinter2.utils.LogUtil;

/**
 * Created by wuyr on 17-7-15 下午12:12.
 */

public class Application extends android.app.Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.setDebugOn(true);
        LogUtil.setDebugLevel(5);
        mContext = this;
    }

    public static Context getContext() {
        return mContext;
    }
}
