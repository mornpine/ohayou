package com.ohayou.japanese.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.ohayou.japanese.R;

public class ResourcesActivity extends BaseActivity {
	private WebView mWebView;

	@Override
	public void onResume() {
		super.onResume();
		mWebView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mWebView.onPause();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_resources);

		mWebView = (WebView) findViewById(R.id.webview_webview);
		startWebView("http://ohayouapp.com/resources/");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWebView.destroy();
		finish();
	}

	@SuppressLint("SetJavaScriptEnabled")
	@SuppressWarnings("deprecation")
	private void startWebView(String url) {

		WebSettings webSettings = mWebView.getSettings();
		//webSettings.setJavaScriptEnabled(true);
		//webSettings.setPluginState(WebSettings.PluginState.ON);

		webSettings.setSupportZoom(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
		webSettings.setLoadWithOverviewMode(true);
		webSettings.setUseWideViewPort(true);

		mWebView.setWebViewClient(new WebViewClient() {
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				finish();
				Toast.makeText(ResourcesActivity.this, description,
						Toast.LENGTH_LONG).show();
			}

			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url != null && url.startsWith("market://")) {
					view.getContext().startActivity(
							new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					return true;
				} else {
					return false;
				}
			}

			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
			}

			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
			}
		});

		mWebView.loadUrl(url);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
			mWebView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
