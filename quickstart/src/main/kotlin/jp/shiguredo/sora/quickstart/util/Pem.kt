package jp.shiguredo.sora.quickstart.util

internal fun String.unescapePem(): String =
    replace("\\n", "\n")
        .replace("\\r", "\r")
