package com.ohayou.japanese.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.ohayou.japanese.R;

/**
 * Created by Oxygen on 15/10/4.
 */
public class Question1QuestionFragment extends BaseQuestionFragment {
    RadioGroup mAnswers;

    @Override
    protected void loadQuestion() {
        super.loadQuestion();
        mAnswers.removeAllViews();
        mAnswers.clearCheck();
        for (int i = 0; i < mQuestion.answers.length; ++i) {
            RadioButton button = (RadioButton)mActivity.getLayoutInflater().inflate(R.layout.item_question1, null);
            button.setText(mQuestion.answers[i]);
            button.setChecked(false);
            button.setId(i);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = mActivity.getResources().getDimensionPixelSize(R.dimen.question1_answer_margin);
            params.setMargins(margin, margin, margin, margin);
            mAnswers.addView(button, params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mView == null) {
            mView = (ViewGroup)inflater.inflate(R.layout.fragment_question1, null);
            mQuestionText = (TextView)mView.findViewById(R.id.question);
            mAnswers = (RadioGroup)mView.findViewById(R.id.answers);

            if (mQuestion != null) {
                loadQuestion();
            }
            mAnswers.setOnCheckedChangeListener(onCheckedChangeListener);
        }

        return mView;
    }

    RadioGroup.OnCheckedChangeListener onCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            mActivity.setSelect(checkedId);
        }
    };
}
