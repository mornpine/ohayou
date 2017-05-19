package com.ohayou.japanese;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.ohayou.japanese.db.DatabaseHelper;
import com.ohayou.japanese.utils.CommUtils;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.NetworkUtils;

/**
 * Created by Oxygen on 15/8/5.
 */
public class OhayouApplication extends Application {

    DatabaseHelper mHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        CommUtils.ensureDir(Constants.DOWNLOAD_CACHE_PATH);
        CommUtils.ensureDir(Constants.TEXTBOOK_PATH);

        NetworkUtils.init(this);
        mHelper = new DatabaseHelper(this);

        FacebookSdk.sdkInitialize(this);
    }

    public DatabaseHelper getDatabaseHelper() {
        return mHelper;
    }
}
