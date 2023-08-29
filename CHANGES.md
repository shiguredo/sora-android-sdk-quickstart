# 変更履歴

- UPDATE
    - 下位互換がある変更
- ADD
    - 下位互換がある追加
- CHANGE
    - 下位互換のない変更
- FIX
    - バグ修正

## develop

## sora-andoroid-sdk-2023.2.0

- [UPDATE] システム条件を更新する
    - Android Studio 2022.2.1 以降
    - WebRTC SFU Sora 2023.1.0 以降
    - Sora Android SDK 2023.2.0 以降
    - @miosakuma

## sora-andoroid-sdk-2023.1.0

- [UPDATE] システム条件を更新する
    - Android Studio 2022.1.1 以降
    - WebRTC SFU Sora 2022.2.0 以降
    - @miosakuma
- [UPDATE] `compileSdkVersion` を 33 に上げる
    - @miosakuma
- [UPDATE] `targetSdkVersion` を 33 に上げる
    - @miosakuma
- [UPDATE] `minSdkVersion` を 26 に上げる
    - @miosakuma
- [UPDATE] Kotlin のバージョンを 1.8.10 に上げる
    - @miosakuma
- [UPDATE] Compose Compiler のバージョンを 1.4.3 に上げる
    - @miosakuma
- [UPDATE] Gradle を 7.6.1 に上げる
    - @miosakuma
- [UPDATE] 依存ライブラリーのバージョンを上げる
    - com.android.tools.build:gradle を 7.4.2 に上げる
    - com.github.ben-manes:gradle-versions-plugin を 0.46.0 に上げる
    - org.jlleitschuh.gradle:ktlint-gradle を 11.3.1 に上げる
    - com.google.code.gson:gson を 2.10.1 に上げる
    - androidx.appcompat:appcompat を 1.6.1 に上げる
    - com.google.android.material:material: を 1.8.0 に上げる

## sora-andoroid-sdk-2022.4.0

- [UPDATE] compileSdkVersion を 32 に上げる
    - @miosakuma
- [UPDATE] targetSdkVersion を 32 に上げる
    - @miosakuma
- [UPDATE] Kotlin のバージョンを 1.7.10 に上げる
    - @miosakuma
- [UPDATE] Gradle を 7.5.1 に上げる
    - @miosakuma
- [UPDATE] 依存ライブラリーのバージョンを上げる
    - com.google.code.gson:gson を 2.9.1 に上げる
    - androidx.appcompat:appcompat を 1.5.0 に上げる
    - com.android.tools.build:gradle を 7.2.2 に上げる
    - @miosakuma

## sora-andoroid-sdk-2022.3.0

- [UPDATE] システム条件を更新する
    - Android Studio 2021.2.1 以降
    - WebRTC SFU Sora 2022.1 以降
    - Sora Android SDK 2022.3.0 以降
    - @miosakuma
- [UPDATE] compileSdkVersion を 31 に上げる
    - AndroidManifest.xml の Activity に `android:exported="true"` を明示的に記載する
    - @miosakuma
- [UPDATE] Gradle のバージョンを 7.4.2 に上げる
    - @miosakuma
- [UPDATE] Gktlint のバージョンを 0.45.2 に上げる
    - @miosakuma
- [UPDATE] 依存ライブラリを更新する
    - com.android.tools.build:gradle を 7.2.1 に上げる
    - org.jlleitschuh.gradle:ktlint-gradle を 10.3.0 に上げる
    - com.google.code.gson:gson を 2.9.0 に上げる
    - androidx.appcompat:appcompat を 1.4.2 に上げる
    - com.google.android.material:material を 1.6.1 に上げる
    - com.github.permissions-dispatcher:permissionsdispatcher を 4.9.2　に上げる
    - com.github.ben-manes:gradle-versions-plugin を 0.42.0 に上げる
    - @miosakuma

## sora-andoroid-sdk-2022.2.0

- [UPDATE] システム条件を更新する
    - Android Studio 2021.1.1 以降
    - Sora Android SDK 2022.2.0 以降
    - @miosakuma
- [UPDATE] Kotlin Android Extensions の非推奨化に伴い、ビュー バインディングに移行する
    - @miosakuma

## sora-andoroid-sdk-2022.1.0

- [UPDATE] システム条件を更新する
    - Android Studio 2020.3.1 以降
    - Sora Android SDK 2022.1.0 以降
    - @miosakuma
- [ADD] シグナリング接続時に送信するメタデータを外部ファイルから設定できるようにする
    - @miosakuma
  
## sora-andoroid-sdk-2021.3

- [UPDATE] システム条件を更新する
    - Sora Android SDK 2021.3 以降
    - @miosakuma

## sora-andoroid-sdk-2021.2

- [UPDATE] システム条件を更新する
    - Android Studio 4.2 以降
    - WebRTC SFU Sora 2021.1 以降
    - Sora Android SDK 2021.2 以降
    - @miosakuma
- [UPDATE] `com.android.tools.build:gradle` を 4.2.2 に上げる
    - @enm10k
- [UPDATE] JCenter への参照を取り除く
    - @enm10k
- [UPDATE] シグナリングエンドポイント URL の設定を `/build.gradle` から `/gradle.properties.example` に移動する
    - @miosakuma

## sora-andoroid-sdk-2021.1.1

- [UPDATE] SDK のバージョンを 2021.1.1 に上げる
    - @enm10k

## 2021.1

### UPDATE

- SDK のバージョンを 2021.1 に上げる
- Kotlin のバージョンを 1.4.31 に上げる
- Gradle のバージョンを 6.8.3 に上げる
- 依存ライブラリを更新する
    - com.android.tools.build:gradle を 4.1.2 に上げる
    - com.google.android.material:material を 1.3.0 に上げる
    - org.permissionsdispatcher:permissionsdispatcher を 4.8.0 に上げる
    - org.permissionsdispatcher:permissionsdispatcher-processor を 4.8.0 に上げる
    - androidx.appcompat:appcompat を 1.2.0 に上げる

### CHANGE

- ビルドの設定を更新する
    - compileSdkVersion を 31 に上げる
    - targetSdkVersion を 31 に上げる
    - minSdkVersion を 21 に上げる

## 2020.1

- SDK のバージョンを 2020.1 に上げる

## 1.9.0

### UPDATE

- SDK のバージョンを 1.9.0 に上げる
- Android Studio 3.4.2 に対応する
- Kotlin を 1.3.41 に上げる
- PermissionsDispatcher を 4.5.0 に上げる

### CHANGE

- `MODE_IN_COMMUNICATION` を使うように変更する

## 1.8.1

### UPDATE

- SDK のバージョンを 1.8.1 に上げる
- Kotlin を 1.3.30 に上げる
- PermissionsDispatcher を 4.3.1 に上げる
- Android Studio 3.4 に対応する

## 1.8.0

### UPDATE

- SDK のバージョンを 1.8.0 に上げる
- SDP semantics を Unified Plan に変更する
- Kotlin を 1.3.20 に上げる
- Anko を依存から外す
- Android support library から androidx に移行する
- PermissionsDispatcher を 4.3.0 に上げる
- compileSdkVersion, targetSdkVersion を 28 に上げる
- Android Studio 3.3 に対応する

## 1.7.1

### UPDATE

- Kotlin を 1.2.71 に上げる
- Android Studio 3.2.1 に対応する
- targetSdkVersion を 27 に上げる
    - cf. Google Developers Japan: 今後の Google Play でのアプリのセキュリティおよびパフォーマンスの改善について
      <https://developers-jp.googleblog.com/2017/12/improving-app-security-and-performance.html>

## 1.7.0

### UPDATE

- SDK のバージョンを 1.7.0 に上げる
- Android Studio 3.1.4 に対応する

## 1.6.0

### UPDATE

- SDK のバージョンを 1.6.0 に上げる
- Android Studio 3.1.2 に対応する
- PermissionsDispatcher を 3.2.0 に上げる
    - lint バグフィックスにより不要な SuppressLint アノテーションを削除する
- Kotlin を 1.2.51 に上げる
- Anko を 0.10.5 に上げる
- 音量をボリュームキーから制御できるようにする
- PermissionsDispatcher を 3.3.1 に上げる

### CHANGE

- ボリューム変更対象ストリームを `STREAM_VOICE_CALL` に変更する

## 1.5.3

### UPDATE

- SDK のバージョンを 1.5.4 に上げる

## 1.5.2

### UPDATE

- SDK のバージョンを 1.5.3 に上げる
- Kotlin 1.2.31 に上げる

## 1.5.1

### UPDATE

- SDK のバージョンを 1.5.2 に上げる
- kotlin ソースディレクトリの名前を kotlin に変更する
- PermissionDispatcher 3.x に対応する
- Kotlin 1.2.30 に上げる

## 1.5.0

### UPDATE

- SDK のバージョンを 1.5.0 に上げる
- Sora プッシュ API の listener を実装する

## 1.4.2

### UPDATE

- SDK のバージョンを 1.4.1 に上げる

## 1.4.1

### UPDATE

- Android support library を 26.0.2 に上げる
- PermissionsDispatcher を 3.1.0 に上げる

## 1.4.0

### UPDATE

- SDK のバージョンを 1.4.0 に上げる
- Android Studio 3.0 に対応する
    - gradle: 4.1
    - android-maven-gradle-plugin: 2.0
- Kotlin 1.2.10 に上げる

## 1.3.1

### UPDATE

- SDK のバージョンを 1.3.1 に上げる
- Kotlin を 1.1.51 に上げる
- CircleCI でのビルドを設定する

### CHANGE

- Signaling Endpoint の設定を Config.kt から build.gradle に移動する
- パッケージ名を jp.shiguredo.samples から jp.shiguredo.sora.quickstart に変更する

## 1.3.0

### UPDATE

- SDK のバージョンを上げる
- デバッグログを出力するよう変更する

## 1.2.0

### UPDATE

- SDK のバージョンを上げる

## 1.1.0

### UPDATE

- 依存ライブラリのバージョンを上げる

### ADD

- Sora Android SDK 依存を JitPack 経由とし、AAR の手動ダウンロードを不要にする

## 1.0.0

最初のリリース
