package com.ohayou.japanese.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ohayou.japanese.R;
import com.ohayou.japanese.utils.CommUtils;

/**
 * Created by Oxygen on 15/8/9.
 */
public class SettingsActivity extends BaseActivity implements View.OnClickListener {

    TextView mClearDownload;
    TextView mVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        mClearDownload = (TextView)findViewById(R.id.clear_download);
        mVersion = (TextView)findViewById(R.id.version);

        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.clear).setOnClickListener(this);
        findViewById(R.id.rate).setOnClickListener(this);

        mVersion.setText(getString(R.string.version, CommUtils.getVersionName(this)));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.rate: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                startActivity(intent);
            }
                break;
            case R.id.clear: {
                Intent intent = new Intent(mContext, DeleteTextbookActivity.class);
                startActivity(intent);
            }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClearDownload.setText(getString(R.string.clear_download, CommUtils.getDownloadSize()));
    }
}
