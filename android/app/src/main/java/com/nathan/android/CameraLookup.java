package com.nathan.android;

import android.graphics.Bitmap;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Only reads the public search result; this WebView has no key vault or device bridge. */
public final class CameraLookup {
    private static WebView browser;
    private static CountDownLatch pending;
    private static String nonce;
    private static String result;

    public static synchronized String search(String query) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        String token = UUID.randomUUID().toString();
        pending = done; nonce = token; result = "{\"error\":\"Camera search timed out\"}";
        MainActivity.current.runOnUiThread(() -> {
            browser = new WebView(MainActivity.current);
            browser.getSettings().setJavaScriptEnabled(true);
            browser.getSettings().setDomStorageEnabled(true);
            browser.getSettings().setAllowFileAccess(false);
            browser.getSettings().setAllowContentAccess(false);
            browser.addJavascriptInterface(new Object() {
                @JavascriptInterface public void receive(String receivedToken, String json) {
                    if (!token.equals(receivedToken) || json.length() > 2_000_000) return;
                    result = json; done.countDown();
                }
            }, "NathanCameraResult");
            browser.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return !"opencctv.org".equals(request.getUrl().getHost());
                }
                @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    String path = request.getUrl().getPath();
                    if (path != null && (path.contains("/api/cameras/markers") ||
                        path.contains("/api/cameras/marker-names") || path.contains("/api/cameras/feed-pool") ||
                        path.contains("/api/cameras/batch") || path.contains("/api/feed/"))) {
                        return new WebResourceResponse("application/json", "UTF-8", new ByteArrayInputStream("[]".getBytes()));
                    }
                    return null;
                }
                @Override public void onPageFinished(WebView view, String url) {
                    if (!url.startsWith("https://opencctv.org/")) return;
                    String script = "(() => { if(window.nathanSearch)return;window.nathanSearch=true;" +
                        "const original=window.fetch;window.fetch=async(...args)=>{" +
                        "const response=await original(...args);const u=String(args[0]);" +
                        "if(u.includes('/api/cameras/list?')&&u.includes('&q='))" +
                        "response.clone().json().then(j=>NathanCameraResult.receive(" + JSONObject.quote(token) + ",JSON.stringify(j)));return response;};" +
                        "let tries=0;const timer=setInterval(()=>{if(++tries>50){clearInterval(timer);return;}" +
                        "const input=document.querySelector('input[placeholder=\"Search cameras...\"]');" +
                        "if(!input){const list=[...document.querySelectorAll('button')].find(b=>b.textContent==='List');if(list)list.click();return;}" +
                        "clearInterval(timer);Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input," + JSONObject.quote(query) + ");" +
                        "input.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "setTimeout(()=>input.closest('form').requestSubmit(),100);},400);})();";
                    view.evaluateJavascript(script, null);
                }
            });
            browser.loadUrl("https://opencctv.org/");
        });
        try {
            done.await(35, TimeUnit.SECONDS);
            return result;
        } finally {
            MainActivity.current.runOnUiThread(CameraLookup::cancel);
        }
    }
    static void cancel() {
        if (browser != null) { browser.stopLoading(); browser.removeJavascriptInterface("NathanCameraResult"); browser.destroy(); browser = null; }
        if (pending != null) { pending.countDown(); pending = null; }
    }
}
