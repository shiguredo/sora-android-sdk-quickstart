package jp.shiguredo.sora.quickstart

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import jp.shiguredo.sora.sdk.camera.CameraCapturerFactory
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.jetbrains.anko.*
import org.webrtc.*
import permissions.dispatcher.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.simpleName

    private var egl: EglBase? = null
    private var ui: MainActivityUI? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoraLogger.enabled = true

        egl = EglBase.create()

        ui = MainActivityUI()
        ui?.setContentView(this)
        ui?.init(egl!!.eglBaseContext)
    }

    override fun onDestroy() {
        Log.d(TAG, "onPause")
        super.onPause()
        close()
        dispose()
    }

    private var mediaChannel: SoraMediaChannel? = null
    private var capturer: CameraVideoCapturer? = null

    private val channelListener = object : SoraMediaChannel.Listener {

        override fun onConnect(mediaChannel: SoraMediaChannel) {
            Log.d(TAG, "onConnect")
        }

        override fun onClose(mediaChannel: SoraMediaChannel) {
            Log.d(TAG, "onClose")
            close()
        }

        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason) {
            Log.d(TAG, "onError")
            close()
        }

        override fun onAddRemoteStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            Log.d(TAG, "onAddRemoteStream")
            runOnUiThread {
                if (ms.videoTracks.size > 0) {
                    val track = ms.videoTracks[0]
                    track.setEnabled(true)
                    track.addRenderer(VideoRenderer(ui!!.remoteSurfaceRenderer!!))
                }
            }
        }

        override fun onAddLocalStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            Log.d(TAG, "onAddLocalStream")
            runOnUiThread {
                if (ms.videoTracks.size > 0) {
                    val track = ms.videoTracks[0]
                    track.setEnabled(true)
                    track.addRenderer(VideoRenderer(ui!!.localSurfaceRenderer!!))
                    capturer?.startCapture(400, 400, 30)
                }
            }
        }
    }

    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    fun start() {

        capturer = CameraCapturerFactory.create(this)

        val option = SoraMediaOption().apply {

            enableAudioDownstream()
            enableVideoDownstream(egl!!.eglBaseContext)

            enableAudioUpstream()
            enableVideoUpstream(capturer!!, egl!!.eglBaseContext)

            enableMultistream()
        }

        mediaChannel = SoraMediaChannel(
                context           = this,
                signalingEndpoint = BuildConfig.SIGNALING_ENDPOINT,
                channelId         = BuildConfig.CHANNEL_ID,
                mediaOption       = option,
                listener          = channelListener)
        mediaChannel!!.connect()
    }

    fun close() {
        mediaChannel?.disconnect()
        mediaChannel = null
        capturer?.stopCapture()
    }

    fun dispose() {
        capturer?.stopCapture()
        capturer = null
        ui?.releaseRenderers()
        egl?.release()
        egl = null
    }

    // UI events
    fun onStartButtonClicked() {
        startWithPermissionCheck()
    }

    fun onStopButtonClicked() {
        close()
    }

    // 以下 PermissionDispatcher用

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForCamera(request: PermissionRequest) {
        Log.d(TAG, "showRationalForCamera")
        showRationaleDialog(getString(R.string.permission_rationale_camera), request)
    }

    @OnShowRationale(Manifest.permission.RECORD_AUDIO)
    fun showRationaleForAudio(request: PermissionRequest) {
        Log.d(TAG, "showRationalForAudio")
        showRationaleDialog(getString(R.string.permission_rationale_record_audio), request)
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraDenied() {
        Snackbar.make(this.contentView!!,
                getString(R.string.permission_denied_camera),
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    fun onAudioDenied() {
        Snackbar.make(this.contentView!!,
                getString(R.string.permission_denied_record_audio),
                Snackbar.LENGTH_LONG)
                .setAction("OK") {  }
                .show()
    }

    private fun showRationaleDialog(message: String, request: PermissionRequest) {
        AlertDialog.Builder(this)
                .setPositiveButton(getString(R.string.permission_button_positive)) { _, _ -> request.proceed() }
                .setNegativeButton(getString(R.string.permission_button_negative)) { _, _ -> request.cancel() }
                .setCancelable(false)
                .setMessage(message)
                .show()
    }
}

class MainActivityUI : AnkoComponent<MainActivity> {

    val TAG = MainActivityUI::class.simpleName

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var startButton: Button? = null
    private var stopButton: Button? = null

    val localSurfaceRenderer: SurfaceViewRenderer?
        get() = localRenderer

    val remoteSurfaceRenderer: SurfaceViewRenderer?
        get() = remoteRenderer

    override fun createView(ui: AnkoContext<MainActivity>): View = with(ui) {

        return verticalLayout {

            padding = dip(6)
            lparams(width = matchParent, height = matchParent)

            startButton = button("START") {

                lparams {

                    width = matchParent
                    height = wrapContent
                    margin = dip(10)
                }

                backgroundColor = Color.parseColor("#F06292")
                textColor = Color.WHITE

                onClick {
                    ui.owner.onStartButtonClicked()
                    disableStartButton()
                }
            }

            stopButton = button("STOP") {

                lparams {

                    width = matchParent
                    height = wrapContent
                    margin = dip(10)
                }

                backgroundColor = Color.parseColor("#F06292")
                textColor = Color.WHITE

                onClick {
                    ui.owner.onStopButtonClicked()
                    disableStopButton()
                }
            }

            localRenderer = surfaceViewRenderer {

                lparams {

                    width = 400
                    height = 400
                    margin = dip(10)
                }
            }

            remoteRenderer = surfaceViewRenderer {

                lparams {

                    width = 400
                    height = 400
                    margin = dip(10)
                }
            }
        }
    }

    fun disableStartButton() {
        stopButton?.enabled = true
        stopButton?.backgroundColor = Color.parseColor("#F06292")
        startButton?.enabled = false
        startButton?.backgroundColor = Color.parseColor("#CCCCCC")
    }

    fun disableStopButton() {
        stopButton?.enabled = false
        stopButton?.backgroundColor = Color.parseColor("#CCCCCC")
        startButton?.enabled = true
        startButton?.backgroundColor = Color.parseColor("#F06292")
    }

    fun init(eglContext: EglBase.Context) {
        localRenderer?.init(eglContext, null)
        remoteRenderer?.init(eglContext, null)
        disableStopButton()
    }

    fun releaseRenderers() {
        localRenderer?.release()
        remoteRenderer?.release()
    }
}
