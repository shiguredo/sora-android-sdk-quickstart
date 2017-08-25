package jp.shiguredo.quickstart

import android.view.ViewManager
import org.jetbrains.anko.custom.ankoView
import org.webrtc.SurfaceViewRenderer

public inline fun ViewManager.surfaceViewRenderer(theme: Int = 0) = surfaceViewRenderer(theme) {}
public inline fun ViewManager.surfaceViewRenderer(theme: Int = 0, init: SurfaceViewRenderer.() -> Unit) =
        ankoView({ SurfaceViewRenderer(it) }, theme, init)