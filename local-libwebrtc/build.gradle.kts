plugins { base }

configurations { maybeCreate("default") }

artifacts {
    add("default", file("libwebrtc.aar")) {
        type = "aar"
    }
}
