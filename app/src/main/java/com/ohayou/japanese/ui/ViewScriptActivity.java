package com.ohayou.japanese.ui;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Html;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

// import com.facebook.ads.AdSize;
// import com.facebook.ads.AdView;
import com.appodeal.ads.Appodeal;
import com.ohayou.japanese.R;
import com.ohayou.japanese.model.UserInfo;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by Oxygen on 15/8/9.
 */
public class ViewScriptActivity extends BaseActivity implements View.OnClickListener{
    public static final String EXTRA_SCRIPT = "EXTRA_SCRIPT";
    public static final String EXTRA_AUDIO_PATH = "EXTRA_AUDIO_PATH";

    TextView mScriptView;
    String mScript;
    TextView mTranslateBotton;
    View mPrompt;

    ImageView mPlay;
    ImageView mRewind20;
    ImageView mFastforward20;
    ImageView mRewind10;
    ImageView mFastforward10;
    ImageView mRestart;

    MediaPlayer mPlayer;
    boolean mIsPlaying = false;
    String mAudioPath;

    MenuItem mTranslateItem;
    String mTranslateText;
    boolean mInTranslateMode = true;

    // RelativeLayout adViewContainer;
    // private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_script);

        mScriptView = (TextView)findViewById(R.id.script);
        mTranslateBotton = (TextView)findViewById(R.id.translate);

        mPlay = (ImageView)findViewById(R.id.play);
        mRestart = (ImageView)findViewById(R.id.restart);
        mRewind20 = (ImageView)findViewById(R.id.rewind20);
        mFastforward20 = (ImageView)findViewById(R.id.fastforward20);
        mRewind10 = (ImageView)findViewById(R.id.rewind10);
        mFastforward10 = (ImageView)findViewById(R.id.fastforward10);
        mPrompt = findViewById(R.id.translate_prompt);

        Intent intent = getIntent();
        mScript = intent.getStringExtra(EXTRA_SCRIPT);
        mAudioPath = intent.getStringExtra(EXTRA_AUDIO_PATH);
        mScriptView.setText(Html.fromHtml(mScript));
        mScriptView.setCustomSelectionActionModeCallback(callback);

        findViewById(R.id.back).setOnClickListener(this);
        mTranslateBotton.setOnClickListener(this);
        mPlay.setOnClickListener(this);
        mRestart.setOnClickListener(this);
        mRewind20.setOnClickListener(this);
        mFastforward20.setOnClickListener(this);
        mRewind10.setOnClickListener(this);
        mFastforward10.setOnClickListener(this);

        prepareAudio();

        if (!UserInfo.sRemovedAds) {
            /*
            adViewContainer = (RelativeLayout) findViewById(R.id.adViewContainer);

            adView = new AdView(this, "1011245595574360_1071185489580370", AdSize.BANNER_320_50);
            adViewContainer.addView(adView);
            adView.loadAd();
            */
            Appodeal.show(this, Appodeal.BANNER_VIEW);
        }
    }

    void prepareAudio() {
        mIsPlaying = false;
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setOnCompletionListener(completionListener);
            mPlayer.setOnErrorListener(onErrorListener);
        }
        try {
            mPlayer.reset();
            mPlayer.setDataSource(mAudioPath);
            mPlayer.prepare();
            mPlay.setImageResource(R.drawable.control_play);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mp.seekTo(0);
            mPlay.setImageResource(R.drawable.control_play);
            mIsPlaying = false;
        }
    };

    MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mPlay.setImageResource(R.drawable.control_play);
            mIsPlaying = false;
            prepareAudio();
            return true;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.translate:
                if (mScriptView.hasSelection()) {
                    if (mTranslateItem != null) {
                        mTranslateItem.collapseActionView();
                    }
                    final int selStart = mScriptView.getSelectionStart();
                    final int selEnd = mScriptView.getSelectionEnd();

                    int min = Math.max(0, Math.min(selStart, selEnd));
                    int max = Math.max(0, Math.max(selStart, selEnd));
                    mTranslateText = mScriptView.getText().subSequence(min, max).toString().replace("\n", "<br>");
                    showLoadingDialog(null);
                    NetworkUtils.doJsonObjectRequest(Constants.URL_GT, NetworkUtils.genParamList("text", mTranslateText), translateListener);
                } else {
                    mTranslateText = null;
                    mInTranslateMode = false;
                    mTranslateBotton.setText(R.string.back_to_scripts);
                    mTranslateBotton.setId(R.id.back_to_scripts);
                    mPrompt.setVisibility(View.VISIBLE);
                    showLoadingDialog(null);
                    NetworkUtils.doJsonObjectRequest(Constants.URL_GT, NetworkUtils.genParamList("text", mScript), translateListener);
                }
                break;
            case R.id.back_to_scripts:
                mInTranslateMode = true;
                mTranslateBotton.setText(R.string.translate);
                mTranslateBotton.setId(R.id.translate);
                mPrompt.setVisibility(View.GONE);
                mScriptView.setTextIsSelectable(true);
                mScriptView.setText(Html.fromHtml(mScript));
                break;
            case R.id.play:
                if (mIsPlaying) {
                    mPlayer.pause();
                    mPlay.setImageResource(R.drawable.control_play);
                } else {
                    mPlayer.start();
                    mPlay.setImageResource(R.drawable.control_pause);
                }
                mIsPlaying = !mIsPlaying;
                break;
            case R.id.rewind20:
                if (mIsPlaying) {
                    mPlayer.seekTo(mPlayer.getCurrentPosition() - 20000);
                }
                break;
            case R.id.rewind10:
                if (mIsPlaying) {
                    mPlayer.seekTo(mPlayer.getCurrentPosition() - 10000);
                }
                break;
            case R.id.restart:
                mPlayer.pause();
                mPlayer.seekTo(0);
                mPlayer.start();
                mIsPlaying = true;
                mPlay.setImageResource(R.drawable.control_pause);
                break;
            case R.id.fastforward10:
                if (mIsPlaying) {
                    mPlayer.seekTo(mPlayer.getCurrentPosition() + 10000);
                }
                break;
            case R.id.fastforward20:
                if (mIsPlaying) {
                    mPlayer.seekTo(mPlayer.getCurrentPosition() + 20000);
                }
                break;
        }
    }

    ActionMode.Callback callback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mInTranslateMode && mScriptView.hasSelection()) {
                mTranslateItem = menu.add(R.string.translate);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item == mTranslateItem) {
                onClick(mTranslateBotton);
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mTranslateItem = null;
        }
    };

    NetworkUtils.JsonObjectResultListener translateListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    JSONObject data = json.getJSONObject("data");
                    JSONArray translations = data.getJSONArray("translations");
                    String text = translations.getJSONObject(0).getString("translatedText");
                    if (mTranslateText != null) {
                        View view = View.inflate(mContext, R.layout.popupwindow_translate, null);
                        ((TextView)view.findViewById(R.id.text)).setText(Html.fromHtml(text));
                        PopupWindow popupWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

                        popupWindow.setBackgroundDrawable(new BitmapDrawable());
                        popupWindow.setOutsideTouchable(true);

                        popupWindow.showAtLocation(mScriptView, Gravity.CENTER, 0, 0);
                    } else {
                        mScriptView.setText(Html.fromHtml(text));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.connection_fail, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (!mInTranslateMode) {
            onClick(mTranslateBotton);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        prepareAudio();
        /*
        if (adViewContainer != null && UserInfo.sRemovedAds) {
            adViewContainer.setVisibility(View.GONE);
            adView.destroy();
        }
        */
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayer != null) {
            mIsPlaying = false;
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        /*
        if (adView != null) {
            adView.destroy();
        }
        */
        super.onDestroy();
    }
}
