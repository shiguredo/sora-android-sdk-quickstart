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
import jp.shiguredo.sora.sdk.util.SoraLogger
import jp.shiguredo.sora.sdk2.*
import kotlinx.android.synthetic.main.activity_main.*
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.VideoCapturer
import permissions.dispatcher.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.simpleName
    }

    private var oldAudioMode: Int = AudioManager.MODE_NORMAL
    private var audioManager: AudioManager? = null
    private var configuration: Configuration? = null
    private var mediaChannel: MediaChannel? = null
    private var capturer: VideoCapturer? = null

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
    }

    @NeedsPermission(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun start() {
        Log.d(TAG, "start")

        configuration = Configuration(
                context = this,
                url = BuildConfig.SIGNALING_ENDPOINT,
                channelId = BuildConfig.CHANNEL_ID,
                role = Role.SENDONLY).apply {
            multistreamEnabled = true
            videoBitRate = 5000
        }

        Sora.connect(configuration!!) { result ->
            mediaChannel = result.getOrNull()
            if (mediaChannel == null)
                return@connect

            capturer = configuration!!.videoCapturer
            capturer!!.changeCaptureFormat(1920, 1080, 30)
            val localStream = mediaChannel!!.streams.firstOrNull()
            if (localStream != null) {
                localStream.videoRenderer = localRenderer
            }

            mediaChannel!!.onAddRemoteStream { stream ->
                stream.videoRenderer = remoteRenderer
            }
        }
    }

    private fun close() {
        mediaChannel?.disconnect()
        mediaChannel = null
        capturer?.stopCapture()
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
