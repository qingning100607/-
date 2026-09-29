# 云朵休息室 ☁️

一个安静的放松类 Android App：自然音景、呼吸练习、云朵小精灵、每日云签、心情站、解压铺小游戏。

- 包名：`com.qingning.cloudrest`
- 当前版本：**v1.9.3**
- 最低系统：Android 7.0（minSdk 24）

## 更新日志

见 [CHANGELOG.md](CHANGELOG.md)。

## 自行构建

需要 JDK 17 + Android SDK（compileSdk 36）。

```bash
# 1. 准备 local.properties（本文件不入库，需自行创建）
cat > local.properties <<'EOF'
sdk.dir=/path/to/Android/Sdk

# 下面四项用于打包已签名的 release APK
cloudrest.storeFile=keystore/release.jks
cloudrest.storePassword=你的密钥库口令
cloudrest.keyAlias=你的别名
cloudrest.keyPassword=你的别名口令
EOF

# 2. 打包
gradle assembleRelease
```

产物在 `app/build/outputs/apk/release/app-release.apk`。

> ⚠️ `keystore/`、`local.properties` 已在 `.gitignore` 中，**请勿提交**。
> 只想跑 debug 的话，删掉上面四个 `cloudrest.*` 项即可，`assembleDebug` 不受影响。

## 检查更新机制

App 内的「检查更新」会按顺序尝试：

1. 仓库根目录的 `version.json`（raw 直读，最快最稳）
2. `tags` 页面 HTML 解析
3. GitHub API（`releases/latest` → `tags`）

发新版本时，只需：

1. 修改 `version.json` 里的 `version`
2. 打 tag（如 `1.9.4`）
3. 在 Releases 里发布并附上 APK

## 授权

自然声与短音效均为真实录音采样（Wikimedia Commons，CC0 / CC BY / CC BY-SA），
作者署名见 App 内「设置 → 关于」。详见 [LICENSE](LICENSE)。