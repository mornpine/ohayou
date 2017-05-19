package com.ohayou.japanese.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.SparseArray;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.ohayou.japanese.R;
import com.ohayou.japanese.model.SettingsStore;
import com.ohayou.japanese.model.UserInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

    public static SparseArray<String> sErrorCodes = new SparseArray<>();

    public static final DefaultRetryPolicy gRetryPolicy = new DefaultRetryPolicy(60 * 1000, 2, 1.0f);

    public static final int RET_CODE_USER_NOT_EXIST = 408;
    public static final int RET_CODE_SUCCESS = 200;
    public static final int RET_CODE_NO_UPDATE = 210;

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

    public static String getErrorString(int code) {
        String str = sErrorCodes.get(code);
        if (str == null) {
            return "Unknown Error!";
        }
        return str;
    }

    public static void showError(Context context, int status) {
        Toast.makeText(context, getErrorString(status), Toast.LENGTH_LONG).show();
    }

    public static void init(Context context) {
        sQueue = Volley.newRequestQueue(context);

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

        doJsonObjectRequest(Constants.URL_GET_ERROR_CODES, null, errorCodesListener);
        doJsonObjectRequest(Constants.URL_GET_SETTINGS_STORE, null, settingsStoreListener);

        new GcmRegisterTask().execute(context);
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


    static NetworkUtils.JsonObjectResultListener errorCodesListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            try {
                if (json != null) {
                    JSONArray data = json.getJSONArray("data");
                    for (int i = 0; i < data.length(); ++i) {
                        JSONObject obj = data.getJSONObject(i);
                        int code = Integer.parseInt(obj.getString("code"));
                        sErrorCodes.append(code, obj.getString("text"));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    static JsonObjectResultListener settingsStoreListener = new JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            try {
                if (json != null) {
                    JSONArray data = json.getJSONArray("data");
                    JSONObject obj = data.getJSONObject(0);
                    SettingsStore.points_initial_points = Integer.parseInt(obj.getString("points_initial_points"));
                    SettingsStore.points_complete_textbook = Integer.parseInt(obj.getString("points_complete_textbook"));
                    SettingsStore.points_view_script_textbook = Integer.parseInt(obj.getString("points_view_script_textbook"));
                    SettingsStore.points_view_script_1 = Integer.parseInt(obj.getString("points_view_script_1"));
                    SettingsStore.points_view_scripts = Integer.parseInt(obj.getString("points_view_scripts"));
                    SettingsStore.points_remove_ads = Integer.parseInt(obj.getString("points_remove_ads"));
                    String[] tmp = obj.getString("points_exchange_1").split(":");
                    SettingsStore.points_exchange_1 = Integer.parseInt(tmp[0]);
                    SettingsStore.points_exchange_1_rate = Float.parseFloat(tmp[1]);
                    tmp = obj.getString("points_exchange_2").split(":");
                    SettingsStore.points_exchange_2 = Integer.parseInt(tmp[0]);
                    SettingsStore.points_exchange_2_rate = Float.parseFloat(tmp[1]);
                    tmp = obj.getString("points_exchange_3").split(":");
                    SettingsStore.points_exchange_3 = Integer.parseInt(tmp[0]);
                    SettingsStore.points_exchange_3_rate = Float.parseFloat(tmp[1]);
                    SettingsStore.init = true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    static class GcmRegisterTask extends AsyncTask<Context, Void, String> {
        @Override
        protected String doInBackground(Context... params) {
            InstanceID instanceID = InstanceID.getInstance(params[0]);
            String token = null;
            try {
                token = instanceID.getToken(params[0].getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return token;
        }

        @Override
        protected void onPostExecute(String s) {
            UserInfo.sGcmToken = s;
            if (UserInfo.sEmail != null && s != null) {
                doJsonObjectRequest(Constants.URL_UPDATE_GCM, genParamList("email", UserInfo.sEmail, "gcm", s), null);
            }
        }
    }
}
