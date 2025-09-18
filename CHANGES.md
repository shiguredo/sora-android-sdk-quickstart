# 変更履歴

- CHANGE
  - 下位互換のない変更
- UPDATE
  - 下位互換がある変更
- ADD
  - 下位互換がある追加
- FIX
  - バグ修正

## 2025.3

- [UPDATE] Sora Android SDK を 2025.3.0 にあげる
  - @zztkm

### misc

- [UPDATE] gradle を Kotlin DSL 対応する
  - build.gradle、settings.gradle、samples/build.gradle を kts ファイルに置き換えた
  - ライブラリバージョン管理を Version Catalog による管理に変更した
  - @t-miya

## sora-andoroid-sdk-2025.2.0

**リリース日**: 2025-09-17

- [CHANGE] 廃止された `onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason)` を削除する
  - `onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason, message: String)` のみを実装するように変更
  - @zztkm
- [CHANGE] `SoraMediaOption` 設定時に `enableMultistream()` を適用しないようにする
  - Sora がデフォルトでレガシーストリームを使用するように設定されている場合、接続エラーになる
  - @zztkm
- [UPDATE] システム条件を更新する
  - Android Studio 2025.1.1 以降
  - WebRTC SFU Sora 2025.1.0 以降
  - @miosakuma
- [UPDATE] Sora Android SDK を 2025.2.0 にあげる
  - @miosakuma @zztkm
- [UPDATE] `compileSdkVersion` を 36 に上げる
  - @miosakuma
- [UPDATE] `targetSdkVersion` を 36 に上げる
  - @miosakuma
- [UPDATE] 依存ライブラリーのバージョンを上げる
  - com.android.tools.build:gradle を 8.11.1 に上げる
  - Gradle を 8.14.3 に上げる
  - com.google.code.gson:gson を 2.13.1 に上げる
  - @miosakuma
- [UPDATE] edge-to-edge の画面表示に対応する
  - targetSdkVersion 35 以降 edge-to-edge の画面表示がデフォルトとなった
  - activity_main.xml に `android:fitsSystemWindows="true"` を指定する
  - バックグラウンドカラーを白以外にしてステータスバーの文字が見えるようにする
  - @miosakuma
- [UPDATE] `close` メソッド内で `disableStopButton` を呼び出すようにする
  - `close` は OkHttp のワーカースレッドで実行される可能性があり、ワーカースレッドでは UI を操作できないため、`disableStopButton` を必ず UI スレッドで呼び出すようにした
  - @zztkm
- [UPDATE] シグナリング URL を複数指定できるようにする
  - エンドポイントを複数指定できるように変更
  - 今まで通り 1 つのエンドポイントを指定することも可能
  - @zztkm
- [UPDATE] `SoraMediaChannel.Listener` の実装を追加する
  - `onClose` で closeEvent に応じてログを出力するようにした
  - `onWarning` を追加した
  - `onRemoveRemoteStream` を用いてリモートストリームが削除された時に画面をクリアするようにした
  - `onAttendeesCountUpdated` で接続数をログに出力するようにした
  - `onOfferMessage` で Sora から受信した Offer メッセージをログに出力するようにした
  - `onNotificationMessage` で通知内容をログに出力するようにした
  - @zztkm

### misc

- [UPDATE] GitHub Actions の定期実行をやめる
  - @zztkm
- [ADD] .github ディレクトリに copilot-instructions.md を追加
  - @torikizi

## sora-andoroid-sdk-2025.1.1

**リリース日**: 2025-08-07

- [UPDATE] Sora Android SDK を 2025.1.1 にあげる
  - @miosakuma

## sora-andoroid-sdk-2025.1.0

**リリース日**: 2025-01-27

- [UPDATE] システム条件を更新する
  - Android Studio 2024.2.2 以降
  - @miosakuma @zztkm
- [FIX] 画面回転時に Sora への接続が切断されないようにする
  - 画面回転時にアクティビティが再作成され、 `onDestroy()` が実行されることにより Sora から切断する処理が実行されていた
  - AndroidManifest.xml の `<activity>` に `android:configChanges` を追加して画面回転時にアクティビティが再作成を行わないように設定することで回避した
  - @miosakuma

## sora-andoroid-sdk-2024.3.1

**リリース日**: 2024-08-30

- [UPDATE] システム条件を更新する
  - Android Studio 2024.1.1 以降
  - WebRTC SFU Sora 2024.1.0 以降
  - Sora Android SDK 2024.3.1 以降
  - @miosakuma
- [UPDATE] Android Gradle Plugin (AGP) を 8.5.0 にアップグレードする
  - Android Studion の AGP Upgrade Assistant を利用してアップグレードされた内容
    - `com.android.tools.build:gradle` を 8.5.0 に上げる
    - ビルドに利用される Gradle を 8.7 に上げる
    - Android マニフェストからビルドファイルにパッケージを移動
      - Android マニフェストに定義されていた package を削除
      - ビルドファイルに namespace を追加
  - AGP 8.5.0 対応で発生したビルドスクリプトのエラーを手動で修正した内容
    - AGP 8.0 から buildConfig がデフォルト false になったため、true に設定する
  - @zztkm
- [UPDATE] 依存ライブラリーのバージョンを上げる
  - com.google.code.gson:gson を 2.11.0 に上げる
  - androidx.appcompat:appcompat を 1.7.0 に上げる
  - com.google.android.material:material を 1.12.0 に上げる
  - @zztkm
- [UPDATE] compileSdkVersion を 34 に上げる
  - Android API レベル 34 以降でコンパイルする必要がある依存ライブラリがあるため
  - @zztkm
- [UPDATE] Kotlin のバージョンを 1.9.25 に上げる
  - @zztkm

## sora-andoroid-sdk-2024.3.0

Sora Android SDK 2024.3.0 のリリース作業時に発生した問題によりスキップしました。

## sora-andoroid-sdk-2024.2.0

- [UPDATE] システム条件を更新する
  - Sora Android SDK 2024.2.0 以降
  - @miosakuma
- [UPDATE] Github Actions の actions/setup-java@v4 にあげる
  - @miosakuma
- [FIX] Github Actions でのビルドを Java 17 にする
  - @miosakuma

## sora-andoroid-sdk-2024.1.1

- [UPDATE] システム条件を更新する
  - Android Studio 2023.2.1 以降
  - WebRTC SFU Sora 2023.2.0 以降
  - Sora Android SDK 2024.1.1 以降
  - @miosakuma

## sora-andoroid-sdk-2024.1.0

Sora Android SDK 2024.1.0 のリリース作業時に発生した問題によりスキップしました。

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
  - com.github.permissions-dispatcher:permissionsdispatcher を 4.9.2 　に上げる
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
