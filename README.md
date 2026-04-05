# drum-practice

Kotlin Multiplatform + Compose Multiplatform 的离线鼓练习工具骨架：Android、Desktop（JVM）；在 **macOS** 上可额外启用 **iOS** 目标。

## 环境

- JDK 17+
- Android Studio / IntelliJ（建议安装 Kotlin Multiplatform 插件）
- Android SDK：复制 `local.properties.example` 为 `local.properties`，设置 `sdk.dir`（或设置环境变量 `ANDROID_HOME`）

## 构建

```bash
# Windows
.\gradlew.bat :composeApp:assembleDebug

# 桌面端（Compose Desktop）
.\gradlew.bat :composeApp:run
```

Gradle 发行版已配置为 **华为云** 镜像；Maven 仓库在 `settings.gradle.kts` 中增加了 **阿里云** 镜像以加速依赖解析。

## 模块说明

- `composeApp`：`commonMain` 共享 UI 与领域逻辑；`androidMain` / `desktopMain` / `iosMain`（仅 macOS 注册 iOS 目标时参与编译）为各端实现。
- 五线谱：**Android** 上为 **Verovio（JNI）+ WebView 仅用于展示 SVG**（`MusicXmlScoreScreen`）；桌面 / iOS 该入口为占位说明。
- 数据：**SQLDelight**（`DrumDatabase`）；**Koin** 在 Android `Application` 中注入 `openDrumDatabase()`。
- 节拍器：`MetronomeEngine` 为 expect/actual 占位，后续在 Android 用 `AudioTrack` 等实现。

## iOS（macOS）

在 macOS 上构建时 Gradle 会注册 `iosArm64` / `iosSimulatorArm64` 并编译 `iosMain`；需配合 Xcode 工程（可用官方 KMP 向导生成 `iosApp` 再合并）以在模拟器/真机运行。
