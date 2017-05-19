package com.ohayou.japanese.utils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;

import cz.msebera.android.httpclient.Header;

public class FileDownloader {
	private static AsyncHttpClient mAsyncHttpClient;

	static {
		mAsyncHttpClient = new AsyncHttpClient();
	}

	public interface DownloadListener {
		void onProgress(long bytesWritten, long totalSize, Object priData);
		void onResult(boolean success, File file, Object priData);
	}

	static class FileHandler extends FileAsyncHttpResponseHandler {
		String mPath;
		Object mPriData;
		DownloadListener mListener;

		public FileHandler(String path, DownloadListener listener, Object priData) {
			super(new File(path + ".tmp"));
			mPath = path;
			mListener = listener;
			mPriData = priData;
		}

		@Override
		public void onSuccess(int statusCode, Header[] headers, File file) {
			File newFile = new File(mPath);
			file.renameTo(newFile);
			mListener.onResult(true, newFile, mPriData);
		}
		@Override
		public void onFailure(int statusCode, Header[] arg1, Throwable arg2, File file) {
			if (file.exists()) {
				file.delete();
			}
			mListener.onResult(false, null, mPriData);
		}

		@Override
		public void onProgress(long bytesWritten, long totalSize) {
			super.onProgress(bytesWritten, totalSize);
			mListener.onProgress(bytesWritten, totalSize, mPriData);
		}
	};

	public static void addDownload(String url, String path, DownloadListener listener, Object priData) {
		FileHandler handler = new FileHandler(path, listener, priData);
		mAsyncHttpClient.get(url, handler);
	}
}
