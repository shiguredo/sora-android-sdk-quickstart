pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "sora-android-sdk-quickstart"
include(":quickstart")

// ローカルの sora-sdk-android のパスを指定
val soraSdkDirPath = ""
// ローカル SDK を composite build で取り込む
includeBuild(soraSdkDirPath) {
    dependencySubstitution {
        substitute(module("com.github.shiguredo:sora-android-sdk")).using(project(":sora-android-sdk"))
    }
}
