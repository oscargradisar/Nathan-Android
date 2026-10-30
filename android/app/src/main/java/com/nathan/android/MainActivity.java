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
import android.webkit.WebResourceError;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.chaquo.python.Python;

public class MainActivity extends Activity {
    static MainActivity current;
    WebView view;
    public volatile String serverUrl;
    public volatile String startupError;
    private ValueCallback<Uri[]> fileChoice;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); current = this;
        FrameLayout root = new FrameLayout(this); root.setFitsSystemWindows(true); setContentView(root);
        TextView loading = new TextView(this); loading.setText("Starting Nathan on this phone…");
        loading.setTextColor(0xff3fd3ff); loading.setPadding(24, 80, 24, 24);
        root.setBackgroundColor(0xff030810); root.addView(loading);
        new Thread(() -> {
            try {
                KeyVault.init(getApplicationContext());
                serverUrl = Python.getInstance().getModule("android_runtime")
                    .callAttr("start", getFilesDir().getAbsolutePath()).toString();
                runOnUiThread(() -> {
                    view = new WebView(this);
                    view.getSettings().setJavaScriptEnabled(true);
                    view.getSettings().setDomStorageEnabled(true);
                    view.getSettings().setAllowFileAccess(false);
                    view.getSettings().setAllowContentAccess(true);
                    view.setWebChromeClient(new WebChromeClient() {
                        @Override public boolean onConsoleMessage(ConsoleMessage message) {
                            android.util.Log.i("NathanWebView", message.message()); return true;
                        }
                        @Override public boolean onShowFileChooser(WebView web, ValueCallback<Uri[]> callback,
                            FileChooserParams params) {
                            if (fileChoice != null) fileChoice.onReceiveValue(null);
                            fileChoice = callback;
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.setType("image/*"); intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            startActivityForResult(intent, 40); return true;
                        }
                    });
                    view.setWebViewClient(new WebViewClient() {
                        @Override public void onReceivedError(WebView web, WebResourceRequest request, WebResourceError error) {
                            android.util.Log.e("NathanWebView", request.getUrl() + ": " + error.getDescription());
                        }
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
        }, "Nathan startup").start();
    }
    public static boolean openExternal(String value) {
        Uri uri = Uri.parse(value);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) return false;
        current.runOnUiThread(() -> current.startActivity(new Intent(Intent.ACTION_VIEW, uri)));
        return true;
    }
    public static void finishApp() { current.runOnUiThread(() -> current.finish()); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 40 || fileChoice == null) return;
        Uri[] files = null;
        if (result == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = Math.min(4, data.getClipData().getItemCount()); files = new Uri[count];
                for (int i = 0; i < count; i++) files[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) files = new Uri[]{data.getData()};
        }
        fileChoice.onReceiveValue(files); fileChoice = null;
    }
    @Override public void onBackPressed() {
        if (view != null && view.canGoBack()) view.goBack();
        else new AlertDialog.Builder(this).setMessage("Close Nathan?")
            .setPositiveButton("Close", (d, which) -> finish()).setNegativeButton("Stay", null).show();
    }
    @Override public void onDestroy() {
        CameraLookup.cancel();
        if (fileChoice != null) fileChoice.onReceiveValue(null);
        if (view != null) view.destroy();
        if (isFinishing()) Python.getInstance().getModule("android_runtime").callAttr("stop");
        super.onDestroy();
    }
}
