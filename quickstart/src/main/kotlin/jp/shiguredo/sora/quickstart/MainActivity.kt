package jp.shiguredo.sora.quickstart

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
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
import org.webrtc.Camera2Enumerator
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

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
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
        binding.localRenderer.init(eglContext, null)
        binding.remoteRenderer.init(eglContext, null)
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

    private fun start() {
        Log.d(TAG, "start")

        val eglContext = egl?.eglBaseContext ?: run {
            Log.e(TAG, "EGL is not initialized")
            Snackbar.make(binding.rootLayout, "初期化に失敗しました", Snackbar.LENGTH_LONG).show()
            restoreUiOnStartFailure()
            return
        }

        val cap = createPreferredCapturer()
        if (cap == null) {
            Log.e(TAG, "Failed to create camera capturer")
            Snackbar.make(binding.rootLayout, "カメラの初期化に失敗しました", Snackbar.LENGTH_LONG).show()
            restoreUiOnStartFailure()
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

    private fun createPreferredCapturer(): CameraVideoCapturer? {
        logAvailableCameras()
        val usbCapturer = createUsbCameraCapturer()
        if (usbCapturer != null) {
            Log.d(TAG, "Using external USB camera")
            return usbCapturer
        }
        return CameraCapturerFactory.create(this)
    }

    private fun logAvailableCameras() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "Camera enumeration requires API 21+")
            return
        }

        val camera2Supported = Camera2Enumerator.isSupported(this)
        Log.d(TAG, "Camera2Enumerator.isSupported: $camera2Supported")
        if (!camera2Supported) {
            Log.d(TAG, "Camera2 enumerator not supported on this device")
            return
        }

        val manager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (manager == null) {
            Log.w(TAG, "CameraManager not available for logging")
            return
        }

        val enumerator = Camera2Enumerator(this)
        val deviceNames = enumerator.deviceNames.toList()

        Log.d(TAG, "=== Camera2 enumerator device list ===")
        deviceNames.forEach { name ->
            val facing = when {
                enumerator.isFrontFacing(name) -> "FRONT"
                enumerator.isBackFacing(name) -> "BACK"
                else -> "EXTERNAL/UNKNOWN"
            }
            Log.d(TAG, "Enumerator device: name=$name facing=$facing")
        }

        val cameraIds = try {
            manager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to get cameraIdList", e)
            return
        }

        Log.d(TAG, "=== CameraManager cameraId list ===")
        cameraIds.forEach { cameraId ->
            val characteristics = try {
                manager.getCameraCharacteristics(cameraId)
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to get characteristics for $cameraId", e)
                return@forEach
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Camera access error for $cameraId", e)
                return@forEach
            }

            val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                else -> "UNKNOWN"
            }

            val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            val hardwareLevelLabel = when (hardwareLevel) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                else -> "UNKNOWN"
            }

            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?.joinToString(prefix = "[", postfix = "]") { cap ->
                    when (cap) {
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "BACKWARD_COMPATIBLE"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "MANUAL_SENSOR"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "MANUAL_POST_PROCESSING"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "DEPTH_OUTPUT"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "BURST_CAPTURE"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "CONSTRAINED_HIGH_SPEED_VIDEO"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV_REPROCESSING"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> "PRIVATE_REPROCESSING"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MOTION_TRACKING -> "MOTION_TRACKING"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> "LOGICAL_MULTI_CAMERA"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME -> "MONOCHROME"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA -> "SECURE_IMAGE_DATA"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_SYSTEM_CAMERA -> "SYSTEM_CAMERA"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR -> "ULTRA_HIGH_RESOLUTION_SENSOR"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_REMOSAIC_REPROCESSING -> "REMOSAIC_REPROCESSING"
                        else -> "UNKNOWN($cap)"
                    }
                } ?: "[]"

            Log.d(
                TAG,
                "CameraManager device: id=$cameraId facing=$facing hardwareLevel=$hardwareLevelLabel capabilities=$capabilities"
            )
        }
    }

    private fun createUsbCameraCapturer(): CameraVideoCapturer? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "USB camera not supported below API 21")
            return null
        }

        if (!Camera2Enumerator.isSupported(this)) {
            Log.d(TAG, "Camera2 is not supported; USB camera unavailable")
            return null
        }

        val enumerator = Camera2Enumerator(this)
        val deviceNames = enumerator.deviceNames.toList()
        Log.d(TAG, "Camera2 device names: ${deviceNames.joinToString()}")

        deviceNames.forEach { name ->
            val front = enumerator.isFrontFacing(name)
            val back = enumerator.isBackFacing(name)
            Log.d(TAG, "Camera device name=$name, front=$front, back=$back")
        }

        val externalName = deviceNames.firstOrNull { name ->
            !enumerator.isFrontFacing(name) && !enumerator.isBackFacing(name)
        }

        if (externalName != null) {
            Log.d(TAG, "Using external camera from enumerator: $externalName")
            return try {
                enumerator.createCapturer(externalName, null)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to create external camera capturer", e)
                null
            }
        }

        val manager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (manager == null) {
            Log.w(TAG, "CameraManager not available")
            return null
        }

        val cameraIds = try {
            manager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to obtain camera id list", e)
            return null
        }

        cameraIds.forEach { cameraId ->
            val characteristics = try {
                manager.getCameraCharacteristics(cameraId)
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to get characteristics for $cameraId", e)
                return@forEach
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Camera access error for $cameraId", e)
                return@forEach
            }

            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (lensFacing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                val deviceName = deviceNames.firstOrNull { it == cameraId } ?: cameraId
                Log.d(TAG, "Using external camera from characteristics: id=$cameraId name=$deviceName")
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    return capturer
                } else {
                    Log.w(TAG, "Failed to create capturer for external camera: $deviceName")
                }
            }
        }

        Log.d(TAG, "External USB camera not found")
        return null
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
                "ビデオチャットを利用するには、カメラとマイクの使用許可が必要です"
            ) {
                permissionsLauncher.launch(check.missing)
            }
        } else {
            permissionsLauncher.launch(check.missing)
        }
    }

    private fun hasPermission(perm: String): Boolean =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    private data class PermissionCheck(
        val allGranted: Boolean,
        val missing: Array<String>
    )

    private fun evaluatePermissions(): PermissionCheck {
        val missing = requiredPermissions
            .filterNot(::hasPermission)
            .toTypedArray()
        return PermissionCheck(missing.isEmpty(), missing)
    }

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

    private fun showPermissionError() {
        Log.d(TAG, "showPermissionError")
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
