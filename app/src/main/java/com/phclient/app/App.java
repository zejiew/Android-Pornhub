package com.phclient.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.webkit.CookieManager;
import android.webkit.WebView;

public class App extends Application {

    private static App instance;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences(Const.PREFS, MODE_PRIVATE);

        boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (debuggable) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // 只做全局 Cookie 接受；第三方 Cookie 需要在持有 WebView 实例时开启，
        // 见 MainActivity#initWebView（在 Application 里 new WebView 有兼容性风险）。
        CookieManager.getInstance().setAcceptCookie(true);
    }

    public static App get() {
        return instance;
    }

    public SharedPreferences prefs() {
        return prefs;
    }
}
