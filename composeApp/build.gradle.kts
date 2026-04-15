import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

val hostIsMacOs: Boolean =
    System.getProperty("os.name").lowercase().contains("mac")

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    if (hostIsMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { target ->
            target.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.jetbrains.compose.components.animatedimage)
            implementation(libs.bundles.kmp.common)
            implementation(libs.androidx.navigation.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        if (hostIsMacOs) {
            iosMain.dependencies {
                implementation(libs.sqldelight.native)
            }
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.sqlite)
            }
        }
    }
}

// Create a variable called keystorePropertiesFile, and initialize it to your
// keystore.properties file, in the rootProject folder.
val keystorePropertiesFile = rootProject.file("keystore.properties")

// Initialize a new Properties() object called keystoreProperties.
val keystoreProperties = Properties()

// Load your keystore.properties file into the keystoreProperties object.
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    namespace = "com.drumpractise.app"
    compileSdk = 35

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    defaultConfig {
        applicationId = "com.drumpractise.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
                arguments +=
                    listOf(
                        "-DPROJECT_ROOT=${rootProject.projectDir.invariantSeparatorsPath}",
                    )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.named("main").configure {
        java.srcDir("src/androidMain/java")
    }


}

val verovioDataSource = rootProject.layout.projectDirectory.dir("external/verovio/data")
val verovioAssetsDest = layout.projectDirectory.dir("src/androidMain/assets/verovio/data")

tasks.register<Copy>("copyVerovioData") {
    group = "verovio"
    description = "Copy Verovio engraving data into android assets (requires external/verovio)"
    from(verovioDataSource)
    into(verovioAssetsDest)
}

tasks.named("preBuild").configure {
    dependsOn("copyVerovioData")
}

val swigOutputJava = layout.projectDirectory.dir("src/androidMain/java/org/verovio/lib")
val swigOutputCpp = layout.projectDirectory.file("src/androidMain/cpp/verovio_wrap.cxx")
val swigInterfaceFile = rootProject.layout.projectDirectory.file("external/verovio/bindings/java/verovio.i")

tasks.register<Exec>("generateVerovioSwigBindings") {
    group = "verovio"
    description = "Regenerate JNI bindings with SWIG (optional; set VEROVIO_SWIG to swig executable path)"
    workingDir = rootProject.projectDir
    val swigExe =
        providers.environmentVariable("VEROVIO_SWIG").orNull
            ?: "swig"
    doFirst {
        swigOutputJava.asFile.mkdirs()
        swigOutputCpp.asFile.parentFile.mkdirs()
    }
    commandLine(
        swigExe,
        "-java",
        "-c++",
        "-package",
        "org.verovio.lib",
        "-outdir",
        swigOutputJava.asFile.absolutePath,
        "-o",
        swigOutputCpp.asFile.absolutePath,
        swigInterfaceFile.asFile.absolutePath,
    )
}

compose.desktop {
    application {
        mainClass = "com.drumpractise.app.MainKt"
    }
}

sqldelight {
    databases {
        create("DrumDatabase") {
            packageName.set("com.drumpractise.db")
        }
    }
}
