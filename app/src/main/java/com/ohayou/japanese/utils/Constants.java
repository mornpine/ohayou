package com.ohayou.japanese.utils;

import android.os.Environment;

/**
 * Created by Oxygen on 15/8/6.
 */
public class Constants {
    public static final String URL_BASE = "http://ohayouapp.com/api/";

    public static final String URL_GET_ERROR_CODES = URL_BASE + "get_error_codes.php";
    public static final String URL_GET_SETTINGS_STORE = URL_BASE + "get_settings_store.php";
    public static final String URL_CHECK_UPGRADE = URL_BASE + "check_upgrade.php";//version=20150806
    public static final String URL_CHECK_EMAIL = URL_BASE + "check_user.php";
    public static final String URL_REGISTER = URL_BASE + "register_user.php";
    public static final String URL_LOGIN = URL_BASE + "login_user.php";
    public static final String URL_FEEDBACK = URL_BASE + "add_feedback.php";
    public static final String URL_LOGIN_FB = URL_BASE + "login_user_fb.php";
    public static final String URL_GET_TEXTBOOK_CATEGORY = URL_BASE + "get_textbook_category.php";
    public static final String URL_GET_TEXTBOOKS = URL_BASE + "get_textbooks.php";
    public static final String URL_GET_QUESTIONS = URL_BASE + "get_questions.php";
    public static final String URL_ADD_POINTS = URL_BASE + "add_points.php";
    public static final String URL_DEL_POINTS = URL_BASE + "del_points.php";
    public static final String URL_UPDATE_USER = URL_BASE + "update_user.php";
    public static final String URL_CHECK_PROMO_CODE = URL_BASE + "check_promo_code.php";
    public static final String URL_GT = URL_BASE + "gt.php";
    public static final String URL_UPDATE_GCM = URL_BASE + "update_gcm.php";

    public static final String DOWNLOAD_CACHE_PATH = Environment.getExternalStorageDirectory().getAbsoluteFile() + "/.ohayou";
    public static final String TEXTBOOK_PATH = DOWNLOAD_CACHE_PATH + "/textbooks";

    public static String getTextbookUrl(int tid) {
        return String.format("http://ohayouapp.com/textbooks/%d.zip", tid);
    }

    public static final String MIXPANEL_KEY = "07b818c206c0dfe685efa4e7fabceaea";
}
