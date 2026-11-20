# PH 客户端（Android）

一个基于 **Android 系统 WebView** 的网页客户端工程。**只包含壳，不包含、不托管任何内容**，
所有页面、账号、收藏数据都来自你所访问的站点本身。

---

## 一、这个方案是什么 / 为什么这么做

| 需求 | 实现方式 |
|---|---|
| 登录 | 用系统 WebView 打开真实站点登录页，**Cookie 持久化**在本机 |
| 收藏 | 直接看站点自己的收藏页（数据在站点账号里，不在 App 里） |
| 观看 | WebView 内 HTML5 播放器 + 原生全屏接管 |
| 下载 | 系统 `DownloadManager`，自动带上 Cookie 与 Referer |

**为什么不去逆向私有 API：** 站点登录后的所有接口都依赖会话 Cookie，且带签名与风控。
自己实现一套 API 客户端，一旦对方改动就会整套失效，还会违反其服务条款。
用 WebView 承载真实站点，功能天然完整、维护成本几乎为零，也不会因为改版而崩。

---

## 二、怎么把 APK 编出来

### 方式 A：Android Studio（最省事，推荐）

1. 安装 [Android Studio](https://developer.android.com/studio)（自带 JDK 17 与 SDK）
2. `File → Open`，选择本文件夹
3. 等待 Gradle Sync 完成（首次会下载依赖）
4. 菜单 `Build → Build Bundle(s)/APK(s) → Build APK(s)`
5. 产物：`app/build/outputs/apk/release/app-release.apk`

### 方式 B：命令行

```bash
# 需要：JDK 17、Android SDK（platforms;android-34、build-tools;34.0.0）
./gradlew assembleRelease        # macOS / Linux
gradlew.bat assembleRelease      # Windows
```

Windows 也可以直接双击 `build-apk.bat`。

### 方式 C：GitHub Actions 云端出包（本机没环境时最靠谱）

1. 把整个项目 push 到 GitHub 仓库
2. `.github/workflows/build-apk.yml` 会自动构建
3. 在仓库的 **Actions** 页面 → 对应运行 → **Artifacts** 下载 `app-release.apk`

---

## 三、装到手机上

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```
或把 APK 传到手机，在文件管理器里点开安装（需允许「安装未知来源应用」）。

**首次启动会弹年龄确认**，确认后进入站点。

---

## 四、关键实现点（改代码时看这里）

| 文件 | 作用 |
|---|---|
| `MainActivity.java` | 主容器：WebView、Cookie、全屏、下载、返回键 |
| `App.java` | Application：开启 WebView 调试、全局 Cookie 接受 |
| `Account.java` | 识别当前登录用户名（收藏页 URL 需要它） |
| `AdBlocker.java` | 广告域名黑名单（**不含**视频 CDN，避免影响播放） |
| `DownloadStore.java` | 下载记录的本地存储 |
| `SimpleFileProvider.java` | 零依赖的 FileProvider（Android 7+ 打开本地视频必需） |
| `Const.java` | 站点地址、UA、偏好键集中管理 |

几个必须知道的细节：

- **收藏页 URL 必须带用户名**：正确格式是
  `/users/<你的用户名>/videos/favorites`。
  如果写成 `/users/favorites`，站点会把 `favorites` **当成用户名**，
  跳到一个叫 favorites 的用户主页上。App 会从页面自动嗅探用户名并缓存，
  识别失败时引导你去登录页。
- **搜索按钮**必须先拿到焦点再主动 `showSoftInput`，只调 `requestFocus`
  键盘不会弹出，用户会以为按钮坏了。
- **Cookie 是登录态的唯一依赖**。`setAcceptCookie(true)` + `setAcceptThirdPartyCookies`
  缺一不可；`MainActivity` 在 `onPageFinished` 与 `onPause` 都调了 `flush()`，
  避免进程被杀掉后登录态丢失。
- **桌面 UA**：站点移动版页面限制较多，用桌面 UA 兼容性最好。想强制移动版改
  `Const.UA_DESKTOP` 即可。
- **下载必须带 Cookie 和 Referer**，否则私有资源返回 403。见 `enqueueDownload()`。
- **广告拦截只拦广告域**，误拦 CDN 会导致视频一直转圈；出问题就去掉
  `shouldInterceptRequest` 里的判断。
- **`usesCleartextTraffic="true"`**：部分 CDN 走 http，改成 false 会更安全但可能播放失败。

---

## 五、可能要改的地方

| 想改什么 | 改哪里 |
|---|---|
| 站点地址 | `Const.HOME` |
| 起始页 | `MainActivity` 里 `web.loadUrl(...)` |
| 底部导航项 | `activity_main.xml` + `initUi()` |
| 主题色 | `res/values/colors.xml` |
| 应用名 | `res/values/strings.xml` 的 `app_name` |
| 包名 | `app/build.gradle` 的 `applicationId` + `namespace` |
| 签名 | `app/build.gradle` 的 `signingConfigs`（默认用 debug 签名，见下方说明） |

**关于签名**：默认 `release` 用 debug 密钥签名，好处是直接能装、能覆盖升级；
坏处是不能上架、也不能防止别人重新打包。要发布就换成自己的 keystore。

---

## 六、需要你自己补的部分（我没法替你决定）

1. **应用图标**目前是矢量占位图（`ic_launcher_fg.xml`），
   想换成自己的图，用 Android Studio 的 `Image Asset` 生成即可。
2. **包名 `com.phclient.app` 建议改掉**，避免和别人的包冲突。
3. 如果站点有登录后的地区限制，**需要你自己的网络环境配合**，App 不做这方面处理。

---

## 七、务必注意

- 成人内容受各地法律约束。**请确认你所在地区合法、你本人已成年**，并自行承担访问后果。
- 本项目**不提供、不存储、不分发任何内容**，只是一个浏览器壳。
- 站点用户协议通常禁止未授权客户端或抓取内容。**自用可以，分发要谨慎**。
- Google Play 等商店禁止此类应用，**不要尝试上架**。
