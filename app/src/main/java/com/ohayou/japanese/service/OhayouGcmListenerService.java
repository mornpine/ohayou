package com.ohayou.japanese.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.google.android.gms.gcm.GcmListenerService;
import com.ohayou.japanese.R;
import com.ohayou.japanese.model.UserInfo;
import com.showbox.cashchacha.ui.ShowboxActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Oxygen on 15/12/14.
 */
public class OhayouGcmListenerService extends GcmListenerService {

    public static boolean sOhayouMode = true;

    public static final Pattern sNumber = Pattern.compile("[-0-9]+");

    Handler mHandler;

    public OhayouGcmListenerService() {
        mHandler = new Handler();
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        super.onMessageReceived(from, data);

        String msg = data.getString("message");
        if (!TextUtils.isEmpty(msg)) {
            showNotification(msg);
            Matcher matcher = sNumber.matcher(msg);
            if (matcher.find()) {
                final int points = Integer.parseInt(matcher.group());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (sOhayouMode) {
                            UserInfo.addPoints(points);
                        } else {
                            ShowboxActivity.addPoints(points);
                        }
                    }
                });
            }
        }
    }


    public void showNotification(String content) {

        final int id = 1000;
        String title = getString(com.showbox.cashchacha.R.string.app_name);

        Intent notifyIntent = new Intent(Intent.ACTION_MAIN);
        notifyIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notifyIntent.setPackage(getPackageName());

        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notifyIntent, 0);

        final NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        @SuppressWarnings("deprecation")
        Notification notification = new Notification.Builder(this)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setContentText(content)
                .setSmallIcon(R.drawable.logo_app)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(intent)
                .getNotification();

        manager.notify(id, notification);
    }
}
