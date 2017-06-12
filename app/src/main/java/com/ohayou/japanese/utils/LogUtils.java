package com.ohayou.japanese.utils;

import android.util.Log;

import com.ohayou.japanese.BuildConfig;

/**
 * Created by Oxygen on 15/7/29.
 */
public class LogUtils {
    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static void info(String msg) {
        info("Oxygen", msg);
    }

    public static void info(String tag, String msg) {
        if (DEBUG) {
            Log.i(tag, msg + CommUtils.getCaller());
        }
    }

    public static void printVars(Object... args) {
        StringBuilder builder = new StringBuilder();
        builder.append("----->");
        for (int i = 0; i < args.length; i++) {
            builder.append(args[i].getClass().getSimpleName());
            builder.append('(');
            builder.append(args[i].toString());
            builder.append(") ");
        }
        info(builder.toString());
    }

    public static void error(String msg) {
        error("Oxygen", msg);
    }

    public static void error(String tag, String msg) {
        // Log.e(tag, msg + CommUtils.getCaller());
    }


}
