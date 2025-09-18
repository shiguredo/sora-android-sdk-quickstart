import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ktlint)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "jp.shiguredo.sora.quickstart"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // アプリで参照する設定を BuildConfig / resource に書き込む。
        val signalingEndpoint = project.properties["signaling_endpoint"] as? String ?: ""
        val channelId = project.properties["channel_id"] as? String ?: ""
        val signalingMetadata = project.properties["signaling_metadata"] as? String ?: ""

        buildConfigField("String", "SIGNALING_ENDPOINT", "\"$signalingEndpoint\"")
        buildConfigField("String", "CHANNEL_ID", "\"$channelId\"")
        buildConfigField("String", "SIGNALING_METADATA", "\"$signalingMetadata\"")

        manifestPlaceholders["usesCleartextTraffic"] = rootProject.extra["usesCleartextTraffic"] as Boolean
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaCompatibility.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaCompatibility.get())
    }

    buildFeatures {
        // AGP 8.0 からデフォルトで false になった
        // このオプションが true でないと、defaultConfig に含まれている
        // buildConfigField オプションが無効になってしまうため、true に設定する
        // 参考: https://developer.android.com/build/releases/past-releases/agp-8-0-0-release-notes#default-changes
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    // AGP 8.0 からモジュールレベルの build script 内に namespace が必要になった
    // 参考: https://developer.android.com/build/releases/past-releases/agp-8-0-0-release-notes#namespace-dsl
    namespace = "jp.shiguredo.sora.quickstart"
}

ktlint {
    // ktlint バージョンは以下の理由によりハードコーディングしている
    // - Gradleの設計上の制限: プラグイン設定の評価タイミングが早すぎる
    // - ktlint-gradleプラグインの仕様: 動的な値の解決に対応していない
    // - Version Catalogの制約: プラグイン設定フェーズでは利用不可
    version.set("0.45.2")
    android.set(false)
    outputToConsole.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    ignoreFailures.set(false)
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.gson)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.permissions.dispatcher)
    kapt(libs.permissions.dispatcher.processor)

    // Sora Android SDK
    if (findProject(":sora-android-sdk") != null) {
        // module is included
        api(project(":sora-android-sdk"))
    } else {
        // external dependency
        implementation("${libs.sora.android.sdk.get()}@aar") {
            isTransitive = true
        }
    }
}

configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(0, "seconds")
        cacheChangingModulesFor(0, "seconds")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    finalizedBy("ktlintFormat")
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
}
