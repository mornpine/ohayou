package com.ohayou.japanese.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.ohayou.japanese.OhayouApplication;
import com.ohayou.japanese.R;
import com.ohayou.japanese.db.DatabaseHelper;
import com.ohayou.japanese.model.Question;
import com.ohayou.japanese.model.SettingsStore;
import com.ohayou.japanese.model.Textbook;
import com.ohayou.japanese.model.UserInfo;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Created by Oxygen on 15/9/29.
 */
public class TextbookActivity extends BaseActivity implements View.OnClickListener{
    public static final String EXTRA_TID = "TID";

    DatabaseHelper mHelper;
    LinearLayout mQuestionNos;
    Textbook mTextbook;
    Question[] mQuestions;
    StringBuffer mAnswerStatus;

    TextView mPoints;
    TextView mBookName;
    TextView mCombo;

    int mCurSel;
    int mCurIndex = -1;

    View mResult;
    View mBgResult;
    ImageView mStatusImage;
    TextView mStatusAnswer;
    TextView mStatusPrompt;

    ImageView mPlay;
    ImageView mRewind20;
    ImageView mFastforward20;
    ImageView mRewind10;
    ImageView mFastforward10;
    ImageView mRestart;

    PopupWindow mPopupWindow;

    MediaPlayer mPlayer;
    boolean mIsPlaying = false;

    Question1QuestionFragment mQ1Fragment = new Question1QuestionFragment();
    Question2QuestionFragment mQ2Fragment = new Question2QuestionFragment();
    Question3QuestionFragment mQ3Fragment = new Question3QuestionFragment();

    RelativeLayout adViewContainer;
    private AdView adView;
    private InterstitialAd interstitialAd;

    static final int[][] sQuestionNoIcon = {
            {R.drawable.questionno_grey_left, R.drawable.questionno_grey_center, R.drawable.questionno_grey_right},
            {R.drawable.questionno_red_left, R.drawable.questionno_red_center, R.drawable.questionno_red_right},
            {R.drawable.questionno_green_left, R.drawable.questionno_green_center, R.drawable.questionno_green_right},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_textbook);

        mPoints = (TextView)findViewById(R.id.points);
        mBookName = (TextView)findViewById(R.id.book_name);
        mQuestionNos = (LinearLayout)findViewById(R.id.question_nos);
        mCombo = (TextView)findViewById(R.id.combo);

        mPlay = (ImageView)findViewById(R.id.play);
        mRestart = (ImageView)findViewById(R.id.restart);
        mRewind20 = (ImageView)findViewById(R.id.rewind20);
        mFastforward20 = (ImageView)findViewById(R.id.fastforward20);
        mRewind10 = (ImageView)findViewById(R.id.rewind10);
        mFastforward10 = (ImageView)findViewById(R.id.fastforward10);

        mResult = findViewById(R.id.result);
        mBgResult = findViewById(R.id.bg_result);
        mStatusImage = (ImageView)findViewById(R.id.status_image);
        mStatusPrompt = (TextView)findViewById(R.id.status_prompt);
        mStatusAnswer = (TextView)findViewById(R.id.status_answer);

        mCombo.setOnClickListener(this);
        mPlay.setOnClickListener(this);
        mRestart.setOnClickListener(this);
        mRewind20.setOnClickListener(this);
        mFastforward20.setOnClickListener(this);
        mRewind10.setOnClickListener(this);
        mFastforward10.setOnClickListener(this);
        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.view_script).setOnClickListener(this);

        mHelper = ((OhayouApplication)getApplication()).getDatabaseHelper();
        int tid = getIntent().getIntExtra(EXTRA_TID, -1);
        mTextbook = mHelper.getTextbook(tid);
        if (mTextbook == null) {
            Toast.makeText(this, "Something is wrong!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (mTextbook.questions == null) {
            Toast.makeText(this, getString(R.string.questions_not_ready), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mBookName.setText(mTextbook.name);

        try {
            mQuestions = Question.parseArray(new JSONArray(mTextbook.questions));
            if (mTextbook.status != null) {
                mAnswerStatus = new StringBuffer(mTextbook.status);
            } else {
                mAnswerStatus = new StringBuffer(mQuestions.length);
            }
            for (int i = mAnswerStatus.length(); i < mQuestions.length; ++i) {
                mAnswerStatus.append('0');
            }
            for (int i = 0; i < mQuestions.length; ++i) {
                View view = getLayoutInflater().inflate(R.layout.item_question_no, null);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                mQuestionNos.addView(view, params);
                updateQuestionStatus(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Something is wrong!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        startFirstQuestion();

        if (!UserInfo.sRemovedAds) {
            adViewContainer = (RelativeLayout) findViewById(R.id.adViewContainer);

            adView = new AdView(this, "1011245595574360_1071185489580370", AdSize.BANNER_320_50);
            adViewContainer.addView(adView);
            adView.loadAd();
        }
        UserInfo.addObserver(this);
        loadInterstitialAd();
    }

    void updateQuestionStatus(int idx) {
        int x, y = 1;
        View view = mQuestionNos.getChildAt(idx);
        ImageView imageView = (ImageView)view.findViewById(R.id.image);
        if (idx == 0) {
            y = 0;
        } else if (idx == mQuestions.length - 1) {
            y = 2;
        }
        x = mAnswerStatus.charAt(idx) - '0';
        imageView.setBackgroundResource(sQuestionNoIcon[x][y]);
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

    void startFirstQuestion() {
        for (int i = 0; i < mQuestions.length; ++i) {
            if (mAnswerStatus.charAt(i) == '0') {
                startQuestion(i);
                break;
            }
        }
    }

    private void loadInterstitialAd() {
        interstitialAd = new InterstitialAd(this, "1011245595574360_1088955797803339");
        interstitialAd.loadAd();
    }

    void startQuestion(int idx) {
        BaseQuestionFragment fragment = null;

        if (idx >= mQuestions.length) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.complete)
                    .setMessage(R.string.complete_content0)
                    .setNegativeButton(R.string.redo, redoListener)
                    .setPositiveButton(R.string.return_home, returnHomeListener);
            builder.create().show();

            if (interstitialAd.isAdLoaded()) {
                interstitialAd.show();
            }

            if (mTextbook.complete == 0) {
                NetworkUtils.doJsonObjectRequest(Constants.URL_ADD_POINTS, NetworkUtils.genParamList("email", UserInfo.sEmail, "points", String.valueOf(SettingsStore.points_complete_textbook), "type", "5"), resultListener);
            }
            mHelper.updateTextbook(mTextbook.tid, mTextbook.complete + 1);
            MixpanelAPI mixpanel = MixpanelAPI.getInstance(mContext, Constants.MIXPANEL_KEY);
            try {
                JSONObject props = new JSONObject();
                props.put("email", UserInfo.sEmail);
                props.put("textbook", mTextbook.tid);
                mixpanel.track("finish textbook", props);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }

        mCurIndex = idx;
        switch (mQuestions[idx].type) {
            case 1:
                fragment = new Question1QuestionFragment();
                //fragment = mQ1Fragment;
                break;
            case 2:
                fragment = new Question2QuestionFragment();
                //fragment = mQ2Fragment;
                break;
            case 3:
                fragment = new Question3QuestionFragment();
                //fragment = mQ3Fragment;
                break;
        }
        if (fragment != null) {
            setFragment(fragment);
            fragment.setQuestion(mQuestions[idx], idx);
        }
        prepareAudio();
        mCombo.setEnabled(false);
        mCombo.setText(R.string.check);
    }

    NetworkUtils.JsonObjectResultListener resultListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            if (json != null) {
                try {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        Toast.makeText(mContext, getString(R.string.complete_content1, SettingsStore.points_complete_textbook), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    DialogInterface.OnClickListener redoListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            for (int i = 0; i < mAnswerStatus.length(); ++i) {
                mAnswerStatus.setCharAt(i, '0');
                updateQuestionStatus(i);
            }
            saveStatus();
            startQuestion(0);
        }
    };

    DialogInterface.OnClickListener returnHomeListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            saveStatus();
            finish();
        }
    };

    void saveStatus() {
        int w = 0, r = 0;
        for (int i = 0; i < mAnswerStatus.length(); ++i) {
            char ch = mAnswerStatus.charAt(i);
            if (ch == '1') {
                w++;
            } else if (ch == '2') {
                r++;
            }
        }
        mHelper.updateTextbook(mTextbook.tid, r, w, mAnswerStatus.toString());
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(mContext, Constants.MIXPANEL_KEY);
        try {
            JSONObject props = new JSONObject();
            props.put("email", UserInfo.sEmail);
            props.put("textbook", mTextbook.tid);
            props.put("finish", (float)(r+w) / mTextbook.questions.length());
            mixpanel.track("exit textbook", props);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void setFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.question, fragment);
        transaction.commit();
    }


    public void setSelect(int index) {
        mCurSel = index;
        mCombo.setEnabled(true);
    }

    void showStatus(boolean show) {
        mResult.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            char status = mAnswerStatus.charAt(mCurIndex);
            mBgResult.setBackgroundResource(status == '2' ? R.drawable.bg_status_right : R.drawable.bg_status_wrong);
            mStatusImage.setImageResource(status == '2' ? R.drawable.icon_right_big : R.drawable.icon_wrong_big);
            mStatusPrompt.setVisibility(status == '2' ? View.GONE : View.VISIBLE);
            if (status == '2') {
                mStatusAnswer.setText(R.string.correct);
            } else {
                mStatusAnswer.setText(mQuestions[mCurIndex].answers[mQuestions[mCurIndex].correct_ans]);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                onBackPressed();
                break;
            case R.id.combo:
                if (mAnswerStatus.charAt(mCurIndex) == '0') {
                    if (mCurSel == mQuestions[mCurIndex].correct_ans) {
                        mAnswerStatus.setCharAt(mCurIndex, '2');
                    } else {
                        mAnswerStatus.setCharAt(mCurIndex, '1');
                    }
                    updateQuestionStatus(mCurIndex);
                    mCombo.setText(R.string.continue_);
                    showStatus(true);
                } else {
                    showStatus(false);
                    mCombo.setText(R.string.check);
                    startQuestion(mCurIndex + 1);
                }
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
            case R.id.view_script:
                if (mCurIndex >= 0 && mCurIndex < mQuestions.length) {
                    if (UserInfo.sViewScripts || mHelper.hasBuyInfo(UserInfo.sEmail, mTextbook.tid, mQuestions[mCurIndex].qid)) {
                        Intent intent = new Intent(mContext, ViewScriptActivity.class);
                        intent.putExtra(ViewScriptActivity.EXTRA_SCRIPT, mQuestions[mCurIndex].conversation);
                        intent.putExtra(ViewScriptActivity.EXTRA_AUDIO_PATH, mQuestions[mCurIndex].getAudioPath());
                        startActivity(intent);
                    } else {
                        View view = View.inflate(mContext, R.layout.popupwindow_script_purchase, null);
                        ((TextView)view.findViewById(R.id.remaining_points)).setText(getString(R.string.remaining_points) + " : " + String.valueOf(UserInfo.sPoints));
                        ((TextView)view.findViewById(R.id.one_script_points)).setText(String.valueOf(SettingsStore.points_view_script_1));
                        if (SettingsStore.points_view_script_1 < 2) {
                            ((TextView)view.findViewById(R.id.points1)).setText(R.string.point);
                        }
                        ((TextView)view.findViewById(R.id.textbook_script_points)).setText(String.valueOf(SettingsStore.points_view_script_textbook));
                        ((TextView)view.findViewById(R.id.script_points)).setText(String.valueOf(SettingsStore.points_view_scripts));
                        ((TextView)view.findViewById(R.id.buy_textbook_soundtracks)).setText(getString(R.string.buy_textbook_soundtracks, mQuestions.length));

                        view.findViewById(R.id.buy_all).setOnClickListener(buyClickListener);
                        view.findViewById(R.id.buy_textbook).setOnClickListener(buyClickListener);
                        view.findViewById(R.id.buy_question).setOnClickListener(buyClickListener);
                        view.findViewById(R.id.back).setOnClickListener(buyClickListener);

                        mPopupWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

                        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
                        mPopupWindow.setOutsideTouchable(true);

                        mPopupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
                    }
                }
                break;
        }
    }

    View.OnClickListener buyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPopupWindow.dismiss();
            mPopupWindow = null;
            int count = -1;
            int type = -1;
            switch (v.getId()) {
                case R.id.buy_all:
                    count = SettingsStore.points_view_scripts;
                    type = 3;
                    break;
                case R.id.buy_textbook:
                    count = SettingsStore.points_view_script_textbook;
                    type = 2;
                    break;
                case R.id.buy_question:
                    count = SettingsStore.points_view_script_1;
                    type = 1;
                    break;
                case R.id.back:
                    return;
            }
            if (count > 0) {
                if (UserInfo.sPoints >= count) {
                    buyScript(count, type);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(false)
                            .setMessage(R.string.not_sufficient_points_for_script)
                            .setNegativeButton(R.string.maybe_later, null)
                            .setPositiveButton(R.string.go_to_store, goStoreListener)
                            .create().show();
                }
            }
        }
    };

    void buyScript(final int count, final int type) {
        showLoadingDialog(null);
        List<NetworkUtils.KeyValue> params = NetworkUtils.genParamList("email", UserInfo.sEmail, "points", String.valueOf(count), "type", String.valueOf(type));
        if (type == 2) {
            params.add(new NetworkUtils.KeyValue("item", String.valueOf(mTextbook.tid)));
        } else if (type == 1) {
            params.add(new NetworkUtils.KeyValue("item", String.valueOf(mQuestions[mCurIndex].qid)));
        }
        NetworkUtils.doJsonObjectRequest(Constants.URL_DEL_POINTS, params, new NetworkUtils.JsonObjectResultListener() {
            @Override
            public void onResult(JSONObject json) {
                dismissLoadingDialog();
                try {
                    if (json != null) {
                        int status = json.getInt("status");
                        if (status == NetworkUtils.RET_CODE_SUCCESS) {
                            if (type == 3) {
                                UserInfo.sViewScripts = true;
                                NetworkUtils.doJsonObjectRequest(Constants.URL_UPDATE_USER, NetworkUtils.genParamList("email", UserInfo.sEmail, "can_view_scripts", "1"), updateUserListener);
                            } else {
                                mHelper.addBuyInfo(UserInfo.sEmail, mTextbook.tid, type == 2 ? -1 : mQuestions[mCurIndex].qid);
                            }
                            UserInfo.addPoints(-count);
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            builder.setCancelable(false)
                                    .setMessage(getString(R.string.redeemed_points_for_script, count))
                                    .setPositiveButton(R.string.view_script, viewScriptListener)
                                    .create().show();
                        } else {
                            NetworkUtils.showError(mContext, status);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    NetworkUtils.JsonObjectResultListener updateUserListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            dismissLoadingDialog();
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                    } else {
                        NetworkUtils.showError(mContext, status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };


    DialogInterface.OnClickListener goStoreListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            saveStatus();
            Intent intent = new Intent(mContext, StoreActivity.class);
            startActivity(intent);
        }
    };

    DialogInterface.OnClickListener viewScriptListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            Intent intent = new Intent(mContext, ViewScriptActivity.class);
            intent.putExtra(ViewScriptActivity.EXTRA_SCRIPT, mQuestions[mCurIndex].conversation);
            intent.putExtra(ViewScriptActivity.EXTRA_AUDIO_PATH, mQuestions[mCurIndex].getAudioPath());
            startActivity(intent);
        }
    };

    void prepareAudio() {
        if (mCurIndex != -1) {
            mIsPlaying = false;
            if (mPlayer == null) {
                mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setOnCompletionListener(completionListener);
                mPlayer.setOnErrorListener(onErrorListener);
            }
            try {
                mPlayer.reset();
                mPlayer.setDataSource(mQuestions[mCurIndex].getAudioPath());
                mPlayer.prepare();
                mPlay.setImageResource(R.drawable.control_play);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.stop_and_return);
        builder.setNegativeButton(R.string.no, null);
        builder.setPositiveButton(R.string.yes, yesListener);
        builder.create().show();
        //super.onBackPressed();
    }

    DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            saveStatus();
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        prepareAudio();
        if (adViewContainer != null && UserInfo.sRemovedAds) {
            adViewContainer.setVisibility(View.GONE);
            adView.destroy();
        }
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
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onUpdatePoints() {
        mPoints.setText(String.valueOf(UserInfo.sPoints));
    }
}
