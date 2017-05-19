package com.ohayou.japanese.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    public static String md5Encode(String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] result = messageDigest.digest(password.getBytes());
            StringBuffer sb = new StringBuffer();
            for (byte b : result) {
                int num = b & 0xff;
                sb.append(String.format("%02x", num));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                .matches();
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

    public static boolean unZip(File file, String path) {
        ensureDir(path);
        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration iter = zipFile.entries();
            byte[] buf = new byte[1024];
            while(iter.hasMoreElements()) {
                ZipEntry ze = (ZipEntry)iter.nextElement();
                String dstPath = path + "/" + ze.getName();
                if (ze.isDirectory()) {
                    File dir = new File(dstPath);
                    dir.mkdirs();
                } else {
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(dstPath));
                    InputStream is = new BufferedInputStream(zipFile.getInputStream(ze));
                    int readLen;
                    while ((readLen = is.read(buf, 0, buf.length)) != -1) {
                        os.write(buf, 0, readLen);
                    }
                    is.close();
                    os.close();
                }
            }
            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void clearDownloads(long diffTime) {
        String directoryPath = Constants.DOWNLOAD_CACHE_PATH;
        File folder = new File(directoryPath);
        if (!folder.exists()) {
            return;
        }
        if (folder.isFile()) {
            folder.delete();
            return;
        }
        deleteFiles(folder);
    }

    public static void deleteFiles(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                file.delete();
            } else {
                deleteFiles(file);
                file.delete();
            }
        }
    }

    public static String getDownloadSize() {
        File folder = new File(Constants.DOWNLOAD_CACHE_PATH);
        long size = getFolderSize(folder);
        return formatSize(size);
    }

    public static long getFolderSize(File dir) {
        long size = 0;
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                size += file.length();
            }
            else {
                size += getFolderSize(file);
            }
        }
        return size;
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
