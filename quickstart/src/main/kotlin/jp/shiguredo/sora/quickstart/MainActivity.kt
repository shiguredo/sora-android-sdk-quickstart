package jp.shiguredo.sora.quickstart

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.gson.*
import jp.shiguredo.sora.sdk.camera.CameraCapturerFactory
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.channel.signaling.message.PushMessage
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import kotlinx.android.synthetic.main.activity_main.*
import org.webrtc.*
import permissions.dispatcher.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.simpleName
    }

    private var egl: EglBase? = null
    private var oldAudioMode: Int = AudioManager.MODE_INVALID

    private var audioManager: AudioManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoraLogger.enabled = true

        setContentView(R.layout.activity_main)
        startButton.setOnClickListener {
            disableStartButton()
            startWithPermissionCheck()
        }
        stopButton.setOnClickListener {
            close()
            disableStopButton()
        }

        egl = EglBase.create()
        val eglContext = egl!!.eglBaseContext
        localRenderer?.init(eglContext, null)
        remoteRenderer?.init(eglContext, null)
        disableStopButton()

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        oldAudioMode = audioManager!!.mode
        Log.d(TAG, "AudioManager mode change: $oldAudioMode => MODE_IN_COMMUNICATION(3)")
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    override fun onResume() {
        super.onResume()
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        Log.d(TAG, "AudioManager mode change: MODE_IN_COMMUNICATION(3) => $oldAudioMode")
        audioManager?.run { mode = oldAudioMode }

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
            Log.d(TAG, "onError [$reason]")
            close()
        }

        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason, message: String) {
            SoraLogger.d(TAG, "onError [$reason]: $message")
            close()
        }

        override fun onAddRemoteStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            Log.d(TAG, "onAddRemoteStream")
            runOnUiThread {
                if (ms.videoTracks.size > 0) {
                    val track = ms.videoTracks[0]
                    track.setEnabled(true)
                    track.addSink(this@MainActivity.remoteRenderer)
                }
            }
        }

        override fun onAddLocalStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            Log.d(TAG, "onAddLocalStream")
            runOnUiThread {
                if (ms.videoTracks.size > 0) {
                    val track = ms.videoTracks[0]
                    track.setEnabled(true)
                    track.addSink(this@MainActivity.localRenderer)
                    capturer?.startCapture(400, 400, 30)
                }
            }
        }

        override fun onPushMessage(mediaChannel: SoraMediaChannel, push: PushMessage) {
            Log.d(TAG, "onPushMessage: push=${push}")
            val data = push.data
            if(data is Map<*, *>) {
                data.forEach { (key, value) ->
                    Log.d(TAG, "received push data: ${key}=${value}")
                }
            }
        }
    }

    @NeedsPermission(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun start() {
        Log.d(TAG, "start")

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
                signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java),
                mediaOption       = option,
                listener          = channelListener)
        mediaChannel!!.connect()
    }

    private fun close() {
        mediaChannel?.disconnect()
        mediaChannel = null
        capturer?.stopCapture()
    }

    private fun dispose() {
        capturer?.stopCapture()
        capturer = null

        localRenderer?.release()
        remoteRenderer?.release()

        egl?.release()
        egl = null
    }

    private fun disableStartButton() {
        stopButton.isEnabled = true
        stopButton.setBackgroundColor(Color.parseColor("#F06292"))
        startButton.isEnabled = false
        startButton.setBackgroundColor(Color.parseColor("#CCCCCC"))
    }

    private fun disableStopButton() {
        stopButton.isEnabled = false
        stopButton.setBackgroundColor(Color.parseColor("#CCCCCC"))
        startButton.isEnabled = true
        startButton.setBackgroundColor(Color.parseColor("#F06292"))
    }

    // -- PermissionDispatcher --

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @OnShowRationale(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun showRationaleForCameraAndAudio(request: PermissionRequest) {
        Log.d(TAG, "showRationaleForCameraAndAudio")
        showRationaleDialog(
                "ビデオチャットを利用するには、カメラとマイクの使用許可が必要です", request)
    }

    @OnPermissionDenied(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun onCameraAndAudioDenied() {
        Log.d(TAG, "onCameraAndAudioDenied")
        Snackbar.make(rootLayout,
                "ビデオチャットを利用するには、カメラとマイクの使用を許可してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
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
