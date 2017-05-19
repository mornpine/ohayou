package com.ohayou.japanese.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;

import com.ohayou.japanese.R;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by Oxygen on 15/10/4.
 */
public class Question3QuestionFragment extends BaseQuestionFragment {
    LinearLayout mAnswers;
    ImageView mImage;
    TextView mPrompt;
    RadioButton mCurButton = null;
    PhotoViewAttacher mAttacher;
    PopupWindow mPopupWindow;
    int mWithoutImageHeight = -1;
    int mImageWidth = -1;
    int mImageHeight = -1;

    @Override
    protected void loadQuestion() {
        super.loadQuestion();
        mAnswers.removeAllViews();
        LinearLayout curRow = null;
        mWithoutImageHeight = -1;
        for (int i = 0; i < mQuestion.answers.length; ++i) {
            RadioButton button = (RadioButton)mActivity.getLayoutInflater().inflate(R.layout.item_question1, null);
            if (i % 2 == 0) {
                curRow = new LinearLayout(mActivity);
                curRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mAnswers.addView(curRow, params);
            }
            button.setText(mQuestion.answers[i]);
            button.setId(i);
            button.setChecked(false);
            button.setOnCheckedChangeListener(onCheckedChangeListener);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            int margin = mActivity.getResources().getDimensionPixelSize(R.dimen.question1_answer_margin);
            params.setMargins(margin, margin, margin, margin);
            if (curRow != null) {
                curRow.addView(button, params);
            }
        }
        if (curRow != null && curRow.getChildCount() == 1) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            FrameLayout emptyLayout = new FrameLayout(mActivity);
            curRow.addView(emptyLayout, params);
        }
    }

    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (mCurButton != null) {
                    mCurButton.setChecked(false);
                }
                mCurButton = (RadioButton)buttonView;
                mActivity.setSelect(buttonView.getId());
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mView == null) {
            mView = (ViewGroup)inflater.inflate(R.layout.fragment_question3, null);
            mQuestionText = (TextView)mView.findViewById(R.id.question);
            mAnswers = (LinearLayout)mView.findViewById(R.id.answers);
            mImage = (ImageView)mView.findViewById(R.id.image);
            mPrompt = (TextView)mView.findViewById(R.id.prompt);
            mView.findViewById(R.id.image_layout).setOnClickListener(onImageClick);
            mView.addOnLayoutChangeListener(layoutChangeListener);

            if (mQuestion != null) {
                loadQuestion();
            }
        }

        return mView;
    }

    View.OnLayoutChangeListener layoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (bottom - top != oldBottom - oldTop) {
                if (mWithoutImageHeight == -1) {
                    mWithoutImageHeight = mView.getChildAt(0).getHeight();
                    Bitmap bmp = BitmapFactory.decodeFile(mQuestion.getImagePath());
                    mImageWidth = bmp.getWidth();
                    mImageHeight = bmp.getHeight();
                    mImage.setImageBitmap(bmp);
                }
                ViewGroup.LayoutParams params = mImage.getLayoutParams();
                params.width = mImage.getWidth();
                params.height = params.width * mImageHeight / mImageWidth;
                int h1 = bottom - top;
                if (h1 - mWithoutImageHeight < params.height) {
                    params.height = h1 - mWithoutImageHeight;
                }
                mImage.setLayoutParams(params);
            }
        }
    };

    View.OnClickListener onImageClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View view = View.inflate(getActivity(), R.layout.popupwindow_image, null);
            ImageView imageView = (ImageView)view.findViewById(R.id.image);
            imageView.setImageBitmap(BitmapFactory.decodeFile(mQuestion.getImagePath()));
            mAttacher = new PhotoViewAttacher(imageView);
            mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {
                    mPopupWindow.dismiss();
                }
            });
            mPopupWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopupWindow.setOutsideTouchable(true);

            mPopupWindow.showAtLocation(mView, Gravity.CENTER, 0, 0);
        }
    };
}
