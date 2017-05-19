package com.ohayou.japanese.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.ohayou.japanese.OhayouApplication;
import com.ohayou.japanese.R;
import com.ohayou.japanese.db.DatabaseHelper;
import com.ohayou.japanese.model.UserInfo;
import com.ohayou.japanese.utils.CommUtils;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Oxygen on 15/8/8.
 */
public class PasswordActivity extends BaseActivity implements View.OnClickListener{
    public static final String EXTRA_SIGNUP_MODE = "SIGNUP_MODE";

    EditText mPassword;
    TextView mCommit;
    boolean mIsSignUpMode = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_password);

        mPassword = (EditText)findViewById(R.id.password);
        mCommit = (TextView)findViewById(R.id.commit);

        mPassword.addTextChangedListener(watcher);
        mPassword.requestFocus();
        mCommit.setOnClickListener(this);
        mCommit.setClickable(false);
        findViewById(R.id.back).setOnClickListener(this);

        Intent intent = getIntent();
        mIsSignUpMode = intent.getBooleanExtra(EXTRA_SIGNUP_MODE, true);

        if (!mIsSignUpMode) {
            mPassword.setHint(R.string.hint_input_password);
            mCommit.setText(R.string.login);
            ((TextView)findViewById(R.id.title)).setText(R.string.login);
        }

        if (UserInfo.sEmail.isEmpty()) {
            Toast.makeText(this, "Something must be wrong", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean valid = s.length() > 5;
            if (valid != mCommit.isClickable()) {
                mCommit.setClickable(valid);
                mCommit.setBackgroundResource(valid ? R.drawable.btn_big_next : R.drawable.btn_big_disabled);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.commit:
                String password = mPassword.getText().toString();
                String url = mIsSignUpMode ? Constants.URL_REGISTER : Constants.URL_LOGIN;
                NetworkUtils.doJsonObjectRequest(url, NetworkUtils.genParamList("email", UserInfo.sEmail, "password", CommUtils.md5Encode(password)), listener);
                showLoadingDialog(null);
                break;
            case R.id.back:
                finish();
                break;
        }
    }

    NetworkUtils.JsonObjectResultListener listener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        UserInfo.parse(json.getJSONArray("data"));
                        Intent intent = new Intent(mContext, HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        if (mIsSignUpMode) {
                            MixpanelAPI mixpanel = MixpanelAPI.getInstance(mContext, Constants.MIXPANEL_KEY);
                            try {
                                JSONObject props = new JSONObject();
                                props.put("email", UserInfo.sEmail);
                                mixpanel.track("sign up", props);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        DatabaseHelper helper = ((OhayouApplication)getApplication()).getDatabaseHelper();
                        String password = mPassword.getText().toString();
                        if (!UserInfo.sEmail.equals(helper.getUsername()) || !password.equals(helper.getPassword())) {
                            helper.setUser(UserInfo.sEmail, CommUtils.md5Encode(password));
                        }
                    } else {
                        NetworkUtils.showError(mContext, status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
}
