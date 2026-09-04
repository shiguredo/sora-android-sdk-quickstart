import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun String.toBuildConfigStringLiteral(): String =
    "\"" +
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r") +
        "\""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
}

android {
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "jp.shiguredo.sora.quickstart"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"

        // アプリで参照する設定を BuildConfig / resource に書き込む。
        val signalingEndpoint = providers.gradleProperty("signaling_endpoint").orNull ?: ""
        val channelId = providers.gradleProperty("channel_id").orNull ?: ""
        val signalingMetadata = providers.gradleProperty("signaling_metadata").orNull ?: ""

        buildConfigField("String", "SIGNALING_ENDPOINT", "\"$signalingEndpoint\"")
        buildConfigField("String", "CHANNEL_ID", "\"$channelId\"")
        buildConfigField("String", "SIGNALING_METADATA", "\"$signalingMetadata\"")

        // ユーザー CA の動作確認用パラメーター
        // - ヒアドキュメント等により PEM 文字列を入力します
        // - PKCS#8 形式に対応しています
        // - PEM 文字列はコミットしないようにしてください
        // CA 証明書
        val caCertificatePem = providers.gradleProperty("ca_certificate_pem").orNull ?: ""
        // クライアント証明書
        val clientCertificatePem = providers.gradleProperty("client_certificate_pem").orNull ?: ""
        // クライアント証明書の秘密鍵
        val clientPrivateKeyPem = providers.gradleProperty("client_private_key_pem").orNull ?: ""

        buildConfigField("String", "CA_CERTIFICATE_PEM", caCertificatePem.toBuildConfigStringLiteral())
        buildConfigField("String", "CLIENT_CERTIFICATE_PEM", clientCertificatePem.toBuildConfigStringLiteral())
        buildConfigField("String", "CLIENT_PRIVATE_KEY_PEM", clientPrivateKeyPem.toBuildConfigStringLiteral())

        manifestPlaceholders["usesCleartextTraffic"] = rootProject.extra["usesCleartextTraffic"] as Boolean
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // AGP 8.0 からモジュールレベルの build script 内に namespace が必要になった
    // 参考: https://developer.android.com/build/releases/past-releases/agp-8-0-0-release-notes#namespace-dsl
    namespace = "jp.shiguredo.sora.quickstart"
}

ktlint {
    // 設定フェーズでは動的解決や Version Catalog を使えないため固定
    version.set("1.7.1")
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
    testImplementation(libs.junit)

    // Sora Android SDK
    if (findProject(":sora-android-sdk") != null) {
        // module is included
        api(project(":sora-android-sdk"))
    } else {
        // external dependency
        implementation(libs.sora.android.sdk)
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
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}
