package jp.shiguredo.quickstart

import android.os.Build

class Conf {

    companion object {
        const val SIGNALING_ENDPOINT = BuildConfig.SIGNALING_ENDPOINT
        const val CHANNEL_ID = "sora"
    }
}
