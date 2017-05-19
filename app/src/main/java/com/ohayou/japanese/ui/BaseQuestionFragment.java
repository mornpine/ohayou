package com.ohayou.japanese.ui;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ohayou.japanese.model.Question;

/**
 * Created by Oxygen on 15/10/4.
 */
public class BaseQuestionFragment extends Fragment {

    protected ViewGroup mView;
    protected Question mQuestion;
    protected int mIndex;
    protected TextbookActivity mActivity;
    protected TextView mQuestionText;

    public void setQuestion(Question question, int index) {
        mQuestion = question;
        mIndex = index;
        if (mView != null) {
            loadQuestion();
        }
    }

    protected void loadQuestion() {
        mQuestionText.setText(String.format("%d. %s", mIndex + 1, mQuestion.question));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (TextbookActivity)activity;
    }
}

