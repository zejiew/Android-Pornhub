package com.phclient.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 广告 / 统计域名黑名单。
 * 只拦截广告与埋点域名，绝不触碰视频 CDN（*.phncdn.com / *.pornhub.com），
 * 否则会导致播放失败。如遇站点改版导致播放异常，可在设置里关掉拦截。
 */
public final class AdBlocker {

    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "google-analytics.com",
            "googletagmanager.com",
            "adservice.google.com",
            "exoclick.com",
            "exosrv.com",
            "juicyads.com",
            "juicyads.net",
            "trafficjunky.net",
            "trafficjunky.com",
            "popads.net",
            "popcash.net",
            "adnium.com",
            "tsyndicate.com",
            "realsrv.com",
            "adsco.re",
            "clickadu.com",
            "hilltopads.net",
            "onclickalgo.com",
            "adsterra.com",
            "mgid.com",
            "outbrain.com",
            "taboola.com",
            "adskeeper.com",
            "adpushup.com",
            "propellerads.com"
    ));

    public static boolean isBlocked(String url) {
        if (url == null) {
            return false;
        }
        String host;
        try {
            host = android.net.Uri.parse(url).getHost();
        } catch (Throwable t) {
            return false;
        }
        if (host == null) {
            return false;
        }
        host = host.toLowerCase(Locale.US);
        for (String d : BLOCKED) {
            if (host.equals(d) || host.endsWith("." + d)) {
                return true;
            }
        }
        return false;
    }

    private AdBlocker() {
    }
}
