package com.showbox.cashchacha.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nativex.monetization.MonetizationManager;
import com.nativex.monetization.listeners.SessionListener;
import com.showbox.cashchacha.R;
import com.showbox.cashchacha.utils.CommUtils;
import com.showbox.cashchacha.utils.NetworkUtils;
import com.supersonic.mediationsdk.sdk.Supersonic;
import com.supersonic.mediationsdk.sdk.SupersonicFactory;
import com.tapjoy.TapjoyConnect;
import com.tapjoy.TapjoyConnectNotifier;
import com.adgatemedia.sdk.classes.AdGateMedia;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Hashtable;

/**
 * Created by Oxygen on 15/12/8.
 */
public class ShowboxActivity extends Activity implements View.OnClickListener {
    public static final String EXTRA_GCM_TOKEN = "GCM_TOKEN";

    static WeakReference<ShowboxActivity> sInstant;

    private Supersonic mMediationAgent;
    private AdGateMedia adGateMedia;
    protected ProgressDialog mProcessDlg = null;
    protected String mUserName;
    protected String mToken;
    protected String mGcmId;
    int mPoints = 0;
    int mOfferType;
    boolean mNativeXReady = false;
    String mIP;
    String mIMEI;

    TextView mUserNameView;
    TextView mPointsView;
    EditText mPaypalAccount;

    public static final int ACTION_SUPERSONICS = 29;
    public static final int ACTION_TAPJOY = 26;
    public static final int ACTION_NATIVEX = 22;
    public static final int ACTION_ADGATE = 23;

    public static final String BASE_URL = "http://miidow-showbox-hk.appspot.com/rest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_showbox);

        mUserNameView = (TextView) findViewById(R.id.user_name);
        mPointsView = (TextView) findViewById(R.id.points);
        mPaypalAccount = (EditText) findViewById(R.id.paypal_account);

        findViewById(R.id.btn_supersonic).setOnClickListener(this);
        findViewById(R.id.btn_tapjoy).setOnClickListener(this);
        findViewById(R.id.btn_nativex).setOnClickListener(this);
        findViewById(R.id.btn_adgate).setOnClickListener(this);
        findViewById(R.id.withdraw).setOnClickListener(this);
        findViewById(R.id.logout).setOnClickListener(this);
        findViewById(R.id.points_detail).setOnClickListener(this);

        initTapjoyOfferwall();

        NetworkUtils.init(this);
        initLogin();

        sInstant = new WeakReference<>(this);

        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        mIMEI = tm.getDeviceId();
    }

    public String getUserID() {
        String id = mToken + "|" + Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID) + "|" + mIP;
        if (mIMEI != null) {
            id += "|" + mIMEI;
        }

        return id;
    }

    public String getSuperUserID() {
        String id = mToken + "|" + "|" + mIP;
        return id;
    }

    public static void addPoints(int points) {
        if (sInstant != null) {
            ShowboxActivity activity = sInstant.get();
            if (activity != null) {
                activity.addPointsR(points);
            }
        }
    }

    void addPointsR(int points) {
        mPoints += points;
        mPointsView.setText(String.valueOf(mPoints));
    }

    void initLogin() {
        SharedPreferences pref = getSharedPreferences("showbox.pref", Context.MODE_PRIVATE);

        mUserName = pref.getString("Email", null);
        mToken = pref.getString("Token", null);
        mGcmId = getIntent().getStringExtra(EXTRA_GCM_TOKEN);
        if (TextUtils.isEmpty(mGcmId)) {
            Toast.makeText(this, "Something is wrong!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (mUserName != null) {
            showLoadingDialog("Loading profile...");
            NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "loadProfile", "email", mUserName, "sessionId", mToken, "regId", mGcmId, "countryCode", "HK"), loadProfileListener);
        } else {
            showLoadingDialog("Register a temp user");
            NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "systemUserLogin"), registerAndLoginListener);
        }
    }

    NetworkUtils.JsonObjectResultListener registerAndLoginListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    JSONObject data = json.getJSONObject("data");
                    if (!data.has("error")) {
                        mUserName = data.getString("email");
                        mToken = data.getString("sessionId");
                        SharedPreferences pref = getSharedPreferences("showbox.pref", Context.MODE_PRIVATE);
                        pref.edit().putString("Email", mUserName).putString("Token", mToken).apply();
                        showLoadingDialog("Loading profile...");
                        NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "loadProfile", "email", mUserName, "sessionId", mToken, "regId", mGcmId, "countryCode", "HK"), loadProfileListener);
                    } else {
                        Toast.makeText(ShowboxActivity.this, data.getString("error"), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            } catch (JSONException e) {
                Toast.makeText(ShowboxActivity.this, "Register fail! please retry later!", Toast.LENGTH_SHORT).show();
                finish();
                e.printStackTrace();
            }
        }
    };

    private SessionListener sessionListener = new SessionListener() {
        @Override
        public void createSessionCompleted(boolean success, boolean isOfferWallEnabled, String sessionId) {
            if (success) {
                mNativeXReady = true;
                //LogUtils.info("Wahoo! Now I'm ready to show an ad.");
            } else {
                //LogUtils.info("Oh no! Something isn't set up correctly - re-read the documentation or ask customer support for some help - https://selfservice.nativex.com/Help");
            }
        }
    };

    NetworkUtils.JsonObjectResultListener loadProfileListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    JSONObject data = json.getJSONObject("data");
                    if (!data.has("error")) {
                        mIP = data.getString("clientIP");
                        mMediationAgent = SupersonicFactory.getInstance();
                        mMediationAgent.initOfferwall(ShowboxActivity.this, "3d3c4121", getSuperUserID());
                        MonetizationManager.createSession(getApplicationContext(), "49489", getSuperUserID(), sessionListener);
                        adGateMedia = new AdGateMedia("nqyY", getSuperUserID());
                        TapjoyConnect connect = TapjoyConnect.getTapjoyConnectInstance();
                        if (connect != null) {
                            connect.setUserID(getUserID());
                        }
                        mUserNameView.setText(mUserName);
                        ClipboardManager cm  = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setText(mUserName);

                        String points = data.getString("points");
                        mPointsView.setText(points);
                        mPoints = Integer.parseInt(points);
                    } else {
                        Toast.makeText(ShowboxActivity.this, data.getString("error"), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            } catch (JSONException e) {
                Toast.makeText(ShowboxActivity.this, "LoadProfile! please retry later!", Toast.LENGTH_SHORT).show();
                SharedPreferences pref = getSharedPreferences("showbox.pref", Context.MODE_PRIVATE);
                pref.edit().remove("Email").remove("Token").apply();
                finish();
                e.printStackTrace();
            }
        }
    };

    public void showLoadingDialog(String message) {
        mProcessDlg = new ProgressDialog(ShowboxActivity.this);
        if (message != null) {
            mProcessDlg.setMessage(message);
        }
        mProcessDlg.setCancelable(false);
        mProcessDlg.show();
    }

    public void dismissLoadingDialog() {
        if (mProcessDlg != null) {
            mProcessDlg.dismiss();
            mProcessDlg = null;
        }
    }

    public void initTapjoyOfferwall() {
        Hashtable<String, Object> connectFlags = new Hashtable<String, Object>();
        TapjoyConnect.requestTapjoyConnect(ShowboxActivity.this,
                "b5af377d-a011-4e08-a4df-73feeef862a1", "ta83faARTgik33P-7vhioQEC8lY1nvOWeCJqcJLW2DScCtR7h9aOiOnxRBsg", connectFlags, new TapjoyConnectNotifier() {
                    @Override
                    public void connectSuccess(){
                        if (mUserName != null) {
                            TapjoyConnect.getTapjoyConnectInstance().setUserID(getUserID());
                        }
                    }

                    @Override
                    public void connectFail() {
                    }
                });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_supersonic) {
            mOfferType = ACTION_SUPERSONICS;
            showLoadingDialog(null);
            NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "checkOfferLimit", "email", mUserName, "sessionId", mToken, "offerwall", String.valueOf(ACTION_SUPERSONICS)), checkOfferwallListener);
        } else if (v.getId() == R.id.btn_nativex) {
            mOfferType = ACTION_NATIVEX;
            showLoadingDialog(null);
            NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "checkOfferLimit", "email", mUserName, "sessionId", mToken, "offerwall", String.valueOf(ACTION_NATIVEX)), checkOfferwallListener);
        } else if (v.getId() == R.id.btn_adgate) {
            mOfferType = ACTION_ADGATE;
            showLoadingDialog(null);
            NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "checkOfferLimit", "email", mUserName, "sessionId", mToken, "offerwall", String.valueOf(ACTION_ADGATE)), checkOfferwallListener);
        } else if (v.getId() == R.id.btn_tapjoy) {
            mOfferType = ACTION_TAPJOY;
            showLoadingDialog(null);
            NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "checkOfferLimit", "email", mUserName, "sessionId", mToken, "offerwall", String.valueOf(ACTION_TAPJOY)), checkOfferwallListener);
        } else if (v.getId() == R.id.logout) {
            SharedPreferences pref = getSharedPreferences("showbox.pref", Context.MODE_PRIVATE);
            pref.edit().remove("Email").remove("Token").apply();
            showLoadingDialog("Logout and Register a temp user...");
            NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "systemUserLogin"), registerAndLoginListener);
        } else if (v.getId() == R.id.points_detail) {
            showLoadingDialog("Loading points detail...");
            NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "loadPointDetails", "email", mUserName, "sessionId", mToken), pointsDetailListener);
        } else if (v.getId() == R.id.withdraw) {
            String account = mPaypalAccount.getText().toString();

            if (TextUtils.isEmpty(account)) {
                Toast.makeText(this,
                        "Please enter your PayPal email account.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (!CommUtils.isEmailValid(account)) {
                Toast.makeText(this,
                        "Please enter a valid PayPal email account.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (mPoints < 600) {
                Toast.makeText(this,
                        "You need to have at least 600 points before cashing out.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage(String.format("Are you sure to cash out %d points?", mPoints))
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes", cashoutListener);
            builder.create().show();
        }
    }

    DialogInterface.OnClickListener cashoutListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            showLoadingDialog("Cashing out...");
            NetworkUtils.doJsonObjectRequest(BASE_URL, NetworkUtils.genParamList("m", "redeemGift", "id", "92053de0-c851-45cb-984a-f31df639a136", "email", mUserName, "sessionId", mToken, "cashoutAccount", mPaypalAccount.getText().toString(), "countryCode", "HK", "type", "22", "points", String.valueOf(mPoints)), redeemGiftListener);
        }
    };

    NetworkUtils.JsonObjectResultListener redeemGiftListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    JSONObject data = json.getJSONObject("data");
                    if (!data.has("error")) {
                        addPointsR(-mPoints);
                        Toast.makeText(ShowboxActivity.this,
                                "Cash out successful.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ShowboxActivity.this, data.getString("error"), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    NetworkUtils.JsonObjectResultListener checkOfferwallListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    JSONObject data = json.getJSONObject("data");
                    if (!data.has("error")) {
                        String success = data.getString("success");
                        if (success.equals("1")) {
                            switch (mOfferType) {
                                case ACTION_SUPERSONICS:
                                    mMediationAgent.showOfferwall();
                                    break;
                                case ACTION_NATIVEX:
                                    if (mNativeXReady) {
                                        MonetizationManager.showAd(ShowboxActivity.this, "Player Generated Event");
                                    } else {
                                        Toast.makeText(ShowboxActivity.this, "NativeX is connecting, try later!", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case ACTION_ADGATE:
                                    adGateMedia.showOfferWall(null, ShowboxActivity.this);
                                    break;
                                case ACTION_TAPJOY:
                                    TapjoyConnect connect = TapjoyConnect.getTapjoyConnectInstance();
                                    if (connect != null) {
                                        connect.showOffers();
                                    } else {
                                        Toast.makeText(ShowboxActivity.this, "Tapjoy is connecting, try later!", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                            }
                        }
                    } else {
                        Toast.makeText(ShowboxActivity.this, data.getString("error"), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    NetworkUtils.JsonObjectResultListener pointsDetailListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    JSONObject data = json.getJSONObject("data");
                    if (!data.has("error")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ShowboxActivity.this)
                                .setCancelable(true)
                                .setTitle("Points Detail")
                                .setMessage(String.format("Tapjoy points: %s\nNativeX points: %s\nAdGate points: %s\nSupersonicAds points: %s", data.getString("tapjoyOhayouPoints"), data.getString("nativeXPoints"), data.getString("adGatePoints"), data.getString("supersonicadsOhayouPoints")))
                                .setPositiveButton("OK", null);
                        builder.create().show();
                    } else {
                        Toast.makeText(ShowboxActivity.this, data.getString("error"), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if (mMediationAgent != null) {
            mMediationAgent.onResume(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mMediationAgent != null) {
            mMediationAgent.onPause(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sInstant = null;
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    public void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false).setMessage("Do you really want to exit CashChaCha?");

        builder.setNegativeButton("NO", null);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dif, int arg1) {
                finish();
            }
        });

        builder.create().show();
    }
}
