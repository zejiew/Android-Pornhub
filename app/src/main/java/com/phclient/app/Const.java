package com.phclient.app;

/** 常量集中管理，换域名只改这里。 */
public final class Const {

    /** 站点入口 */
    public static final String HOME = "https://www.pornhub.com/";
    public static final String LOGIN = "https://www.pornhub.com/login";

    /**
     * 收藏页。
     *
     * 注意：站点路径必须带用户名，格式为 /users/<username>/videos/favorites。
     * 若写成 /users/favorites，站点会把 "favorites" 当作用户名，
     * 从而跳到某个名叫 favorites 的用户主页（这是此前的 bug）。
     */
    public static final String USER_FAVORITES_FMT =
            "https://www.pornhub.com/users/%s/videos/favorites";

    /** 需要用户名，同上格式 */
    public static final String USER_HISTORY_FMT =
            "https://www.pornhub.com/users/%s/videos/viewed";

    public static final String SUBSCRIPTIONS = "https://www.pornhub.com/subscriptions";
    public static final String SEARCH = "https://www.pornhub.com/video/search?search=";

    /** 桌面 UA：站点对移动版页面限制较多，用桌面 UA 兼容性最好 */
    public static final String UA_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + " (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public static final String PREFS = "phclient";
    public static final String KEY_AGE_OK = "age_confirmed";

    /** 登录用户名缓存：收藏/历史页需要它来拼 URL */
    public static final String KEY_USERNAME = "login_username";

    private Const() {
    }
}
