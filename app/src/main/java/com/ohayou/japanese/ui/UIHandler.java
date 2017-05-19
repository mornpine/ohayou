package com.ohayou.japanese.ui;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by Oxygen on 15/8/7.
 */
public class UIHandler extends Handler {
    WeakReference<MsgHandler> mHandler;

    public interface MsgHandler {
        void handleMessage(Message msg);
    }

    public UIHandler(MsgHandler handler) {
        mHandler = new WeakReference<MsgHandler>(handler);
    }

    @Override
    public void handleMessage(Message msg) {
        MsgHandler handler = mHandler.get();
        if (handler != null) {
            handler.handleMessage(msg);
        }
    }
}
