package com.ohayou.japanese.model;

import android.text.TextUtils;

import com.ohayou.japanese.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Oxygen on 15/9/29.
 */
public class Question {
    public int tid;
    public int qid;
    public String conversation;
    public String question;
    public int type;
    public String[] answers;
    public int correct_ans;

    public static TextUtils.SimpleStringSplitter sSplitter = new TextUtils.SimpleStringSplitter(';');

    public static Question parse(JSONObject object) {
        Question question = new Question();
        try {
            question.tid = Integer.parseInt(object.getString("tid"));
            question.qid = Integer.parseInt(object.getString("qid"));
            question.conversation = object.getString("conversation");
            question.question = object.getString("question");
            question.type = Integer.parseInt(object.getString("atype"));
            question.answers = object.getString("answer").split(";");
            question.correct_ans = Integer.parseInt(object.getString("correct_ans")) - 1;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return question;
    }

    public static Question[] parseArray(JSONArray array) {
        Question[] questions = new Question[array.length()];
        try {
            for (int i = 0; i < array.length(); ++i) {
                questions[i] = parse(array.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return questions;
    }

    public String getAudioPath() {
        return String.format("%s/%d/audio/%d.mp3", Constants.TEXTBOOK_PATH, tid, qid);
    }

    public String getImagePath(int idx) {
        return String.format("%s/%d/img/%d_%d.jpg", Constants.TEXTBOOK_PATH, tid, qid, idx + 1);
    }

    public String getImagePath() {
        return String.format("%s/%d/img/%d.jpg", Constants.TEXTBOOK_PATH, tid, qid);
    }
}
