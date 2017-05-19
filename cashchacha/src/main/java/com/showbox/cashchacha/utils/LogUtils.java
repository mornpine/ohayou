package com.showbox.cashchacha.utils;

import android.util.Log;

/**
 * Created by Oxygen on 15/7/29.
 */
public class LogUtils {
    public static final boolean DEBUG = true;

    public static void info(String msg) {
        info("Oxygen", msg);
    }

    public static void info(String tag, String msg) {
        if (DEBUG) {
            // Log.i(tag, msg + CommUtils.getCaller());
        }
    }

    public static void printVars(Object... args) {
        StringBuilder builder = new StringBuilder();
        builder.append("----->");
        for (Object arg : args) {
            builder.append(arg.getClass().getSimpleName());
            builder.append('(');
            builder.append(arg.toString());
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
