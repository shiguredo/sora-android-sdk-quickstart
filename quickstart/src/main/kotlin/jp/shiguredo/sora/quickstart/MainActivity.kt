package jp.shiguredo.sora.quickstart

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import jp.shiguredo.sora.quickstart.databinding.ActivityMainBinding
import jp.shiguredo.sora.sdk.camera.CameraCapturerFactory
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.channel.signaling.message.OfferMessage
import jp.shiguredo.sora.sdk.channel.signaling.message.PushMessage
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.MediaStream
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.RuntimePermissions
import java.nio.ByteBuffer

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.simpleName
    }

    private var egl: EglBase? = null
    private var oldAudioMode: Int = AudioManager.MODE_INVALID

    private var audioManager: AudioManager? = null

    private lateinit var binding: ActivityMainBinding

    private var headerLengthMap: MutableMap<String, Int> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoraLogger.enabled = true

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.startButton.setOnClickListener {
            disableStartButton()
            startWithPermissionCheck()
        }
        binding.stopButton.setOnClickListener {
            close()
            disableStopButton()
        }

        egl = EglBase.create()
        val eglContext = egl!!.eglBaseContext
        binding.localRenderer?.init(eglContext, null)
        binding.remoteRenderer?.init(eglContext, null)
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
                    track.addSink(this@MainActivity.binding.remoteRenderer)
                }
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

        override fun onOfferMessage(mediaChannel: SoraMediaChannel, offer: OfferMessage) {
            Log.d("kensaku", "onOfferMessage: offer=${offer.dataChannels}")

            val dataChannels = offer.dataChannels
            // dataChannels の null チェック
            if (dataChannels != null) {
                for (dataChannel in dataChannels) {
                    // label の 先頭に # がついているものがメッセージ送信用
                    // # がついていないものは処理しない
                    val label = dataChannel["label"] as String
                    if (!label.startsWith("#")) {
                        continue
                    }

                    // dataChannel に header (List<Map<String, Any>>) がある場合は
                    // header list の中から type: sender_connection_id の Map を取得し
                    // length の値を取得する
                    // NOTE: 2024.2.0 の Sora では sender_connection_id のみ対応しているので
                    // このような単純な実装となっている
                    val header = dataChannel["header"] as? List<*>?
                    if (header != null) {
                        for (headerMap in header) {
                            if (headerMap is Map<*, *>) {
                                if (headerMap["type"] == "sender_connection_id") {
                                    // double で取得されるため int に変換する
                                    val length = headerMap["length"] as Double
                                    headerLengthMap[label] = length.toInt()
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun onDataChannelMessage(
            mediaChannel: SoraMediaChannel,
            label: String,
            data: ByteBuffer
        ) {
            // headerLengthMap に label が存在するか確認する
            // 存在する場合はその length を取得し、以下のように分割処理する
            // 1. 先頭から length バイト分が sender_connection_id
            // 2. 残りがメッセージ本体
            if (headerLengthMap.containsKey(label)) {
                val length = headerLengthMap[label]!!
                val senderConnectionId = ByteArray(length)
                data.get(senderConnectionId)
                val message = ByteArray(data.remaining())
                data.get(message)
                // String() で utf-8 文字列に変換する
                Log.d("kensaku", "received data: label=$label, sender_connection_id=${String(senderConnectionId)}, message=${String(message)}")
            } else {
                Log.d(TAG, "received data: label=$label, message=${mediaChannel.dataToString(data)}")
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

        val dataChannels = listOf(
            mapOf(
                "label" to "#spam",
                "direction" to "sendrecv",
            ),
            mapOf(
                "label" to "#egg",
                "max_retransmits" to 10,
                "ordered" to false,
                "protocol" to "bob",
                "compress" to false,
                "direction" to "recvonly",
                "header" to listOf(
                    mapOf(
                        "type" to "sender_connection_id"
                    )
                )
            ),
        )

        mediaChannel = SoraMediaChannel(
            context = this,
            signalingEndpoint = BuildConfig.SIGNALING_ENDPOINT,
            channelId = BuildConfig.CHANNEL_ID,
            signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java),
            dataChannelSignaling = true,
            ignoreDisconnectWebSocket = false,
            dataChannels = dataChannels,
            mediaOption = option,
            listener = channelListener
        )
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

        binding.localRenderer?.release()
        binding.remoteRenderer?.release()

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

    // -- PermissionDispatcher --

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @OnShowRationale(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun showRationaleForCameraAndAudio(request: PermissionRequest) {
        Log.d(TAG, "showRationaleForCameraAndAudio")
        showRationaleDialog(
            "ビデオチャットを利用するには、カメラとマイクの使用許可が必要です", request
        )
    }

    @OnPermissionDenied(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun onCameraAndAudioDenied() {
        Log.d(TAG, "onCameraAndAudioDenied")
        Snackbar.make(
            binding.rootLayout,
            "ビデオチャットを利用するには、カメラとマイクの使用を許可してください",
            Snackbar.LENGTH_LONG
        )
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
