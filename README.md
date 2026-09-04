# Sora Android SDK クイックスタート

このリポジトリでは、 Sora Android SDK を利用するシンプルな Android アプリケーションを提供します。

このアプリケーションは、 Sora JavaScript SDK のステレオ音声を受信して確認するためのサンプルです。
音声のみの `recvonly` 接続を確立し、受信した PCM の L / R の RMS 値と波形を表示します。
それは、素の `SoraMediaChannel` を使ったサンプルになっています。

## ステレオ音声受信の検証

ステレオ音声受信の検証には、開発中の Sora Android SDK を composite build で指定します。

```console
$ SORA_SDK_DIR=../sora-android-sdk ./gradlew :quickstart:installDebug
```

`gradle.properties` にシグナリングエンドポイントとチャネル ID を設定してからアプリケーションを起動し、 `START` を押してください。

送信側は Sora JavaScript SDK の `e2e-tests/fake_stereo_audio` ページを使用します。

```console
$ cd ../sora-js-sdk
$ pnpm run e2e-dev -- --port 9000
```

ブラウザで `http://localhost:9000/fake_stereo_audio/` を開き、同じチャネル ID のまま `Use Stereo Audio (Fake Generator)` を有効にして `connect` を押します。

Android 側では `channels: 2`、 L / R の RMS 値、 `L - R RMS`、 L / R / L - R の波形を確認できます。

## About Support

We check PRs or Issues only when written in JAPANESE.
In other languages, we won't be able to deal with them. Thank you for your understanding.

## 時雨堂のオープンソースソフトウェアについて

利用前に https://github.com/shiguredo/oss をお読みください。

## Sora Android SDK

Sora Android SDK は [WebRTC SFU Sora](https://sora.shiguredo.jp) の
Android クライアントアプリケーションを開発するためのライブラリです。
Sora Android SDK の使い方は
[Sora Android SDK ドキュメント](https://sora-android-sdk.shiguredo.jp/)
を参照してください。

## システム条件

- Android 5.0 以降 (エミュレーターでの動作は保証しません)
- Android Studio 2025.3.1 以降
- WebRTC SFU Sora 2025.2.0 以降

## 参考リンク

- [Sora Android SDK ドキュメント](https://sora-android-sdk.shiguredo.jp/)
- [サンプル集](https://github.com/shiguredo/sora-android-sdk-samples)

## Copyright

Copyright 2023 Shiguredo Inc. and Lyo Kato <lyo.kato at gmail.com>
