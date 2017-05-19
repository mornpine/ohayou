package com.ohayou.japanese.model;

import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Oxygen on 15/8/8.
 */
public class UserInfo {
    public static String sEmail;
    public static int sPoints;
    public static boolean sRemovedAds;
    public static boolean sViewScripts;
    public static String sGcmToken;
    public static LinkedList<WeakReference<UpdatePoints>> sObservers = new LinkedList<>();

    public static void parse(JSONArray array) {
        try {
            sPoints = Integer.parseInt(array.getString(2));
            sEmail = array.getString(0);
            sRemovedAds = array.getString(3).equals("1");
            sViewScripts = array.getString(4).equals("1");
            if (sGcmToken != null) {
                NetworkUtils.doJsonObjectRequest(Constants.URL_UPDATE_GCM, NetworkUtils.genParamList("email", sEmail, "gcm", sGcmToken), null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void addObserver(UpdatePoints up) {
        sObservers.add(new WeakReference<>(up));
        up.onUpdatePoints();
    }

    public static void removeObserver(UpdatePoints up) {
        Iterator<WeakReference<UpdatePoints>> iter = sObservers.iterator();
        while (iter.hasNext()) {
            WeakReference<UpdatePoints> wup = iter.next();
            UpdatePoints up2 = wup.get();
            if (up2 != null) {
                if (up2 == up) {
                    iter.remove();
                    break;
                }
            } else {
                iter.remove();
            }
        }
    }

    public static void addPoints(int points) {
        sPoints += points;
        notifyPointsChanged();
    }

    public static void notifyPointsChanged() {
        Iterator<WeakReference<UpdatePoints>> iter = sObservers.iterator();
        while (iter.hasNext()) {
            WeakReference<UpdatePoints> wup = iter.next();
            UpdatePoints up = wup.get();
            if (up != null) {
                up.onUpdatePoints();
            } else {
                iter.remove();
            }
        }
    }

    public interface UpdatePoints {
        void onUpdatePoints();
    }
}
