package com.mora.gpumon;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private WebView wv;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        wv = new WebView(this);
        wv.setBackgroundColor(Color.parseColor("#0b0b0d"));
        setContentView(wv);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        wv.setWebViewClient(new WebViewClient());
        wv.addJavascriptInterface(new Bridge(), "Android");
        wv.loadUrl("file:///android_asset/index.html");
    }

    /** Jembatan JS -> shell root. Dipanggil dari index.html: Android.exec(cmd, callbackId). */
    private class Bridge {
        @JavascriptInterface
        public void exec(final String cmd, final String cb) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int code = -1;
                    StringBuilder out = new StringBuilder();
                    try {
                        ProcessBuilder pb = new ProcessBuilder("su", "-c", cmd);
                        pb.redirectErrorStream(true);
                        Process p = pb.start();
                        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String line;
                        while ((line = r.readLine()) != null) out.append(line).append('\n');
                        code = p.waitFor();
                    } catch (Exception e) {
                        out.append("ERR ").append(e.getMessage());
                    }
                    final String js = "window['" + cb + "']&&window['" + cb + "'](" + code + ","
                            + org.json.JSONObject.quote(out.toString()) + ")";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { wv.evaluateJavascript(js, null); }
                    });
                }
            }).start();
        }

        @JavascriptInterface
        public void toast(final String m) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() { Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show(); }
            });
        }
    }
}
