# Shizuku

`Shizuku` 是一个面向 Mizuki 静态博客的原生 Android 写作应用。它支持离线写作、本地草稿、Markdown 快捷插入、Mizuki Frontmatter 编辑、图片导入，以及直接推送到 GitHub 触发博客构建。

## 功能特性

- 原生 Android + Jetpack Compose 界面
- 面向 Mizuki 博客结构的文章管理与发布
- Markdown 正文编辑、快捷插入、分屏预览
- 可折叠元数据面板，支持完整 Frontmatter 字段
- 本地草稿持久化，支持离线写作
- 直接推送到 GitHub 仓库发布文章
- GitHub PAT 使用 Android Keystore 加密后再本地存储
- 支持删除本地草稿，并可选同步删除 GitHub 远程文章

## 技术栈

- Kotlin
- Jetpack Compose
- Room
- DataStore
- Retrofit + OkHttp
- Markwon

## 构建要求

- JDK 17
- Android SDK 34
- Termux 或标准 Android 构建环境

## 调试构建

```bash
./gradlew :app:assembleDebug
```

调试 APK 输出路径：

`app/build/outputs/apk/debug/app-debug.apk`

## Release 签名与构建

项目支持通过根目录的 `keystore.properties` 读取 release 签名配置。文件格式如下：

```properties
storeFile=/absolute/path/to/shizuku-release.jks
storePassword=your_store_password
keyAlias=shizuku-release
keyPassword=your_key_password
```

构建 release：

```bash
./gradlew :app:assembleRelease
```

release APK 输出路径：

`app/build/outputs/apk/release/app-release.apk`

## GitHub 发布流程

1. 创建或准备一个公开 GitHub 仓库
2. 提交源码并推送
3. 使用 `gh release create v1.0.0` 上传 release APK

## 许可

源码采用 **CC BY-NC 4.0** 许可。

- 你可以分享和修改源码
- 必须保留署名
- 不可将源码用于商业用途
- 许可详情见 [LICENSE](./LICENSE)

## 状态

当前发布版本：`1.0.0`
