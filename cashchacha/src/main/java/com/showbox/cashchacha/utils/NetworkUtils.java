package com.showbox.cashchacha.utils;

import android.content.Context;
import android.provider.Settings;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Oxygen on 15/8/5.
 */
public class NetworkUtils {
    static RequestQueue sQueue;
    static String sBaseParams;

    public static final DefaultRetryPolicy gRetryPolicy = new DefaultRetryPolicy(60 * 1000, 2, 1.0f);

    public interface JsonObjectResultListener {
        void onResult(JSONObject json);
    }

    public static class KeyValue {
        public String key;
        public String value;
        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static void init(Context context) {
        if (sQueue == null) {
            sQueue = Volley.newRequestQueue(context.getApplicationContext());

            StringBuilder params = new StringBuilder();
            params.append("?version=");
            params.append(CommUtils.getVersionCode(context));
            params.append("&package=");
            params.append(context.getPackageName());
            params.append("&deviceId=");
            params.append(Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID));
            params.append("&locale=");
            params.append(Locale.getDefault());

            sBaseParams = params.toString();
        }
    }

    public static List<KeyValue> genParamList(String... args) {
        if (args.length % 2 != 0) {
            throw new RuntimeException("Args must be pairs");
        }
        ArrayList<KeyValue> params = new ArrayList<>();
        for (int i = 0; i < args.length; i += 2) {
            params.add(new KeyValue(args[i], args[i + 1]));
        }

        return params;
    }

    public static void doJsonObjectRequest(String url, List<KeyValue> params, final JsonObjectResultListener listener) {
        StringBuilder builder = new StringBuilder();
        builder.append(url);
        builder.append(sBaseParams);
        if (params != null) {
            for (KeyValue keyValue : params) {
                builder.append('&');
                builder.append(keyValue.key);
                builder.append('=');
                builder.append(URLEncoder.encode(keyValue.value));
            }
        }
        String reqUrl = builder.toString();
        LogUtils.info("-----> url = " + reqUrl);
        JsonObjectRequest request = new JsonObjectRequest(reqUrl, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (listener != null) {
                    listener.onResult(response);
                }
                LogUtils.info("----> response = " + response );
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LogUtils.error("----------> error = " + error.getMessage());
                if (listener != null) {
                    listener.onResult(null);
                }
            }
        });

        request.setRetryPolicy(gRetryPolicy);
        sQueue.add(request);
    }
}
