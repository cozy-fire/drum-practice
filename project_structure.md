# drum-practice 项目结构

Kotlin Multiplatform + Compose Multiplatform 单模块工程。下列为**源码与配置**目录树；构建产物（`build/`、`.gradle/`、`composeApp/build/`）及 IDE 配置（`.idea/`）不展开。

## 根目录

```
drum-practice/
├── .gitignore
├── README.md
├── project_structure.md          # 本文件
├── THIRD_PARTY_NOTICES.md      # 第三方许可（如 Verovio LGPL）
├── external/
│   └── README.md                 # Verovio 源码获取说明
├── build.gradle.kts              # 根工程（占位，逻辑在 composeApp）
├── settings.gradle.kts           # 模块与 Maven 仓库（含阿里云镜像）
├── gradle.properties
├── gradlew                       # Unix Gradle Wrapper
├── gradlew.bat                   # Windows Gradle Wrapper
├── local.properties              # 本机 Android sdk.dir（通常不提交）
├── local.properties.example      # SDK 路径模板
├── gradle/
│   ├── libs.versions.toml        # 版本目录（Kotlin、Compose、AGP、SQLDelight、Koin 等）
│   └── wrapper/
│       └── gradle-wrapper.properties
├── webassets/
│   └── README.md                 # 可选静态资源说明（当前无构建脚本）
└── composeApp/
    ├── build.gradle.kts          # KMP 目标、Compose、SQLDelight、Android、Desktop、Verovio NDK
    └── src/
        ├── commonMain/
        ├── androidMain/
        ├── desktopMain/
        └── iosMain/
```

## 模块 `composeApp`

| 路径 | 说明 |
|------|------|
| `composeApp/build.gradle.kts` | `androidTarget`、`jvm("desktop")`；在 **macOS** 上额外注册 iOS 目标；Compose / Serialization / SQLDelight / Koin；Verovio `copyVerovioData`、CMake / NDK |

### `commonMain`（共享）

```
composeApp/src/commonMain/kotlin/com/drumpractise/app/
├── App.kt                         # 主界面入口（导航）
├── data/
│   └── DrumDatabaseProvider.kt    # expect fun openDrumDatabase()
├── metronome/
│   └── MetronomeEngine.kt         # expect class 节拍器
├── navigation/
│   └── AppRoutes.kt
├── score/
│   └── MusicXmlScoreScreen.kt     # expect 离线五线谱（Android 为 Verovio）
├── theme/
├── workbench/
└── ...
```

### `androidMain`

```
composeApp/src/androidMain/
├── cpp/                           # CMake：libverovio-android + verovio_wrap
├── java/org/verovio/lib/          # SWIG 生成 Java 绑定
├── kotlin/.../score/
│   ├── MusicXmlScoreScreen.android.kt
│   └── nativenotation/VerovioScoreViewModel.kt
├── assets/
│   ├── verovio/data/              # 构建时从 external/verovio/data 拷贝
│   └── verovio-sample.mei
└── ...
```

### `desktopMain` / `iosMain`

- `MusicXmlScoreScreen.*.kt`：当前为「仅 Android 支持 Verovio」的占位 UI。

## 构建生成（不纳入版本树，仅说明）

| 位置 | 内容 |
|------|------|
| `composeApp/build/generated/sqldelight/` | SQLDelight 生成的 `DrumDatabase` 等 |
| `composeApp/build/outputs/apk/debug/` | Android Debug APK |
| 根目录 `build/` | Gradle 配置缓存报告等 |

## 跨端分层（概念）

- **commonMain**：共享 UI、`expect` 声明。
- **各平台 actual**：SQLite 驱动、节拍器、五线谱（Android JNI）等。
