package com.ohayou.japanese.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.ohayou.japanese.OhayouApplication;
import com.ohayou.japanese.R;
import com.ohayou.japanese.db.DatabaseHelper;
import com.ohayou.japanese.model.UserInfo;
import com.ohayou.japanese.service.OhayouGcmListenerService;
import com.ohayou.japanese.utils.CommUtils;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.NetworkUtils;
import com.showbox.cashchacha.ui.ShowboxActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Oxygen on 15/8/6.
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    EditText mEditEmail;
    TextView mBtnNext;
    TextView mBtnFacebook;
    boolean mMatch = false;
    boolean mChecking = false;
    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mEditEmail = (EditText)findViewById(R.id.edit_email);
        mBtnNext = (TextView)findViewById(R.id.btn_next);
        mBtnFacebook = (TextView)findViewById(R.id.btn_facebook);

        mEditEmail.addTextChangedListener(watcher);
        mBtnNext.setOnClickListener(this);
        mBtnFacebook.setOnClickListener(this);
        mBtnNext.setClickable(false);

        findViewById(R.id.cancel).setOnClickListener(this);

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, loginResult);

        DatabaseHelper helper = ((OhayouApplication)getApplication()).getDatabaseHelper();
        String username = helper.getUsername();
        if (username != null) {
            if (username.equalsIgnoreCase("cashchacha")) {
                mEditEmail.setText(username);
                mEditEmail.setSelection(username.length());
            } else {
                String password = helper.getPassword();
                if (password != null && password.equals("FACEBOOK")) {
                    showLoadingDialog(null);
                    NetworkUtils.doJsonObjectRequest(Constants.URL_LOGIN_FB, NetworkUtils.genParamList("email", username), loginListener);
                } else {
                    mEditEmail.setText(username);
                    mEditEmail.setSelection(username.length());
                    if (password != null && !password.isEmpty()) {
                        showLoadingDialog(null);
                        NetworkUtils.doJsonObjectRequest(Constants.URL_LOGIN, NetworkUtils.genParamList("email", username, "password", password), loginListener);
                    }
                }
            }
        }
    }

    TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean match = CommUtils.isEmailValid(s);
            if (mMatch != match) {
                mMatch = match;
                mBtnNext.setClickable(match);
                mBtnNext.setBackgroundResource(match ? R.drawable.btn_big_next : R.drawable.btn_big_disabled);
            }
            if (s.toString().equalsIgnoreCase("cashchacha")) {
                DatabaseHelper helper = ((OhayouApplication)getApplication()).getDatabaseHelper();
                helper.setUser("cashchacha", "");
                goShowbox();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    void goShowbox() {
        if (UserInfo.sGcmToken != null) {
            OhayouGcmListenerService.sOhayouMode = false;
            Intent intent = new Intent(mContext, ShowboxActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ShowboxActivity.EXTRA_GCM_TOKEN, UserInfo.sGcmToken);
            startActivity(intent);
        } else {
            Toast.makeText(mContext, "GCM is not ready, please try later!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                finish();
                break;
            case R.id.btn_next:
                if (!mChecking) {
                    mChecking = true;
                    ArrayList<NetworkUtils.KeyValue> params = new ArrayList<>();
                    params.add(new NetworkUtils.KeyValue("email", mEditEmail.getText().toString()));
                    showLoadingDialog(null);
                    NetworkUtils.doJsonObjectRequest(Constants.URL_CHECK_EMAIL, params, checkEmailListener);
                }
                break;
            case R.id.btn_facebook:
                ArrayList<String> perms = new ArrayList<>();
                perms.add("email");
                LoginManager.getInstance().logInWithReadPermissions(this, perms);
                break;
        }
    }

    NetworkUtils.JsonObjectResultListener checkEmailListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            mChecking = false;
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    switch (status) {
                        case NetworkUtils.RET_CODE_SUCCESS:
                        case NetworkUtils.RET_CODE_USER_NOT_EXIST: {
                            OhayouGcmListenerService.sOhayouMode = true;
                            UserInfo.sEmail = mEditEmail.getText().toString();
                            Intent intent = new Intent(mContext, PasswordActivity.class);
                            intent.putExtra(PasswordActivity.EXTRA_SIGNUP_MODE, status == NetworkUtils.RET_CODE_USER_NOT_EXIST);
                            startActivity(intent);
                            break;
                        }
                        default: {
                            NetworkUtils.showError(mContext, status);
                            break;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void makeMeRequest(final AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        //{"id":"","gender":"male","email":"","name":""}
                        try {
                            String email = object.getString("email");
                            showLoadingDialog(null);
                            DatabaseHelper helper = ((OhayouApplication)getApplication()).getDatabaseHelper();
                            helper.setUser(email, "FACEBOOK");
                            NetworkUtils.doJsonObjectRequest(Constants.URL_LOGIN_FB, NetworkUtils.genParamList("email", email), loginListener);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "email,gender,name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    NetworkUtils.JsonObjectResultListener loginListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        UserInfo.parse(json.getJSONArray("data"));
                        OhayouGcmListenerService.sOhayouMode = true;
                        Intent intent = new Intent(mContext, HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        NetworkUtils.showError(mContext, status);
                        DatabaseHelper helper = ((OhayouApplication)getApplication()).getDatabaseHelper();
                        helper.clearUser();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    FacebookCallback<LoginResult> loginResult = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            makeMeRequest(loginResult.getAccessToken());
        }

        @Override
        public void onCancel() {
        }

        @Override
        public void onError(FacebookException exception) {
            exception.printStackTrace();
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
