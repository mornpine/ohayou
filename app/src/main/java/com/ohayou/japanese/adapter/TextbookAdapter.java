package com.ohayou.japanese.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.ohayou.japanese.R;
import com.ohayou.japanese.db.DatabaseHelper;
import com.ohayou.japanese.model.Textbook;
import com.ohayou.japanese.model.UserInfo;
import com.ohayou.japanese.ui.TextbookActivity;
import com.ohayou.japanese.ui.UIHandler;
import com.ohayou.japanese.utils.CommUtils;
import com.ohayou.japanese.utils.Constants;
import com.ohayou.japanese.utils.FileDownloader;
import com.ohayou.japanese.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Oxygen on 15/8/9.
 */
public class TextbookAdapter extends BaseAdapter {
    Context mContext;
    ArrayList<Textbook> mData;
    DatabaseHelper mHelper;
    UIHandler mHandler;
    SparseArray<Textbook> mTidMap = new SparseArray<>();
    ArrayList<Textbook> mTextbooks = new ArrayList<>();
    SparseArray<ArrayList<Textbook>> mSortedTextbooks = new SparseArray<>();
    SparseArray<ViewHolder> mViewHolders = new SparseArray<>();
    int mCid;
    int mTid = -1;

    public static final int MSG_UPDATE_PROCESS = 1;
    public static final int MSG_DOWNLOAD_SUCCESS = 2;
    public static final int MSG_DOWNLOAD_FAIL = 3;

    public TextbookAdapter(Context context, DatabaseHelper helper) {
        mContext = context;
        mHelper = helper;
        mHandler = new UIHandler(msgHandler);
    }

    public void setCategory(int cid) {
        if (mCid != cid) {
            mCid = cid;
            if (cid == 0) {
                mData = mTextbooks;
            } else {
                mData = mSortedTextbooks.get(cid);
            }
            notifyDataSetChanged();
        }
    }

    public void parseTextbooks(JSONArray array) {
        mTextbooks.clear();
        mTidMap.clear();
        mSortedTextbooks.clear();
        try {
            for (int i = 0; i < array.length(); ++i) {
                JSONObject obj = array.getJSONObject(i);
                int tid = Integer.parseInt(obj.getString("tid"));
                Textbook book = mHelper.getTextbook(tid);
                if (book == null) {
                    book = new Textbook();
                    book.cid = Integer.parseInt(obj.getString("cid"));
                    book.tid = Integer.parseInt(obj.getString("tid"));
                    book.name = obj.getString("tname");
                    book.level = obj.getString("level");
                }
                mTextbooks.add(book);
                mTidMap.put(book.tid, book);
                ArrayList<Textbook> category = mSortedTextbooks.get(book.cid);
                if (category == null) {
                    category = new ArrayList<>();
                    mSortedTextbooks.put(book.cid, category);
                }
                category.add(book);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (mCid == 0) {
            mData = mTextbooks;
        } else {
            mData = mSortedTextbooks.get(mCid);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mData == null ? 0 : mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView mTitle;
        TextView mLevel;
        TextView mControl;
        TextView mPercent;
        TextView mPassed;
        TextView mFailed;
        TextView mMessage;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.item_textbook, null);
            holder = new ViewHolder();
            holder.mTitle = (TextView)convertView.findViewById(R.id.title);
            holder.mLevel = (TextView)convertView.findViewById(R.id.level);
            holder.mControl = (TextView)convertView.findViewById(R.id.control);
            holder.mPercent = (TextView)convertView.findViewById(R.id.percent);
            holder.mPassed = (TextView)convertView.findViewById(R.id.passed);
            holder.mFailed = (TextView)convertView.findViewById(R.id.failed);
            holder.mMessage = (TextView)convertView.findViewById(R.id.message);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        Textbook book = mData.get(position);
        mViewHolders.put(book.tid, holder);
        holder.mControl.setTag(book);
        updateItem(book.tid);

        return convertView;
    }

    public void updateLastItem() {
        if (mTid != -1) {
            updateItem(mTid);
        }
    }

    public void updateItem(int tid) {
        ViewHolder holder = mViewHolders.get(tid);
        if (holder != null) {
            Textbook book = (Textbook)holder.mControl.getTag();
            holder.mTitle.setText(book.name);
            holder.mLevel.setText(book.level);

            switch (book.getStatus()) {
                case Textbook.TEXTBOOK_STATUS_NEW:
                    setControlDownload(holder);
                    break;
                case Textbook.TEXTBOOK_STATUS_DOWNLOADING:
                    setControlDownloading(holder);
                    break;
                case Textbook.TEXTBOOK_STATUS_READY:
                    setControlStart(holder);
                    break;
                case Textbook.TEXTBOOK_STATUS_ABORT:
                    setControlContinue(holder);
                    break;
                case Textbook.TEXTBOOK_STATUS_FINISH:
                    setControlRedo(holder);
                    break;
            }
        }
    }

    void setVisibilityMode1(ViewHolder holder) {
        holder.mPercent.setVisibility(View.GONE);
        holder.mPassed.setVisibility(View.GONE);
        holder.mFailed.setVisibility(View.GONE);
        holder.mMessage.setVisibility(View.VISIBLE);
    }

    void setVisibilityMode2(ViewHolder holder) {
        Textbook book = (Textbook)holder.mControl.getTag();
        holder.mPercent.setVisibility(View.VISIBLE);
        holder.mPassed.setVisibility(View.VISIBLE);
        holder.mFailed.setVisibility(View.VISIBLE);
        holder.mMessage.setVisibility(View.VISIBLE);
        holder.mMessage.setText(R.string.completed);
        holder.mPassed.setText(Integer.toString(book.passed));
        holder.mFailed.setText(Integer.toString(book.failed));
        holder.mPassed.setEnabled(book.passed > 0);
        holder.mFailed.setEnabled(book.failed > 0);
        holder.mPercent.setText(String.format("%d%%", CommUtils.getPercent((book.passed + book.failed) * 100, book.status.length())));
    }

    void setVisibilityMode3(ViewHolder holder) {
        setVisibilityMode2(holder);
        holder.mMessage.setVisibility(View.GONE);
        holder.mPercent.setText(R.string.done);
    }

    void setControlDownload(ViewHolder holder) {
        setVisibilityMode1(holder);
        holder.mControl.setText(R.string.download);
        holder.mControl.setBackgroundResource(R.drawable.btn_download);
        holder.mControl.setTextColor(mContext.getResources().getColor(R.color.orange_dark));
        holder.mControl.setOnClickListener(downListener);
        holder.mMessage.setText(R.string.get_started);
    }

    public AbsListView.RecyclerListener getRecyclerListener () {
        return recyclerListener;
    }

    AbsListView.RecyclerListener recyclerListener = new AbsListView.RecyclerListener() {
        @Override
        public void onMovedToScrapHeap(View view) {
            ViewHolder holder = (ViewHolder)view.getTag();
            Textbook book = (Textbook)holder.mControl.getTag();
            mViewHolders.remove(book.tid);
        }
    };

    View.OnClickListener downListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Textbook book = (Textbook)v.getTag();
            String url = Constants.getTextbookUrl(book.tid);
            FileDownloader.addDownload(url, book.getTextbookZipPath(), downloadListener, book.tid);
            if (mHelper.getTextbook(book.tid) == null) {
                mHelper.addTextbook(book);
            }
            if (book.questions == null) {
                NetworkUtils.doJsonObjectRequest(Constants.URL_GET_QUESTIONS, NetworkUtils.genParamList("tid", Integer.toString(book.tid)), questionsListener);
            }
            setControlDownloading(mViewHolders.get(book.tid));
            MixpanelAPI mixpanel = MixpanelAPI.getInstance(mContext, Constants.MIXPANEL_KEY);
            try {
                JSONObject props = new JSONObject();
                props.put("email", UserInfo.sEmail);
                props.put("textbook", book.tid);
                mixpanel.track("download textbook", props);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    FileDownloader.DownloadListener downloadListener = new FileDownloader.DownloadListener() {
        @Override
        public void onProgress(long bytesWritten, long totalSize, Object priData) {
            mHandler.obtainMessage(MSG_UPDATE_PROCESS, (Integer)priData, (int)(bytesWritten * 100 / totalSize)).sendToTarget();
        }

        @Override
        public void onResult(boolean success, File file, Object priData) {
            mHandler.obtainMessage(success ? MSG_DOWNLOAD_SUCCESS : MSG_DOWNLOAD_FAIL,
                    (Integer)priData, 0, file).sendToTarget();
        }
    };

    void setControlDownloading(ViewHolder holder) {
        setVisibilityMode1(holder);
        holder.mControl.setText(R.string.downloading);
        holder.mControl.setClickable(false);
        holder.mMessage.setText(R.string.get_started);
    }

    void setControlStart(ViewHolder holder) {
        Textbook book = (Textbook)holder.mControl.getTag();
        setVisibilityMode1(holder);
        holder.mControl.setText(R.string.start);
        holder.mControl.setBackgroundResource(R.drawable.btn_start);
        holder.mControl.setTextColor(mContext.getResources().getColor(R.color.btn_start));
        holder.mControl.setOnClickListener(startListener);
        holder.mMessage.setText(R.string.get_started);
        if (mHelper.getTextbook(book.tid) == null) {
            mHelper.addTextbook(book);
        }
        if (book.questions == null) {
            NetworkUtils.doJsonObjectRequest(Constants.URL_GET_QUESTIONS, NetworkUtils.genParamList("tid", Integer.toString(book.tid)), questionsListener);
        }
    }

    void setControlContinue(ViewHolder holder) {
        setVisibilityMode2(holder);
        holder.mControl.setText(R.string.continue_);
        holder.mControl.setBackgroundResource(R.drawable.btn_start);
        holder.mControl.setTextColor(mContext.getResources().getColor(R.color.btn_start));
        holder.mControl.setOnClickListener(startListener);
    }

    void setControlRedo(ViewHolder holder) {
        setVisibilityMode3(holder);
        holder.mControl.setText(R.string.redo);
        holder.mControl.setBackgroundResource(R.drawable.btn_start);
        holder.mControl.setTextColor(mContext.getResources().getColor(R.color.btn_start));
        holder.mControl.setOnClickListener(redoListener);
    }

    View.OnClickListener redoListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Textbook book = (Textbook)v.getTag();
            mHelper.updateTextbook(book.tid, 0, 0, null);
            startListener.onClick(v);
        }
    };

    View.OnClickListener startListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Textbook book = (Textbook)v.getTag();
            Intent intent = new Intent(mContext, TextbookActivity.class);
            intent.putExtra(TextbookActivity.EXTRA_TID, book.tid);
            mTid = book.tid;
            mContext.startActivity(intent);
        }
    };

    NetworkUtils.JsonObjectResultListener questionsListener = new NetworkUtils.JsonObjectResultListener() {
        @Override
        public void onResult(JSONObject json) {
            try {
                if (json != null) {
                    int status = json.getInt("status");
                    if (status == NetworkUtils.RET_CODE_SUCCESS) {
                        JSONArray array = json.getJSONArray("data");
                        if (array.length() > 0) {
                            JSONObject obj = array.getJSONObject(0);
                            int tid = obj.getInt("tid");
                            for (int i = 0; i < mData.size(); ++i) {
                                Textbook textbook = mData.get(i);
                                if (textbook.tid == tid) {
                                    textbook.questions = array.toString();
                                    mHelper.updateTextbook(textbook.tid, textbook.questions);
                                }
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

    UIHandler.MsgHandler msgHandler = new UIHandler.MsgHandler() {
        @Override
        public void handleMessage(Message msg) {
            Textbook book;
            switch (msg.what) {
                case MSG_UPDATE_PROCESS:
                    book = mTidMap.get(msg.arg1);
                    if (book != null) {
                        ViewHolder vh = mViewHolders.get(book.tid);
                        if (vh != null) {
                            vh.mControl.setText(String.format("%d%%", msg.arg2));
                        }
                    }
                    break;
                case MSG_DOWNLOAD_SUCCESS:
                    File zipFile = (File)msg.obj;
                    if (CommUtils.unZip(zipFile, Constants.TEXTBOOK_PATH)) {
                        zipFile.delete();
                        book = mTidMap.get(msg.arg1);
                        if (book != null) {
                            ViewHolder vh = mViewHolders.get(book.tid);
                            if (vh != null) {
                                setControlStart(vh);
                            }
                        }
                    } else {
                        book = mTidMap.get(msg.arg1);
                        if (book != null) {
                            Toast.makeText(mContext, mContext.getString(R.string.unzip_textbook_fail, book.name), Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
                case MSG_DOWNLOAD_FAIL:
                    book = mTidMap.get(msg.arg1);
                    if (book != null) {
                        ViewHolder vh = mViewHolders.get(book.tid);
                        if (vh != null) {
                            setControlStart(vh);
                        }
                        Toast.makeText(mContext, mContext.getString(R.string.download_textbook_fail, book.name), Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    };
}
