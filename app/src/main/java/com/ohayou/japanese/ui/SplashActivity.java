package com.ohayou.japanese.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.widget.Toast;

import com.loopj.android.http.*;
import com.ohayou.japanese.R;
import com.ohayou.japanese.utils.CommUtils;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.LogUtils;
import com.ohayou.japanese.utils.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Oxygen on 15/8/5.
 */
public class SplashActivity extends BaseActivity implements UIHandler.MsgHandler {

    String mUpgradeUrl;
    UIHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        mHandler = new UIHandler(this);
        NetworkUtils.doJsonObjectRequest(Constants.URL_CHECK_UPGRADE, null, upgradeListener);
    }

    NetworkUtils.JsonObjectResultListener upgradeListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        JSONObject data = json.getJSONObject("data");
                        int compulsory = Integer.parseInt(data.getString("compulsory"));
                        int version = Integer.parseInt(data.getString("version"));
                        String url = data.getString("url");
                        showUpgradeDlg(compulsory == 1, url);
                    } else if (status == NetworkUtils.RET_CODE_NO_UPDATE){
                        nextStep();
                    } else {
                        NetworkUtils.showError(mContext, status);
                        nextStep();
                    }
                } else {
                    nextStep();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    void showUpgradeDlg(boolean force, String url) {
        mUpgradeUrl = url;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(R.string.dialog_optional_update)
                .setNegativeButton(R.string.dialog_btn_update, updateListener);
        if (force) {
            builder.setPositiveButton(R.string.dialog_btn_exit,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            nextStep();
                        }
                    });
        } else {
            builder.setPositiveButton(R.string.dialog_btn_skip,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            nextStep();
                        }
                    });
        }

        builder.create().show();

    }

    DialogInterface.OnClickListener updateListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            //CommUtils.openGoogleMarketWithName(mContext, mContext.getPackageName());
            showLoadingDialog(getResources().getString(R.string.downloading) + "...");
            DownloadRequest request = new DownloadRequest(mUpgradeUrl);
            request.execute();
        }
    };

    void nextStep() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case -1:
                dismissLoadingDialog();
                Toast.makeText(mContext, getResources().getString(R.string.download_fail), Toast.LENGTH_LONG).show();
                SplashActivity.this.finish();
                break;
            case 1:
                changeLoadingMsg(getResources().getString(R.string.downloading) + " " + (String)msg.obj);
                break;
            case 2:
                dismissLoadingDialog();
                File file = (File)msg.obj;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                startActivity(intent);
                SplashActivity.this.finish();
                break;
            default:
                break;
        }
    }

    public class DownloadRequest {
        private AsyncHttpClient mAsyncHttpClient;

        private String mUrl;

        private FileAsyncHttpResponseHandler mHttpHandler;

        public DownloadRequest(String url) {
            mUrl = url;

            final String fileName = "upgrade.apk";
            File file = new File(Constants.DOWNLOAD_CACHE_PATH, fileName + ".tmp");
            if (file.exists()) {
                file.delete();
            }
            mHttpHandler = new FileAsyncHttpResponseHandler(file) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File file) {
                    for (Header header : headers) {
                        LogUtils.info("--->" + header.getName() + " = " + header.getValue());
                    }
                    File newFile = new File(Constants.DOWNLOAD_CACHE_PATH, fileName);
                    file.renameTo(newFile);
                    mHandler.obtainMessage(2, newFile).sendToTarget();
                }
                @Override
                public void onFailure(int statusCode, Header[] arg1, Throwable arg2, File file) {
                    if (file.exists()) {
                        file.delete();
                    }
                    mHandler.obtainMessage(-1).sendToTarget();
                }
                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    String msg;
                    if (totalSize > bytesWritten) {
                        msg = String.format("%d%%", bytesWritten * 100 / totalSize);
                    } else {
                        msg = CommUtils.formatSize(bytesWritten);
                    }
                    mHandler.obtainMessage(1, msg).sendToTarget();
                    super.onProgress(bytesWritten, totalSize);
                }
            };

            if (mAsyncHttpClient == null) {
                mAsyncHttpClient = new AsyncHttpClient();
            }
        }

        public void execute() {
            mAsyncHttpClient.get(mUrl, mHttpHandler);
        }
    }
}
