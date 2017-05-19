package com.ohayou.japanese.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.ohayou.japanese.model.UserInfo;

/**
 * Created by Oxygen on 15/8/8.
 */
public class BaseActivity extends FragmentActivity implements UserInfo.UpdatePoints {
    protected Context mContext = this;
    protected ProgressDialog mProcessDlg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void showLoadingDialog(String message) {
        mProcessDlg = new ProgressDialog(mContext);
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

    public void changeLoadingMsg(String msg) {
        if (mProcessDlg != null) {
            mProcessDlg.setMessage(msg);
        }
    }

    @Override
    public void onUpdatePoints() {

    }
}
