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

### UPDATE

- Kotlin を 1.2.60 に上げた


## 1.7.0

### UPDATE

- SDK のバージョンを 1.7.0 に上げた
- Android Studio 3.1.4 に対応した

## 1.6.0

### UPDATE

- SDK のバージョンを 1.6.0 に上げた
- Android Studio 3.1.2 に対応した
- PermissionsDispatcher を 3.2.0 に上げた
  - lint バグフィックスにより不要な SuppressLint アノテーションを削除した
- Kotlin を 1.2.51 に上げた
- Anko を 0.10.5 に上げた
- 音量をボリュームキーから制御できるようにした
- PermissionsDispatcher を 3.3.1 に上げた

### CHANGE

- ボリューム変更対象ストリームを `STREAM_VOICE_CALL` に変更した

## 1.5.3

### UPDATE

- SDK のバージョンを 1.5.4 に上げた

## 1.5.2

### UPDATE

- SDK のバージョンを 1.5.3 に上げた
- Kotlin 1.2.31 に上げた

## 1.5.1

### UPDATE

- SDK のバージョンを 1.5.2 に上げた
- kotlin ソースディレクトリの名前を kotlin に変更した
- PermissionDispatcher 3.x に対応した
- Kotlin 1.2.30 に上げた

## 1.5.0

### UPDATE

- SDK のバージョンを 1.5.0 に上げた
- Sora プッシュ API の listener を実装した

## 1.4.2

### UPDATE

- SDK のバージョンを 1.4.1 に上げた

## 1.4.1

### UPDATE

- Android support library を 26.0.2 に上げた
- PermissionsDispatcher を 3.1.0 に上げた

## 1.4.0

### UPDATE

- SDK のバージョンを 1.4.0 に上げた
- Android Studio 3.0 に対応した
  - gradle: 4.1
  - android-maven-gradle-plugin: 2.0
- Kotlin 1.2.10 に上げた

## 1.3.1

### UPDATE

- SDK のバージョンを 1.3.1 に上げた
- Kotlin を 1.1.51 に上げた
- CircleCI でのビルドを設定した

### CHANGE

- Signaling Endpoint の設定を Config.kt から build.gradle に移動した
- パッケージ名を jp.shiguredo.samples から jp.shiguredo.sora.quickstart に変更した

## 1.3.0

### UPDATE

- SDK のバージョンを上げた
- デバッグログを出力するよう変更した

## 1.2.0

### UPDATE

- SDK のバージョンを上げた

## 1.1.0

### UPDATE

- 依存ライブラリのバージョンを上げた

### ADD

- Sora Android SDK 依存を JitPack 経由とし、AAR の手動ダウンロードを不要にした

## 1.0.0

最初のリリース
