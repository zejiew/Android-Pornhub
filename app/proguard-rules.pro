# 未开启混淆，保留占位。若改为 minifyEnabled true，请勿混淆 WebViewClient / JS 回调相关类。
-keepclassmembers class * extends android.webkit.WebViewClient { *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { *; }
