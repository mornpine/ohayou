package com.ohayou.japanese.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.adgatemedia.sdk.classes.AdGateMedia;
import com.appodeal.ads.Appodeal;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.ohayou.japanese.R;
import com.ohayou.japanese.model.SettingsStore;
import com.ohayou.japanese.model.UserInfo;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.IabHelper;
import com.ohayou.japanese.utils.IabResult;
import com.ohayou.japanese.utils.Inventory;
import com.ohayou.japanese.utils.LogUtils;
import com.ohayou.japanese.utils.NetworkUtils;
import com.ohayou.japanese.utils.Purchase;
// import com.supersonic.mediationsdk.sdk.Supersonic;
import com.supersonic.mediationsdk.sdk.SupersonicFactory;
// import com.tapjoy.TapjoyConnect;
// import com.tapjoy.TapjoyConnectNotifier;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;

/**
 * Created by Oxygen on 15/8/24.
 */
public class StoreActivity extends BaseActivity implements View.OnClickListener {
    TextView mPoints;
    TextView mPoints1;
    TextView mPoints2;
    TextView mPoints3;
    TextView mMoney1;
    TextView mMoney2;
    TextView mMoney3;
    TextView mScriptPoints;
    TextView mScriptPoints2;
    TextView mAdsPoints;
    TextView mAdsPoints2;
    View mBuyScript;
    View mRemoveAds;
    EditText mPromoCode;

    IabHelper mHelper;
    boolean mHelperSetup;
    Inventory mInventory;

    // private Supersonic mMediationAgent;
    private AdGateMedia adGateMedia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_store);

        // Appodeal
        String appKey = "7fadc00038f2cd73e1190dd2940ae838d45d6b061c7049d4";
        Appodeal.getUserSettings(this).setUserId("123123");
        Appodeal.initialize(this, appKey, Appodeal.REWARDED_VIDEO);
        Appodeal.setRewardedVideoCallbacks(new AppodealRewardedVideoCallbacks(this));
        Appodeal.cache(this, Appodeal.REWARDED_VIDEO);

        mPoints = (TextView) findViewById(R.id.points);
        mPromoCode = (EditText) findViewById(R.id.promo_code);
        mPoints1 = (TextView) findViewById(R.id.points1);
        mPoints2 = (TextView) findViewById(R.id.points2);
        mPoints3 = (TextView) findViewById(R.id.points3);
        mMoney1 = (TextView) findViewById(R.id.buy_money1);
        mMoney2 = (TextView) findViewById(R.id.buy_money2);
        mMoney3 = (TextView) findViewById(R.id.buy_money3);
        mScriptPoints = (TextView) findViewById(R.id.script_points);
        mScriptPoints2 = (TextView) findViewById(R.id.script_points2);
        mAdsPoints = (TextView) findViewById(R.id.ads_points);
        mAdsPoints2 = (TextView) findViewById(R.id.ads_points2);
        mBuyScript = findViewById(R.id.buy_script);
        mRemoveAds = findViewById(R.id.remove_ads);

        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.buy_points1).setOnClickListener(this);
        findViewById(R.id.buy_points2).setOnClickListener(this);
        findViewById(R.id.buy_points3).setOnClickListener(this);
        findViewById(R.id.redeem).setOnClickListener(this);
        // findViewById(R.id.btn_supersonic).setOnClickListener(this);
        // findViewById(R.id.btn_tapjoy).setOnClickListener(this);
        findViewById(R.id.btn_adgate).setOnClickListener(this);
        mBuyScript.setOnClickListener(this);
        mRemoveAds.setOnClickListener(this);


        if (SettingsStore.init) {
            mPoints1.setText(String.valueOf(SettingsStore.points_exchange_1));
            mPoints2.setText(String.valueOf(SettingsStore.points_exchange_2));
            mPoints3.setText(String.valueOf(SettingsStore.points_exchange_3));
            mMoney1.setText(String.format("US$%.2f", SettingsStore.points_exchange_1_rate));
            mMoney2.setText(String.format("US$%.2f", SettingsStore.points_exchange_2_rate));
            mMoney3.setText(String.format("US$%.2f", SettingsStore.points_exchange_3_rate));
        } else {
            Toast.makeText(mContext, "Something is unexpected, please try reopen app!", Toast.LENGTH_LONG).show();
            finish();
        }

        mHelper = new IabHelper(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvbZC/2QMR1+A/5EnyLjcnOhPZkYMnBDd0Y8PS9Eh8QgRHubIY0sqvP1N7ds8Dzbnqr5VgyQ+yQ7azA2HsR/MiXtqMr5W7Ktluf4LhsynBTt1kcSsgYWmfrttnvtFJO1detq8dHWUPpJuI+2qHQV011S2uRSKZaDPT9IblcvlvtTZgtJ/WM1aUCp0tkJSeT6lqiyZ6CoHZXbRch0VRn1nSk+PdDMNXxSW0c66jAJkBkS1S5jBtt9TEXAyb7OHQOlbiPmyPN3sEZUKrBdHUT5F4ERMXbPbGB0PazWS0WNXdlqjXj0CHFsDar3figjEZfcaWXwkUXeFcPhzKGcRlmMapQIDAQAB");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    LogUtils.error("Problem setting up In-app Billing: " + result);
                    Toast.makeText(mContext, "Fail to setup Billing!", Toast.LENGTH_LONG).show();
                    mHelper = null;
                } else {
                    mHelperSetup = true;
                    mHelper.queryInventoryAsync(inventoryListener);
                }
            }
        });

        UserInfo.addObserver(this);
        initOfferwall();
    }

    void initOfferwall() {
        Hashtable<String, Object> connectFlags = new Hashtable<String, Object>();
        /*
        TapjoyConnect.requestTapjoyConnect(mContext,
                "b5af377d-a011-4e08-a4df-73feeef862a1", "ta83faARTgik33P-7vhioQEC8lY1nvOWeCJqcJLW2DScCtR7h9aOiOnxRBsg", connectFlags, new TapjoyConnectNotifier() {
                    @Override
                    public void connectSuccess() {
                        TapjoyConnect.getTapjoyConnectInstance().setUserID(UserInfo.sEmail);
                    }

                    @Override
                    public void connectFail() {
                    }
                });
        */

        // mMediationAgent = SupersonicFactory.getInstance();
        // mMediationAgent.initOfferwall(this, "3d3c4121", UserInfo.sEmail);
        adGateMedia = new AdGateMedia("nqyY", UserInfo.sEmail);
    }

    IabHelper.QueryInventoryFinishedListener inventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result,
                                             Inventory inventory) {
            if (result.isFailure()) {
                LogUtils.error("Get inventory fail: " + result);
                mHelper.dispose();
                mHelper = null;
            } else {
                mInventory = inventory;
            }
        }
    };

    IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isSuccess()) {
                float money = 0;
                if (info.getSku().equals("buy099")) {
                    money = SettingsStore.points_exchange_1_rate;
                } else if (info.getSku().equals("buy299")) {
                    money = SettingsStore.points_exchange_2_rate;
                } else if (info.getSku().equals("buy499")) {
                    money = SettingsStore.points_exchange_3_rate;
                }
                MixpanelAPI mixpanel = MixpanelAPI.getInstance(mContext, Constants.MIXPANEL_KEY);
                try {
                    JSONObject props = new JSONObject();
                    props.put("email", UserInfo.sEmail);
                    props.put("count", money);
                    mixpanel.track("pay money", props);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                showLoadingDialog(null);
                mHelper.consumeAsync(info, consumeFinishedListener);
            } else {
                Toast.makeText(mContext, R.string.purchase_cancel, Toast.LENGTH_LONG).show();
            }
        }
    };

    IabHelper.OnConsumeFinishedListener consumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (result.isSuccess()) {
                int type = 0;
                float money = 0;
                int buyPoints = 0;
                if (purchase.getSku().equals("buy099")) {
                    buyPoints = SettingsStore.points_exchange_1;
                    type = 1;
                    money = SettingsStore.points_exchange_1_rate;
                } else if (purchase.getSku().equals("buy299")) {
                    buyPoints = SettingsStore.points_exchange_2;
                    type = 2;
                    money = SettingsStore.points_exchange_2_rate;
                } else if (purchase.getSku().equals("buy499")) {
                    buyPoints = SettingsStore.points_exchange_3;
                    type = 3;
                    money = SettingsStore.points_exchange_3_rate;
                }

                if (buyPoints > 0) {
                    MixpanelAPI mixpanel = MixpanelAPI.getInstance(mContext, Constants.MIXPANEL_KEY);
                    try {
                        JSONObject props = new JSONObject();
                        props.put("email", UserInfo.sEmail);
                        props.put("count", money);
                        mixpanel.track("consume money", props);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    NetworkUtils.doJsonObjectRequest(Constants.URL_ADD_POINTS, NetworkUtils.genParamList("email", UserInfo.sEmail, "points", String.valueOf(buyPoints), "type", String.valueOf(type)), resultListener);
                    UserInfo.addPoints(buyPoints);
                    Toast.makeText(mContext, getString(R.string.points_add, buyPoints), Toast.LENGTH_LONG).show();
                }
            } else {
                dismissLoadingDialog();
                Toast.makeText(mContext, "Consume error: " + result.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    };

    NetworkUtils.JsonObjectResultListener resultListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            if (json != null) {
                try {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
        UserInfo.removeObserver(this);
    }

    void updateUserInfoViews() {
        if (UserInfo.sViewScripts) {
            mScriptPoints.setText(R.string.redeemed);
            mScriptPoints.setTextColor(getResources().getColor(R.color.disabled_dark));
            mScriptPoints2.setVisibility(View.GONE);
            mBuyScript.setEnabled(false);
            mBuyScript.setClickable(false);
        } else {
            mScriptPoints.setText(String.valueOf(SettingsStore.points_view_scripts));
        }
        if (UserInfo.sRemovedAds) {
            mAdsPoints.setText(R.string.redeemed);
            mAdsPoints.setTextColor(getResources().getColor(R.color.disabled_dark));
            mAdsPoints2.setVisibility(View.GONE);
            mRemoveAds.setEnabled(false);
            mRemoveAds.setClickable(false);
        } else {
            mAdsPoints.setText(String.valueOf(SettingsStore.points_remove_ads));
        }
        mPoints.setText(String.valueOf(UserInfo.sPoints));
    }

    @Override
    public void onUpdatePoints() {
        updateUserInfoViews();
    }

    @Override
    public void onClick(View v) {
        String skuName = null;
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.buy_points1:
                skuName = "buy099";
                break;
            case R.id.buy_points2:
                skuName = "buy299";
                break;
            case R.id.buy_points3:
                skuName = "buy499";
                break;
            case R.id.buy_script: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setCancelable(false);
                if (UserInfo.sPoints >= SettingsStore.points_view_scripts) {
                    builder.setMessage(getString(R.string.prompt_for_purchase, getString(R.string.script_dict), SettingsStore.points_view_scripts))
                            .setNegativeButton(R.string.yes, purchaseViewScriptsListener)
                            .setPositiveButton(R.string.no, null);
                } else {
                    builder.setMessage(R.string.points_not_sufficient)
                            .setNegativeButton(R.string.ok, null);
                }
                builder.create().show();
                break;
            }
            case R.id.remove_ads: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setCancelable(false);
                if (UserInfo.sPoints >= SettingsStore.points_remove_ads) {
                    builder.setMessage(getString(R.string.prompt_for_purchase, getString(R.string.remove_ads), SettingsStore.points_remove_ads))
                            .setNegativeButton(R.string.yes, purchaseRemoveAdsListener)
                            .setPositiveButton(R.string.no, null);
                } else {
                    builder.setMessage(R.string.points_not_sufficient)
                            .setNegativeButton(R.string.ok, null);
                }
                builder.create().show();
                break;
            }
            case R.id.redeem:
                String promoCode = mPromoCode.getText().toString();
                if (!promoCode.isEmpty()) {
                    showLoadingDialog(null);
                    NetworkUtils.doJsonObjectRequest(Constants.URL_CHECK_PROMO_CODE, NetworkUtils.genParamList("email", UserInfo.sEmail, "promo_code", promoCode), checkPromoCodeListener);
                }
                break;
            /*
            case R.id.btn_supersonic:
                mMediationAgent.showOfferwall();
                break;
            */
            case R.id.btn_adgate:
                // adGateMedia.showOfferWall(null, mContext);
                Appodeal.show(this, Appodeal.REWARDED_VIDEO);

                break;
            /*
            case R.id.btn_tapjoy:
                TapjoyConnect connect = TapjoyConnect.getTapjoyConnectInstance();
                if (connect != null) {
                    connect.showOffers();
                } else {
                    Toast.makeText(this, "Tapjoy is connecting, try later!", Toast.LENGTH_SHORT).show();
                }
                break;
             */
        }

        if (skuName != null) {
            if (mInventory != null) {
                if (mInventory.hasPurchase(skuName)) {
                    showLoadingDialog(null);
                    mHelper.consumeAsync(mInventory.getPurchase(skuName), consumeFinishedListener);
                } else {
                    try {
                        mHelper.launchPurchaseFlow(StoreActivity.this, skuName, 100, purchaseFinishedListener);
                    } catch (IllegalStateException e) {
                        Toast.makeText(mContext, "Purchase not finish normally, Please try later!", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(mContext, R.string.loading_inventory, Toast.LENGTH_LONG).show();
            }
        }
    }

    NetworkUtils.JsonObjectResultListener checkPromoCodeListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        String points = json.getString("points");
                        String promoCode = mPromoCode.getText().toString();
                        NetworkUtils.doJsonObjectRequest(Constants.URL_ADD_POINTS, NetworkUtils.genParamList("email", UserInfo.sEmail, "points", points, "type", "4", "promo_code", promoCode), addPointsResultListener);
                        UserInfo.addPoints(Integer.parseInt(points));
                        Toast.makeText(mContext, json.getString("data"), Toast.LENGTH_LONG).show();
                    } else {
                        NetworkUtils.showError(mContext, status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    NetworkUtils.JsonObjectResultListener addPointsResultListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            if (json != null) {
                try {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    DialogInterface.OnClickListener purchaseViewScriptsListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            showLoadingDialog(null);
            NetworkUtils.doJsonObjectRequest(Constants.URL_DEL_POINTS, NetworkUtils.genParamList("email", UserInfo.sEmail, "points", String.valueOf(SettingsStore.points_view_scripts), "type", "3"), purchaseViewScriptsResultListener);
        }
    };

    DialogInterface.OnClickListener purchaseRemoveAdsListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            showLoadingDialog(null);
            NetworkUtils.doJsonObjectRequest(Constants.URL_DEL_POINTS, NetworkUtils.genParamList("email", UserInfo.sEmail, "points", String.valueOf(SettingsStore.points_remove_ads), "type", "4"), purchaseRemoveAdsResultListener);
        }
    };

    NetworkUtils.JsonObjectResultListener purchaseViewScriptsResultListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        NetworkUtils.doJsonObjectRequest(Constants.URL_UPDATE_USER, NetworkUtils.genParamList("email", UserInfo.sEmail, "can_view_scripts", "1"), updateUserViewScriptsListener);
                        UserInfo.addPoints(-SettingsStore.points_view_scripts);
                        UserInfo.sViewScripts = true;
                    } else {
                        NetworkUtils.showError(mContext, status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    NetworkUtils.JsonObjectResultListener purchaseRemoveAdsResultListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        NetworkUtils.doJsonObjectRequest(Constants.URL_UPDATE_USER, NetworkUtils.genParamList("email", UserInfo.sEmail, "can_remove_ads", "1"), updateUserRemoveAdsListener);
                        UserInfo.addPoints(-SettingsStore.points_remove_ads);
                        UserInfo.sRemovedAds = true;
                    } else {
                        NetworkUtils.showError(mContext, status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    NetworkUtils.JsonObjectResultListener updateUserViewScriptsListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        Toast.makeText(mContext, getString(R.string.successfully_purchase, SettingsStore.points_view_scripts, getString(R.string.script_dict)), Toast.LENGTH_LONG).show();
                    } else {
                        NetworkUtils.showError(mContext, status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    NetworkUtils.JsonObjectResultListener updateUserRemoveAdsListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        Toast.makeText(mContext, getString(R.string.successfully_purchase, SettingsStore.points_remove_ads, getString(R.string.remove_ads)), Toast.LENGTH_LONG).show();
                    } else {
                        NetworkUtils.showError(mContext, status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelperSetup || !mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            LogUtils.info("onActivityResult handled by IABUtil.");
        }
    }
}
