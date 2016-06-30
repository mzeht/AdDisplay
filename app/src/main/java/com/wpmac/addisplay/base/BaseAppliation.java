package com.wpmac.addisplay.base;

import android.app.Application;

import com.wpmac.addisplay.utils.FileUtils;

import org.xutils.x;

/**
 * Created by wpmac on 16/6/8.
 */
public class BaseAppliation extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FileUtils.initialize();
        x.Ext.init(this);
        x.Ext.setDebug(true);
    }
}
