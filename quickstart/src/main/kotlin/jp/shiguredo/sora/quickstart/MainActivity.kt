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

    private var renderersInitialized = false

    private lateinit var binding: ActivityMainBinding

    private val requiredPermissions =
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            // 直近リクエストの結果で判定
            val allGranted = result.values.all { it == true }
            if (allGranted) {
                disableStartButton()
                start()
            } else {
                showPermissionError()
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
        // レンダラーの初期化は接続開始直前に実行する
        disableStopButton()

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { am ->
            oldAudioMode = am.mode
            Log.d(TAG, "AudioManager mode change: $oldAudioMode => MODE_IN_COMMUNICATION(3)")
            am.mode = AudioManager.MODE_IN_COMMUNICATION
        }
    }

    override fun onResume() {
        super.onResume()
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onStop() {
        super.onStop()
        // ここではリソースを解放しない（単なるバックグラウンド遷移に対応）
        // アクティビティ終了時（Back/finish）のみ解放したい場合は下記を使用:
        // if (isFinishing) { close(); dispose() }
    }

    @SuppressLint("WrongConstant")
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        // AudioManager のモードを起動前の状態に戻す。
        // oldAudioMode は初期値として MODE_INVALID を使っており、
        // これは「復元不要」を示すセンチネル。MODE_INVALID のまま代入すると
        // Lint の WrongConstant が発生し得るため、チェックしてから復元する。
        audioManager?.let { am ->
            if (oldAudioMode != AudioManager.MODE_INVALID) {
                Log.d(TAG, "AudioManager mode change: MODE_IN_COMMUNICATION(3) => $oldAudioMode")
                am.mode = oldAudioMode
            } else {
                Log.d(TAG, "AudioManager mode unchanged (oldAudioMode is MODE_INVALID)")
            }
        }

        dispose()
    }

    private var mediaChannel: SoraMediaChannel? = null
    private var capturer: CameraVideoCapturer? = null

    private val channelListener =
        object : SoraMediaChannel.Listener {
            override fun onConnect(mediaChannel: SoraMediaChannel) {
                Log.d(TAG, "onConnect")
            }

            override fun onClose(
                mediaChannel: SoraMediaChannel,
                closeEvent: SoraCloseEvent,
            ) {
                when {
                    closeEvent.code != 1000 -> Log.e(TAG, "onClose: エラーにより Sora から切断されました: $closeEvent")
                    else -> Log.i(TAG, "onClose: Sora から切断されました: $closeEvent")
                }
                close()
            }

            override fun onError(
                mediaChannel: SoraMediaChannel,
                reason: SoraErrorReason,
                message: String,
            ) {
                Log.d(TAG, "onError [$reason]: $message")
                close()
            }

            override fun onWarning(
                mediaChannel: SoraMediaChannel,
                reason: SoraErrorReason,
            ) {
                Log.d(TAG, "onWarning [$reason]")
            }

            override fun onAddRemoteStream(
                mediaChannel: SoraMediaChannel,
                ms: MediaStream,
            ) {
                Log.d(TAG, "onAddRemoteStream")
                runOnUiThread {
                    if (ms.videoTracks.size > 0) {
                        val track = ms.videoTracks[0]
                        track.setEnabled(true)
                        track.addSink(this@MainActivity.binding.remoteRenderer)
                    }
                }
            }

            override fun onRemoveRemoteStream(
                mediaChannel: SoraMediaChannel,
                label: String,
            ) {
                Log.d(TAG, "onRemoveRemoteStream")
                runOnUiThread {
                    binding.remoteRenderer.clearImage()
                }
            }

            override fun onAddLocalStream(
                mediaChannel: SoraMediaChannel,
                ms: MediaStream,
            ) {
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

            override fun onPushMessage(
                mediaChannel: SoraMediaChannel,
                push: PushMessage,
            ) {
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
                attendees: ChannelAttendeesCount,
            ) {
                Log.d(TAG, "onAttendeesCountUpdated: $attendees")
            }

            override fun onOfferMessage(
                mediaChannel: SoraMediaChannel,
                offer: OfferMessage,
            ) {
                Log.d(TAG, "onOfferMessage: offer=$offer")
            }

            override fun onNotificationMessage(
                mediaChannel: SoraMediaChannel,
                notification: NotificationMessage,
            ) {
                Log.d(TAG, "onNotificationMessage: notification=$notification")
            }
        }

    private fun start() {
        Log.d(TAG, "start")

        val eglContext =
            egl?.eglBaseContext ?: run {
                Log.e(TAG, "EGL is not initialized")
                Snackbar.make(binding.rootLayout, "初期化に失敗しました", Snackbar.LENGTH_LONG).show()
                restoreUiOnStartFailure()
                return
            }

        ensureRenderersInitialized(eglContext)

        val cap = CameraCapturerFactory.create(this)
        if (cap == null) {
            Log.e(TAG, "Failed to create camera capturer")
            Snackbar.make(binding.rootLayout, "カメラの初期化に失敗しました", Snackbar.LENGTH_LONG).show()
            restoreUiOnStartFailure()
            releaseRenderers()
            return
        }
        capturer = cap

        val option =
            SoraMediaOption().apply {
                enableAudioDownstream()
                enableVideoDownstream(eglContext)

                enableAudioUpstream()
                enableVideoUpstream(cap, eglContext)
            }

        mediaChannel =
            SoraMediaChannel(
                context = this,
                signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() },
                channelId = BuildConfig.CHANNEL_ID,
                signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java),
                mediaOption = option,
                listener = channelListener,
            )
        mediaChannel?.connect()
    }

    private fun restoreUiOnStartFailure() {
        runOnUiThread {
            // 開始失敗時は開始前の状態へ戻す（Start有効・Stop無効）
            enableStartButton()
        }
    }

    private fun enableStartButton() {
        binding.stopButton.isEnabled = false
        binding.stopButton.setBackgroundColor(Color.parseColor("#CCCCCC"))
        binding.startButton.isEnabled = true
        binding.startButton.setBackgroundColor(Color.parseColor("#F06292"))
    }

    private fun tryStartWithPermissions() {
        val check = evaluatePermissions()
        if (check.allGranted) {
            disableStartButton()
            start()
            return
        }

        val shouldShow = check.missing.any { perm -> shouldShowRequestPermissionRationale(perm) }
        if (shouldShow) {
            showRationaleDialog(
                "ビデオチャットを利用するには、カメラとマイクの使用許可が必要です",
            ) {
                permissionsLauncher.launch(check.missing)
            }
        } else {
            permissionsLauncher.launch(check.missing)
        }
    }

    private fun hasPermission(perm: String): Boolean = ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    private data class PermissionCheck(
        val allGranted: Boolean,
        val missing: Array<String>,
    )

    private fun evaluatePermissions(): PermissionCheck {
        val missing =
            requiredPermissions
                .filterNot(::hasPermission)
                .toTypedArray()
        return PermissionCheck(missing.isEmpty(), missing)
    }

    private fun close() {
        // UI 更新系処理は runOnUiThread で行う
        runOnUiThread {
            disableStopButton()
        }
        releaseRenderers()
        mediaChannel?.disconnect()
        mediaChannel = null
        capturer?.stopCapture()
        capturer = null
    }

    private fun dispose() {
        close()
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

    private fun ensureRenderersInitialized(eglContext: EglBase.Context) {
        if (renderersInitialized) {
            return
        }
        binding.localRenderer.init(eglContext, null)
        binding.remoteRenderer.init(eglContext, null)
        renderersInitialized = true
    }

    private fun releaseRenderers() {
        if (!renderersInitialized) {
            return
        }
        binding.localRenderer.release()
        binding.remoteRenderer.release()
        renderersInitialized = false
    }

    private fun showPermissionError() {
        Log.d(TAG, "showPermissionError")
        Snackbar
            .make(
                binding.rootLayout,
                "ビデオチャットを利用するには、カメラとマイクの使用を許可してください",
                Snackbar.LENGTH_LONG,
            ).setAction("OK") { }
            .show()
    }

    private fun showRationaleDialog(
        message: String,
        onProceed: () -> Unit,
    ) {
        AlertDialog
            .Builder(this)
            .setPositiveButton(getString(R.string.permission_button_positive)) { _, _ -> onProceed() }
            .setNegativeButton(getString(R.string.permission_button_negative)) { _, _ -> }
            .setCancelable(false)
            .setMessage(message)
            .show()
    }
}
