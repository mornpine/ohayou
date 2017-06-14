package com.ohayou.japanese.ui;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.appodeal.ads.Appodeal;
// import com.facebook.ads.AdSize;
// import com.facebook.ads.AdView;
import com.ohayou.japanese.OhayouApplication;
import com.ohayou.japanese.R;
import com.ohayou.japanese.adapter.TextbookAdapter;
import com.ohayou.japanese.db.DatabaseHelper;
import com.ohayou.japanese.model.UserInfo;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Oxygen on 15/8/9.
 */
public class HomeActivity extends BaseActivity implements View.OnClickListener {

    DrawerLayout mDrawerLayout;
    RelativeLayout mNavi;
    LinearLayout mTabs;
    SwipeRefreshLayout mRefreshLayout;
    ListView mList;
    TextbookAdapter mAdapter;
    HorizontalScrollView mCategoryBar;
    TextView mUser, mPoints, mLeftPoints;
    CheckBox mSelectCategory;

    // RelativeLayout adViewContainer;
    // private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh_layout);
        mCategoryBar = (HorizontalScrollView)findViewById(R.id.category_bar);
        mNavi = (RelativeLayout) findViewById(R.id.navi);
        mList = (ListView)findViewById(R.id.list);
        mTabs = (LinearLayout)findViewById(R.id.tabs);
        mUser = (TextView)findViewById(R.id.user);
        mPoints = (TextView)findViewById(R.id.store_points);
        mLeftPoints = (TextView)findViewById(R.id.points);
        mPoints.setOnClickListener(this);
        findViewById(R.id.menu).setOnClickListener(this);
        findViewById(R.id.feedback).setOnClickListener(this);
        findViewById(R.id.settings).setOnClickListener(this);
        findViewById(R.id.store).setOnClickListener(this);
        findViewById(R.id.resource).setOnClickListener(this);

        DatabaseHelper helper = ((OhayouApplication)getApplication()).getDatabaseHelper();
        mAdapter = new TextbookAdapter(this, helper);
        mList.setAdapter(mAdapter);
        mList.setRecyclerListener(mAdapter.getRecyclerListener());

        mUser.setText(UserInfo.sEmail);

        mRefreshLayout.setOnRefreshListener(refreshListener);
        NetworkUtils.doJsonObjectRequest(Constants.URL_GET_TEXTBOOK_CATEGORY, null, categoryListener);
        NetworkUtils.doJsonObjectRequest(Constants.URL_GET_TEXTBOOKS, null, textbooksListener);

        // Appodeal

        String appKey = "7fadc00038f2cd73e1190dd2940ae838d45d6b061c7049d4";
        Appodeal.setBannerViewId(R.id.appodealBannerView);
        Appodeal.initialize(this, appKey, Appodeal.INTERSTITIAL | Appodeal.NON_SKIPPABLE_VIDEO | Appodeal.BANNER | Appodeal.REWARDED_VIDEO);
        Appodeal.cache(this, Appodeal.REWARDED_VIDEO);
        // Appodeal.setRewardedVideoCallbacks(new AppodealRewardedVideoCallbacks(this));

        if (!UserInfo.sRemovedAds) {
            /*
            adViewContainer = (RelativeLayout) findViewById(R.id.adViewContainer);

            adView = new AdView(this, "1011245595574360_1071185489580370", AdSize.BANNER_320_50);
            adViewContainer.addView(adView);
            adView.loadAd();
            */
            Appodeal.show(this, Appodeal.BANNER_VIEW);
        }


        UserInfo.addObserver(this);



    }

    NetworkUtils.JsonObjectResultListener categoryListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        JSONArray array = json.getJSONArray("data");
                        int code = array.toString().hashCode();
                        if (mTabs.getTag() != null &&  (Integer)mTabs.getTag() == code) {
                            return;
                        }
                        mTabs.setTag(code);
                        int selCid = 0;
                        if (mSelectCategory != null) {
                            selCid = (Integer)mSelectCategory.getTag();
                        }
                        mTabs.removeAllViews();
                        mSelectCategory = null;
                        for (int i = 0; i < array.length(); ++i) {
                            JSONObject obj = array.getJSONObject(i);
                            CheckBox item = (CheckBox)getLayoutInflater().inflate(R.layout.item_home_tab, null);
                            int cid = obj.getInt("cid");
                            item.setText(obj.getString("cname"));
                            item.setTag(cid);
                            item.setOnCheckedChangeListener(checkedChangeListener);
                            mTabs.addView(item);
                            if (selCid == cid) {
                                item.setChecked(true);
                            }
                        }
                    } else {
                        NetworkUtils.showError(mContext, status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (mSelectCategory != null) {
                    mSelectCategory.setChecked(false);
                    mSelectCategory.setClickable(true);
                }
                mSelectCategory = (CheckBox)buttonView;
                mSelectCategory.setClickable(false);
                int cid = (Integer)buttonView.getTag();
                mAdapter.setCategory(cid);
                Rect rect = new Rect();
                buttonView.getDrawingRect(rect);
                mCategoryBar.offsetDescendantRectToMyCoords(buttonView, rect);
                int scrollX = mCategoryBar.getScrollX();
                if (rect.left < scrollX || rect.right > scrollX + mCategoryBar.getWidth()) {
                    mCategoryBar.smoothScrollTo(rect.left - rect.width(), mCategoryBar.getScrollY());
                }
            }
        }
    };

    NetworkUtils.JsonObjectResultListener textbooksListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            mRefreshLayout.setRefreshing(false);
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        JSONArray array = json.getJSONArray("data");
                        int code = array.toString().hashCode();
                        if (mList.getTag() != null &&  (Integer)mList.getTag() == code) {
                            return;
                        }
                        mList.setTag(code);
                        mAdapter.parseTextbooks(array);
                    } else {
                        NetworkUtils.showError(mContext, status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            NetworkUtils.doJsonObjectRequest(Constants.URL_GET_TEXTBOOK_CATEGORY, null, categoryListener);
            NetworkUtils.doJsonObjectRequest(Constants.URL_GET_TEXTBOOKS, null, textbooksListener);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menu:
                mDrawerLayout.openDrawer(mNavi);
                break;
            case R.id.feedback: {
                    Intent intent = new Intent(mContext, FeedbackActivity.class);
                    startActivity(intent);
                    mDrawerLayout.closeDrawers();
                }
                break;
            case R.id.settings: {
                Intent intent = new Intent(mContext, SettingsActivity.class);
                startActivity(intent);
                mDrawerLayout.closeDrawers();
            }
            break;
            case R.id.store_points:
            case R.id.store: {
                Intent intent = new Intent(mContext, StoreActivity.class);
                startActivity(intent);
                mDrawerLayout.closeDrawers();
            }
            break;
            case R.id.resource: {
                Intent intent = new Intent(mContext, ResourcesActivity.class);
                startActivity(intent);
                mDrawerLayout.closeDrawers();
            }

                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
        Appodeal.onResume(this, Appodeal.BANNER);
        /*
        if (adViewContainer != null && UserInfo.sRemovedAds) {
            adViewContainer.setVisibility(View.GONE);
            adView.destroy();
        }
        */
    }

    @Override
    protected void onDestroy() {
        /*
        if (adView != null) {
            adView.destroy();
        }
        */
        UserInfo.removeObserver(this);
        super.onDestroy();
    }

    @Override
    public void onUpdatePoints() {
        mPoints.setText(String.valueOf(UserInfo.sPoints));
        mLeftPoints.setText(String.valueOf(UserInfo.sPoints));
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavi)) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}
