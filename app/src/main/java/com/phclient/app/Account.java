package com.phclient.app;

import android.webkit.CookieManager;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 登录用户识别。
 *
 * 收藏页 / 历史页的 URL 必须带用户名（/users/<name>/videos/favorites），
 * 所以需要知道当前登录的是哪个账号。这里用两条路：
 *   1. 页面加载完成后，从 DOM 里嗅探用户名（最可靠，站点会渲染 "Hi <name>" 等）
 *   2. 之前嗅探到的结果缓存到 SharedPreferences，避免每次都要等
 */
public final class Account {

    private static final Pattern[] PATTERNS = {
            // 顶部用户菜单常见的几种渲染形态
            Pattern.compile("\"?username\"?\\s*[:=]\\s*\"([A-Za-z0-9_.\\-]{2,40})\""),
            Pattern.compile("/users/([A-Za-z0-9_.\\-]{2,40})/videos/favorites", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/users/([A-Za-z0-9_.\\-]{2,40})\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("id=\"?username\"?[^>]*>\\s*([A-Za-z0-9_.\\-]{2,40})\\s*<", Pattern.CASE_INSENSITIVE),
    };

    private Account() {
    }

    /** 从一段 HTML 里尝试解析用户名，解析不到返回 null */
    public static String sniffFromHtml(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        String head = html.length() > 200_000 ? html.substring(0, 200_000) : html;
        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(head);
            if (m.find()) {
                String name = m.group(1);
                // 过滤掉明显的非用户名（站点自身的静态路径）
                String low = name.toLowerCase(Locale.US);
                if (low.equals("favorites") || low.equals("viewed") || low.equals("videos")
                        || low.equals("public") || low.equals("upload") || low.equals("login")) {
                    continue;
                }
                return name;
            }
        }
        return null;
    }

    /** 读取缓存中的用户名 */
    public static String cached() {
        String n = App.get().prefs().getString(Const.KEY_USERNAME, "");
        return (n == null || n.isEmpty()) ? null : n;
    }

    /** 缓存用户名 */
    public static void remember(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        App.get().prefs().edit().putString(Const.KEY_USERNAME, name).apply();
    }

    public static void forget() {
        App.get().prefs().edit().remove(Const.KEY_USERNAME).apply();
    }

    /** 是否看起来已经登录（有 cookie 或已缓存用户名） */
    public static boolean likelyLoggedIn() {
        try {
            String c = CookieManager.getInstance().getCookie("https://www.pornhub.com/");
            if (c != null && (c.contains("il=") || c.contains("ph_") || c.contains("sessid")
                    || c.contains("login"))) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return cached() != null;
    }
}
