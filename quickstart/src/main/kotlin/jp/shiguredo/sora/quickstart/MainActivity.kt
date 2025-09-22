package jp.shiguredo.sora.quickstart

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import jp.shiguredo.sora.quickstart.databinding.ActivityMainBinding
import jp.shiguredo.sora.sdk.camera.CameraCapturerFactory
import jp.shiguredo.sora.sdk.channel.SoraCloseEvent
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.channel.signaling.message.NotificationMessage
import jp.shiguredo.sora.sdk.channel.signaling.message.OfferMessage
import jp.shiguredo.sora.sdk.channel.signaling.message.PushMessage
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.MediaStream
class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.simpleName
    }

    private var egl: EglBase? = null
    private var oldAudioMode: Int = AudioManager.MODE_INVALID

    private var audioManager: AudioManager? = null

    private lateinit var binding: ActivityMainBinding

    // -- Permissions (Activity Result API) --
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = requiredPermissions.all { hasPermission(it) }
            if (allGranted) {
                disableStartButton()
                start()
            } else {
                onCameraAndAudioDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoraLogger.enabled = true

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.startButton.setOnClickListener { tryStartWithPermissions() }
        binding.stopButton.setOnClickListener {
            close()
        }

        egl = EglBase.create()
        val eglContext = egl?.eglBaseContext
        if (eglContext == null) {
            Log.e(TAG, "EGL context initialization failed")
            Snackbar.make(binding.rootLayout, "初期化に失敗しました", Snackbar.LENGTH_LONG).show()
            finish()
            return
        }
        binding.localRenderer.init(eglContext, null)
        binding.remoteRenderer.init(eglContext, null)
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

    override fun onStop() {
        super.onStop()
        // 異常終了やバックグラウンド遷移時のリーク抑止
        close()
        dispose()
    }

    // AudioManager.MODE_INVALID が使われているため lint でエラーが出るので一時的に抑制しておく
    @SuppressLint("WrongConstant")
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

        override fun onClose(mediaChannel: SoraMediaChannel, closeEvent: SoraCloseEvent) {
            when {
                closeEvent.code != 1000 -> Log.e(TAG, "onClose: エラーにより Sora から切断されました: $closeEvent")
                else -> Log.i(TAG, "onClose: Sora から切断されました: $closeEvent")
            }
            close()
        }

        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason, message: String) {
            Log.d(TAG, "onError [$reason]: $message")
            close()
        }

        override fun onWarning(mediaChannel: SoraMediaChannel, reason: SoraErrorReason) {
            Log.d(TAG, "onWarning [$reason]")
        }

        override fun onAddRemoteStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            Log.d(TAG, "onAddRemoteStream")
            runOnUiThread {
                if (ms.videoTracks.size > 0) {
                    val track = ms.videoTracks[0]
                    track.setEnabled(true)
                    track.addSink(this@MainActivity.binding.remoteRenderer)
                }
            }
        }

        override fun onRemoveRemoteStream(mediaChannel: SoraMediaChannel, label: String) {
            Log.d(TAG, "onRemoveRemoteStream")
            runOnUiThread {
                binding.remoteRenderer.clearImage()
            }
        }

        override fun onAddLocalStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            Log.d(TAG, "onAddLocalStream")
            runOnUiThread {
                if (ms.videoTracks.size > 0) {
                    val track = ms.videoTracks[0]
                    track.setEnabled(true)
                    track.addSink(this@MainActivity.binding.localRenderer)
                    capturer?.startCapture(400, 400, 30)
                }
            }
        }

        override fun onPushMessage(mediaChannel: SoraMediaChannel, push: PushMessage) {
            Log.d(TAG, "onPushMessage: push=$push")
            val data = push.data
            if (data is Map<*, *>) {
                data.forEach { (key, value) ->
                    Log.d(TAG, "received push data: $key=$value")
                }
            }
        }

        override fun onAttendeesCountUpdated(
            mediaChannel: SoraMediaChannel,
            attendees: ChannelAttendeesCount
        ) {
            Log.d(TAG, "onAttendeesCountUpdated: $attendees")
        }

        override fun onOfferMessage(mediaChannel: SoraMediaChannel, offer: OfferMessage) {
            Log.d(TAG, "onOfferMessage: offer=$offer")
        }

        override fun onNotificationMessage(
            mediaChannel: SoraMediaChannel,
            notification: NotificationMessage
        ) {
            Log.d(TAG, "onNotificationMessage: notification=$notification")
        }
    }

    fun start() {
        Log.d(TAG, "start")

        val eglContext = egl?.eglBaseContext ?: run {
            Log.e(TAG, "EGL is not initialized")
            Snackbar.make(binding.rootLayout, "初期化に失敗しました", Snackbar.LENGTH_LONG).show()
            return
        }

        val cap = CameraCapturerFactory.create(this)
        if (cap == null) {
            Log.e(TAG, "Failed to create camera capturer")
            Snackbar.make(binding.rootLayout, "カメラの初期化に失敗しました", Snackbar.LENGTH_LONG).show()
            return
        }
        capturer = cap

        val option = SoraMediaOption().apply {
            enableAudioDownstream()
            enableVideoDownstream(eglContext)

            enableAudioUpstream()
            enableVideoUpstream(cap, eglContext)
        }

        mediaChannel = SoraMediaChannel(
            context = this,
            signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() },
            channelId = BuildConfig.CHANNEL_ID,
            signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java),
            mediaOption = option,
            listener = channelListener
        )
        mediaChannel?.connect()
    }

    private fun tryStartWithPermissions() {
        val missing = missingPermissions()
        if (missing.isEmpty()) {
            disableStartButton()
            start()
            return
        }

        val shouldShow = missing.any { perm -> shouldShowRequestPermissionRationale(perm) }
        if (shouldShow) {
            showRationaleDialog(
                "ビデオチャットを利用するには、カメラとマイクの使用許可が必要です"
            ) {
                permissionsLauncher.launch(missing.toTypedArray())
            }
        } else {
            permissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun hasPermission(perm: String): Boolean =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    private fun missingPermissions(): List<String> =
        requiredPermissions.filterNot { hasPermission(it) }

    private fun close() {
        // UI 更新系処理は runOnUiThread で行う
        runOnUiThread {
            disableStopButton()
        }
        mediaChannel?.disconnect()
        mediaChannel = null
        capturer?.stopCapture()
    }

    private fun dispose() {
        capturer?.stopCapture()
        capturer = null

        binding.localRenderer.release()
        binding.remoteRenderer.release()

        egl?.release()
        egl = null
    }

    private fun disableStartButton() {
        binding.stopButton.isEnabled = true
        binding.stopButton.setBackgroundColor(Color.parseColor("#F06292"))
        binding.startButton.isEnabled = false
        binding.startButton.setBackgroundColor(Color.parseColor("#CCCCCC"))
    }

    private fun disableStopButton() {
        binding.stopButton.isEnabled = false
        binding.stopButton.setBackgroundColor(Color.parseColor("#CCCCCC"))
        binding.startButton.isEnabled = true
        binding.startButton.setBackgroundColor(Color.parseColor("#F06292"))
    }

    private fun onCameraAndAudioDenied() {
        Log.d(TAG, "onCameraAndAudioDenied")
        Snackbar.make(
            binding.rootLayout,
            "ビデオチャットを利用するには、カメラとマイクの使用を許可してください",
            Snackbar.LENGTH_LONG
        )
            .setAction("OK") { }
            .show()
    }

    private fun showRationaleDialog(message: String, onProceed: () -> Unit) {
        AlertDialog.Builder(this)
            .setPositiveButton(getString(R.string.permission_button_positive)) { _, _ -> onProceed() }
            .setNegativeButton(getString(R.string.permission_button_negative)) { _, _ -> }
            .setCancelable(false)
            .setMessage(message)
            .show()
    }
}
