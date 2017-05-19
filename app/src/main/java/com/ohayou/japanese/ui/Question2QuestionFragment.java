package com.ohayou.japanese.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.ohayou.japanese.R;
import com.ohayou.japanese.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Oxygen on 15/10/6.
 */
public class Question2QuestionFragment extends BaseQuestionFragment {
    LinearLayout mAnswers;
    View mCurView = null;
    int mWithoutImageHeight = -1;
    List<ImageView> mImageList = new ArrayList<>();

    @Override
    protected void loadQuestion() {
        super.loadQuestion();
        mAnswers.removeAllViews();
        LinearLayout curRow = null;
        mImageList.clear();
        mWithoutImageHeight = -1;
        for (int i = 0; i < mQuestion.answers.length; ++i) {
            View view = mActivity.getLayoutInflater().inflate(R.layout.item_question2, null);
            ImageView imageView = (ImageView)view.findViewById(R.id.image);
            RadioButton button = (RadioButton)view.findViewById(R.id.answer);
            if (i % 2 == 0) {
                curRow = new LinearLayout(mActivity);
                curRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mAnswers.addView(curRow, params);
            }
            imageView.setTag(Uri.parse(mQuestion.getImagePath(i)));
            mImageList.add(imageView);
            button.setText(mQuestion.answers[i]);
            button.setChecked(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            int margin = mActivity.getResources().getDimensionPixelSize(R.dimen.question1_answer_margin);
            params.setMargins(margin, margin, margin, margin);
            if (curRow != null) {
                curRow.addView(view, params);
            }
            view.setOnClickListener(clickListener);
            view.setTag(button);
            view.setId(i);
        }
        if (curRow != null && curRow.getChildCount() == 1) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            FrameLayout emptyLayout = new FrameLayout(mActivity);
            curRow.addView(emptyLayout, params);
        }
    }

    View.OnLayoutChangeListener layoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            LogUtils.printVars(top, bottom);
            if (bottom - top != oldBottom - oldTop) {
                if (mWithoutImageHeight == -1) {
                    mWithoutImageHeight = mView.getChildAt(0).getHeight();
                    for (ImageView imageView : mImageList) {
                        imageView.setImageURI((Uri)imageView.getTag());
                    }
                }
                for (ImageView imageView : mImageList) {
                    ViewGroup.LayoutParams params = imageView.getLayoutParams();
                    params.width = imageView.getWidth();
                    params.height = params.width * 320 / 420;
                    int h1 = bottom - top;
                    if (h1 - mWithoutImageHeight < params.height * 2) {
                        params.height = (h1 - mWithoutImageHeight) / 2;
                        params.width = params.height * 420 / 320;
                    }
                    imageView.setLayoutParams(params);
                }
            }
        }
    };

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCurView != null) {
                RadioButton button = (RadioButton)mCurView.getTag();
                button.setChecked(false);
                mCurView.setBackgroundResource(R.drawable.bg_answer_normal);
            }
            RadioButton button = (RadioButton)v.getTag();
            button.setChecked(true);
            v.setBackgroundResource(R.drawable.bg_answer_focus);
            mCurView = v;
            mActivity.setSelect(v.getId());
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mView == null) {
            mView = (ViewGroup)inflater.inflate(R.layout.fragment_question2, null);
            mQuestionText = (TextView)mView.findViewById(R.id.question);
            mAnswers = (LinearLayout)mView.findViewById(R.id.answers);
            mView.addOnLayoutChangeListener(layoutChangeListener);

            if (mQuestion != null) {
                loadQuestion();
            }
        }

        return mView;
    }
}
