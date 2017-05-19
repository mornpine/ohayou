package com.showbox.cashchacha.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.security.MessageDigest;

/**
 * Created by Oxygen on 15/7/29.
 */
public class CommUtils {
    public static int getVersionCode(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static String getVersionName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String getCaller() {
        StackTraceElement stacks[] = (new Throwable().getStackTrace());
        for (StackTraceElement ste : stacks) {
            if (!ste.getClassName().equals(LogUtils.class.getName()) && !ste.getClassName().equals(CommUtils.class.getName())) {
                return " (" + ste.getFileName() + ":" + ste.getLineNumber() + ")";
            }
        }

        return "";
    }

    public static int getPercent(int val, int total) {
        return (val + total / 2) / total;
    }

    public static void ensureDir(String path) {
        File folder = new File(path);
        if (folder.exists()) {
            if (folder.isDirectory()) {
                return;
            }
            folder.delete();
        }
        folder.mkdirs();
    }

    public static boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                .matches();
    }

    public static String formatSize(long size) {
        if (size > 1024 * 1024 * 1024) {
            return String.format("%.1fGB", size *1.0 / (1024 * 1024 * 1024));
        } else if (size > 1024 * 1024) {
            return String.format("%.1fMB", size *1.0 / (1024 * 1024));
        } else if (size > 1024){
            return String.format("%.1fKB", size *1.0 / 1024);
        } else {
            return String.format("%dB", size);
        }
    }

    public static void printSignature(Context context) {
        try {
            Signature[] sigs = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
            for (Signature sig : sigs) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(sig.toByteArray());
                String key = new String(Base64.encode(md.digest(), 0));
                Log.i("Oxygen", "-----> key = " + key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
