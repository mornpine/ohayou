package com.ohayou.japanese.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ohayou.japanese.R;
import com.ohayou.japanese.model.UserInfo;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Oxygen on 15/8/9.
 */
public class FeedbackActivity extends BaseActivity {

    EditText mComment;
    EditText mEmail;
    View mSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_feedback);

        mComment = (EditText)findViewById(R.id.comment);
        mEmail = (EditText)findViewById(R.id.email);
        mSubmit = findViewById(R.id.submit);

        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String comment = mComment.getText().toString();
                String email = mEmail.getText().toString();
                if (email.isEmpty()) {
                    email = UserInfo.sEmail;
                }
                showLoadingDialog(null);
                NetworkUtils.doJsonObjectRequest(Constants.URL_FEEDBACK, NetworkUtils.genParamList("email", email, "text", comment),resultListener);
            }
        });

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeedbackActivity.this.finish();
            }
        });

        mComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean valid = s.length() > 10;
                if (valid != mSubmit.isClickable()) {
                    mSubmit.setClickable(valid);
                    mSubmit.setBackgroundResource(valid ? R.drawable.btn_big_next : R.drawable.btn_big_disabled);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    NetworkUtils.JsonObjectResultListener resultListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        String msg = json.getString("data");
                        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                        finish();
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
