package com.phclient.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.util.Locale;

/**
 * 主界面：承载站点的 WebView 容器。
 *
 * 关键点：
 * 1) Cookie 持久化 —— 登录态的唯一依赖，必须开启并启用第三方 Cookie
 * 2) 桌面 UA —— 站点移动版限制较多，桌面 UA 能力最完整
 * 3) 全屏视频 —— onShowCustomView 接管 <video> 全屏
 * 4) 文件选择 —— 支持上传头像等（onShowFileChooser）
 * 5) 下载 —— DownloadListener 交给系统 DownloadManager，带 Cookie 才能下私有资源
 */
public class MainActivity extends Activity {

    private static final int REQ_FILE_CHOOSER = 1001;

    private WebView web;
    private ProgressBar progress;
    private EditText urlBar;
    private View errorView;
    private TextView errorText;

    private ValueCallback<Uri[]> filePathCallback;

    // 全屏视频
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout fullscreenContainer;
    private int originalOrientation;

    private long lastBackPress = 0L;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!App.get().prefs().getBoolean(Const.KEY_AGE_OK, false)) {
            showAgeGate();
            return;
        }

        initUi();
        initWebView();

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(Const.HOME);
        }
    }

    /** 年龄确认门槛（上架合规要求，也是对自己的免责声明） */
    private void showAgeGate() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.age_title)
                .setMessage(R.string.age_message)
                .setCancelable(false)
                .setPositiveButton(R.string.age_confirm, (d, w) -> {
                    App.get().prefs().edit().putBoolean(Const.KEY_AGE_OK, true).apply();
                    recreate();
                })
                .setNegativeButton(R.string.age_exit, (d, w) -> finish())
                .show();
    }

    private void initUi() {
        setContentView(R.layout.activity_main);

        web = findViewById(R.id.web);
        progress = findViewById(R.id.progress);
        urlBar = findViewById(R.id.urlBar);
        errorView = findViewById(R.id.errorView);
        errorText = findViewById(R.id.errorText);

        ImageButton go = findViewById(R.id.btnGo);
        go.setOnClickListener(v -> doSearchOrNavigate());

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            doSearchOrNavigate();
            return true;
        });

        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            web.reload();
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            highlightNav(R.id.navHome);
            web.loadUrl(Const.HOME);
        });
        findViewById(R.id.navSearch).setOnClickListener(v -> {
            highlightNav(R.id.navSearch);
            focusSearchBar();
        });
        findViewById(R.id.navFav).setOnClickListener(v -> {
            highlightNav(R.id.navFav);
            openFavorites();
        });
        findViewById(R.id.navDownload).setOnClickListener(v -> {
            startActivity(new Intent(this, DownloadActivity.class));
        });
        findViewById(R.id.navMore).setOnClickListener(v -> showMoreMenu());

        // 输入框聚焦时不要自动弹出全键盘挡住内容，交给用户点
        urlBar.clearFocus();
    }

    /**
     * 打开收藏页。
     *
     * 站点收藏页路径必须带用户名：/users/<name>/videos/favorites。
     * 不知道用户名时，先抓当前页面解析；仍拿不到就引导去登录页，
     * 而不是像以前那样跳到 /users/favorites（会被当成用户名 favorites）。
     */
    private void openFavorites() {
        String user = Account.cached();
        if (user != null) {
            web.loadUrl(String.format(Locale.US, Const.USER_FAVORITES_FMT, user));
            return;
        }

        if (!Account.likelyLoggedIn()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.login_needed)
                    .setMessage(R.string.login_needed_msg)
                    .setPositiveButton(R.string.menu_login,
                            (d, w) -> web.loadUrl(Const.LOGIN))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        // 已登录但还没拿到用户名：嗅探当前页面
        Toast.makeText(this, R.string.resolving_account, Toast.LENGTH_SHORT).show();
        web.evaluateJavascript(
                "(function(){try{"
                        + "var a=document.querySelector('a[href*=\"/users/\"][href*=\"/videos\"]');"
                        + "if(a){var m=a.getAttribute('href').match(/\\\\/users\\\\/([^\\\\/?#]+)/);if(m)return m[1];}"
                        + "var u=document.querySelector('.username, #username, [data-username]');"
                        + "if(u){return (u.getAttribute('data-username')||u.textContent||'').trim();}"
                        + "return '';}catch(e){return '';}})()",
                value -> {
                    String name = cleanJsString(value);
                    if (name != null && !name.isEmpty()) {
                        Account.remember(name);
                        web.loadUrl(String.format(Locale.US, Const.USER_FAVORITES_FMT, name));
                    } else {
                        Toast.makeText(this, R.string.need_username, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** 去掉 evaluateJavascript 返回的 JSON 引号与转义 */
    private String cleanJsString(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        s = s.replace("\\\"", "\"").replace("\\\\", "\\").trim();
        if (s.equals("null") || s.isEmpty()) return null;
        return s;
    }

    /** 页面加载完顺手嗅探用户名，成功就缓存；失败静默，不打扰用户 */
    private void sniffUsernameQuietly(String url) {
        if (url == null || Account.cached() != null) {
            return;
        }
        try {
            web.evaluateJavascript(
                    "(function(){try{"
                            + "var a=document.querySelector('a[href*=\"/users/\"][href*=\"/videos\"]');"
                            + "if(a){var m=a.getAttribute('href').match(/\\\\/users\\\\/([^\\\\/?#]+)/);if(m)return m[1];}"
                            + "return '';}catch(e){return '';}})()",
                    value -> {
                        String name = cleanJsString(value);
                        if (name != null && !name.isEmpty()) {
                            Account.remember(name);
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    /** 聚焦搜索框并主动弹出软键盘（以前只 requestFocus，键盘不弹，看着像点了没反应） */
    private void focusSearchBar() {
        urlBar.setFocusableInTouchMode(true);
        urlBar.requestFocus();
        urlBar.postDelayed(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(urlBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 120);
    }

    private void highlightNav(int id) {
        int[] ids = {R.id.navHome, R.id.navSearch, R.id.navFav, R.id.navDownload, R.id.navMore};
        for (int i : ids) {
            LinearLayout item = findViewById(i);
            if (item == null) continue;
            boolean on = (i == id);
            ((android.widget.ImageView) item.getChildAt(0))
                    .setColorFilter(getColor(on ? R.color.accent : R.color.text_dim));
            ((TextView) item.getChildAt(1))
                    .setTextColor(getColor(on ? R.color.accent : R.color.text_dim));
        }
    }

    private void doSearchOrNavigate() {
        String input = urlBar.getText().toString().trim();
        if (input.isEmpty()) {
            // 以前这里直接 return，用户点了没任何反馈，看起来像按钮坏了
            Toast.makeText(this, R.string.search_empty, Toast.LENGTH_SHORT).show();
            focusSearchBar();
            return;
        }
        if (input.startsWith("http://") || input.startsWith("https://")) {
            web.loadUrl(input);
        } else {
            web.loadUrl(Const.SEARCH + Uri.encode(input));
            highlightNav(R.id.navSearch);
        }
        urlBar.clearFocus();
        hideKeyboard();
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                        getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);   // 允许自动播放
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(Const.UA_DESKTOP);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setGeolocationEnabled(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(false);
        }

        // Cookie
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            private boolean handleUrl(String url) {
                if (url == null) return false;
                // 站外链接交给系统浏览器；站内继续在应用内打开
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    String host = Uri.parse(url).getHost();
                    if (host != null && (host.contains("pornhub.com") || host.contains("phncdn.com"))) {
                        return false;
                    }
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Throwable ignored) {
                    }
                    return true;
                }
                // intent:// 等特殊协议
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Throwable ignored) {
                }
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (AdBlocker.isBlocked(url)) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream(new byte[0]));
                }
                return null;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progress.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                if (url != null && !url.contains("login")) {
                    urlBar.setText("");
                    urlBar.setHint(shortUrl(url));
                }
                CookieManager.getInstance().flush();
                // 顺手识别登录用户名，供收藏页/历史页拼 URL 使用
                sniffUsernameQuietly(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    progress.setVisibility(View.GONE);
                    errorView.setVisibility(View.VISIBLE);
                    errorText.setText(getString(R.string.net_error) + "\n\n" + request.getUrl());
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            /** 全屏播放（<video> 请求全屏时进入这里） */
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;

                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                fullscreenContainer = new FrameLayout(MainActivity.this);
                fullscreenContainer.setBackgroundColor(Color.BLACK);
                fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                decor.addView(fullscreenContainer, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

                web.setVisibility(View.GONE);
                setNavVisible(false);
                goFullscreen(true);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                if (fullscreenContainer != null) {
                    decor.removeView(fullscreenContainer);
                    fullscreenContainer = null;
                }
                customView.setVisibility(View.GONE);
                customView = null;
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
                web.setVisibility(View.VISIBLE);
                setNavVisible(true);
                goFullscreen(false);
            }

            /** 文件选择（上传视频/头像等） */
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, REQ_FILE_CHOOSER);
                } catch (Throwable t) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        // 下载：带上 Cookie，否则私有资源会 403
        web.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimeType, long contentLength) {
                enqueueDownload(url, contentDisposition, mimeType);
            }
        });
    }

    private void setNavVisible(boolean visible) {
        View navParent = (View) findViewById(R.id.navHome).getParent();
        if (navParent != null) {
            navParent.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** 退出全屏播放：清理容器、回调与系统栏，恢复竖屏 */
    private void exitFullscreen() {
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        if (fullscreenContainer != null) {
            decor.removeView(fullscreenContainer);
            fullscreenContainer = null;
        }
        if (customView != null) {
            customView.setVisibility(View.GONE);
            customView = null;
        }
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        web.setVisibility(View.VISIBLE);
        setNavVisible(true);
        goFullscreen(false);
    }

    private void goFullscreen(boolean on) {
        Window w = getWindow();
        if (on) {
            originalOrientation = getRequestedOrientation();
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private void showMoreMenu() {
        String[] items = {
                getString(R.string.menu_login),
                getString(R.string.menu_reload),
                getString(R.string.menu_browser),
                getString(R.string.menu_clear),
                getString(R.string.menu_about)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.nav_more)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            web.loadUrl(Const.LOGIN);
                            break;
                        case 1:
                            web.reload();
                            break;
                        case 2:
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(web.getUrl())));
                            } catch (Throwable ignored) {
                            }
                            break;
                        case 3:
                            clearAll();
                            break;
                        case 4:
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.menu_about)
                                    .setMessage(R.string.about_text)
                                    .setPositiveButton("OK", null)
                                    .show();
                            break;
                        default:
                            break;
                    }
                })
                .show();
    }

    private void clearAll() {
        CookieManager.getInstance().removeAllCookies(v -> {
            CookieManager.getInstance().flush();
            web.clearCache(true);
            web.clearHistory();
            web.clearFormData();
            Account.forget();   // 连同缓存的用户名一起清掉，避免登出后还跳旧账号的收藏页
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.clear_done, Toast.LENGTH_SHORT).show();
                web.loadUrl(Const.HOME);
            });
        });
    }

    /** 用系统 DownloadManager 下载，自动携带 Cookie */
    private void enqueueDownload(String url, String contentDisposition, String mimeType) {
        try {
            android.app.DownloadManager dm =
                    (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm == null) {
                Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = android.webkit.URLUtil.guessFileName(
                    url, contentDisposition, mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);

            android.app.DownloadManager.Request req =
                    new android.app.DownloadManager.Request(Uri.parse(url));
            req.addRequestHeader("User-Agent", Const.UA_DESKTOP);
            if (cookies != null) {
                req.addRequestHeader("Cookie", cookies);
            }
            req.addRequestHeader("Referer", web.getUrl() == null ? Const.HOME : web.getUrl());
            req.setMimeType(mimeType);
            req.setTitle(fileName);
            req.setDescription(getString(R.string.app_name));
            req.setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(false);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,
                    "ZHUB/" + fileName);

            long id = dm.enqueue(req);
            DownloadStore.get(this).add(new DownloadStore.Item(
                    id, fileName, url, System.currentTimeMillis()));

            Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private String shortUrl(String url) {
        try {
            Uri u = Uri.parse(url);
            String path = u.getPath();
            if (path == null || path.equals("/")) {
                return u.getHost();
            }
            return path.length() > 40 ? path.substring(0, 40) + "…" : path;
        } catch (Throwable t) {
            return url;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE_CHOOSER) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    results = new Uri[n];
                    for (int i = 0; i < n; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 年龄确认弹窗阶段 web 尚未创建，直接走系统默认（关闭弹窗/退出）
            if (web == null) {
                return super.onKeyDown(keyCode, event);
            }
            if (customView != null) {
                exitFullscreen();
                return true;
            }
            if (web.canGoBack()) {
                web.goBack();
                return true;
            }
            long now = System.currentTimeMillis();
            if (now - lastBackPress < 2000) {
                finish();
            } else {
                lastBackPress = now;
                Toast.makeText(this, R.string.exit_hint, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) {
            web.onPause();
        }
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) {
            web.saveState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.setWebChromeClient(null);
            web.destroy();
        }
        super.onDestroy();
    }
}
