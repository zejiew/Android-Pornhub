package com.phclient.app;

/** 常量集中管理，换域名只改这里。 */
public final class Const {

    /** 站点入口 */
    public static final String HOME = "https://www.pornhub.com/";
    public static final String LOGIN = "https://www.pornhub.com/login";
    public static final String FAVORITES = "https://www.pornhub.com/users/favorites";
    public static final String SUBSCRIPTIONS = "https://www.pornhub.com/subscriptions";
    public static final String HISTORY = "https://www.pornhub.com/users/viewed";
    public static final String SEARCH = "https://www.pornhub.com/video/search?search=";

    /** 桌面 UA：站点对移动版页面限制较多，用桌面 UA 兼容性最好 */
    public static final String UA_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public static final String PREFS = "phclient";
    public static final String KEY_AGE_OK = "age_confirmed";

    private Const() {
    }
}
