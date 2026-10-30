package com.nathan.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.chaquo.python.Python;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    static MainActivity current;
    WebView view;
    public volatile String serverUrl;
    public volatile String startupError;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); current = this;
        FrameLayout root = new FrameLayout(this); setContentView(root);
        TextView loading = new TextView(this); loading.setText("Starting Nathan on this phone…");
        loading.setTextColor(0xff3fd3ff); loading.setPadding(24, 80, 24, 24);
        root.setBackgroundColor(0xff030810); root.addView(loading);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                KeyVault.init(getApplicationContext());
                serverUrl = Python.getInstance().getModule("android_runtime")
                    .callAttr("start", getFilesDir().getAbsolutePath()).toString();
                runOnUiThread(() -> {
                    view = new WebView(this);
                    view.getSettings().setJavaScriptEnabled(true);
                    view.getSettings().setDomStorageEnabled(true);
                    view.getSettings().setAllowFileAccess(false);
                    view.getSettings().setAllowContentAccess(false);
                    view.setWebChromeClient(new WebChromeClient());
                    view.setWebViewClient(new WebViewClient() {
                        @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                            if (!request.isForMainFrame()) return false;
                            Uri url = request.getUrl();
                            if ("127.0.0.1".equals(url.getHost()) &&
                                url.getPort() == Uri.parse(serverUrl).getPort()) return false;
                            openExternal(url.toString()); return true;
                        }
                    });
                    root.removeAllViews(); root.addView(view); view.loadUrl(serverUrl);
                });
            } catch (Exception error) {
                startupError = error.toString();
                android.util.Log.e("Nathan", "Startup failed", error);
                runOnUiThread(() -> loading.setText("Nathan could not start:\n" + startupError));
            }
        });
    }
    public static boolean openExternal(String value) {
        Uri uri = Uri.parse(value);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) return false;
        current.runOnUiThread(() -> current.startActivity(new Intent(Intent.ACTION_VIEW, uri)));
        return true;
    }
    @Override public void onBackPressed() {
        if (view != null && view.canGoBack()) view.goBack();
        else new AlertDialog.Builder(this).setMessage("Close Nathan?")
            .setPositiveButton("Close", (d, which) -> finish()).setNegativeButton("Stay", null).show();
    }
    @Override public void onDestroy() {
        CameraLookup.cancel();
        if (view != null) view.destroy();
        if (isFinishing()) Python.getInstance().getModule("android_runtime").callAttr("stop");
        super.onDestroy();
    }
}
