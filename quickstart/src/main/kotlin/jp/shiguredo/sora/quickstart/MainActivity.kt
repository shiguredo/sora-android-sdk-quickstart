package jp.shiguredo.sora.quickstart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import jp.shiguredo.sora.quickstart.databinding.ActivityMainBinding
import jp.shiguredo.sora.quickstart.util.unescapePem
import jp.shiguredo.sora.sdk.channel.SoraCloseEvent
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraChannelRole
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.channel.signaling.message.NotificationMessage
import jp.shiguredo.sora.sdk.channel.signaling.message.OfferMessage
import jp.shiguredo.sora.sdk.channel.signaling.message.PushMessage
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.AudioTrack
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import java.util.Locale

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = MainActivity::class.simpleName
        private const val UI_UPDATE_INTERVAL_MILLIS = 100L
    }

    private lateinit var binding: ActivityMainBinding

    @Volatile
    private var mediaChannel: SoraMediaChannel? = null

    private var audioManager: AudioManager? = null
    private var oldAudioMode: Int = AudioManager.MODE_INVALID

    private val audioTrackLock = Any()
    private val audioTrackObservations = mutableMapOf<String, AudioTrackObservation>()
    private var activeAudioTrackId: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val uiUpdater =
        object : Runnable {
            override fun run() {
                updateAnalysisUi()
                mainHandler.postDelayed(this, UI_UPDATE_INTERVAL_MILLIS)
            }
        }

    private data class AudioTrackObservation(
        val trackId: String,
        val track: AudioTrack,
        val analyzer: StereoAudioAnalyzer,
        val streamId: String,
    )

    private val channelListener =
        object : SoraMediaChannel.Listener {
            override fun onConnect(mediaChannel: SoraMediaChannel) {
                Log.d(TAG, "onConnect: recvonly audio channel")
            }

            override fun onClose(
                mediaChannel: SoraMediaChannel,
                closeEvent: SoraCloseEvent,
            ) {
                when {
                    closeEvent.code != 1000 -> Log.e(TAG, "onClose: エラーにより Sora から切断されました: $closeEvent")
                    else -> Log.i(TAG, "onClose: Sora から切断されました: $closeEvent")
                }
                close(mediaChannel)
            }

            override fun onError(
                mediaChannel: SoraMediaChannel,
                reason: SoraErrorReason,
                message: String,
            ) {
                Log.e(TAG, "onError [$reason]: $message")
                close(mediaChannel)
            }

            override fun onWarning(
                mediaChannel: SoraMediaChannel,
                reason: SoraErrorReason,
            ) {
                Log.d(TAG, "onWarning [$reason]")
            }

            override fun onAddRemoteTrack(
                mediaChannel: SoraMediaChannel,
                track: MediaStreamTrack,
                streamId: String,
            ) {
                if (this@MainActivity.mediaChannel !== mediaChannel) {
                    Log.d(TAG, "onAddRemoteTrack: 古いチャネルからの通知を無視します")
                    return
                }

                val audioTrack = track as? AudioTrack
                if (audioTrack == null) {
                    Log.w(TAG, "onAddRemoteTrack: audio track ではないため無視します: trackId=${track.id()}")
                    return
                }

                val trackId = audioTrack.id()
                val observation =
                    AudioTrackObservation(
                        trackId = trackId,
                        track = audioTrack,
                        analyzer = StereoAudioAnalyzer(),
                        streamId = streamId,
                    )
                var becameActive = false
                var attached = false

                synchronized(audioTrackLock) {
                    val previousObservation = audioTrackObservations.put(trackId, observation)
                    previousObservation?.let { detachAudioTrackLocked(it) }

                    runCatching {
                        audioTrack.setEnabled(true)
                        audioTrack.addSink(observation.analyzer)
                    }.onSuccess {
                        attached = true
                        if (activeAudioTrackId == null) {
                            activeAudioTrackId = trackId
                            becameActive = true
                        }
                    }.onFailure { throwable ->
                        audioTrackObservations.remove(trackId)
                        if (activeAudioTrackId == trackId) {
                            activeAudioTrackId = audioTrackObservations.keys.firstOrNull()
                        }
                        observation.analyzer.stop()
                        Log.e(TAG, "AudioTrackSink の接続に失敗しました: trackId=$trackId", throwable)
                    }
                }

                if (!attached) {
                    return
                }

                Log.d(
                    TAG,
                    "onAddRemoteTrack: audio track attached, trackId=$trackId, " +
                        "streamId=$streamId, active=$becameActive",
                )
            }

            override fun onRemoveRemoteTrack(
                mediaChannel: SoraMediaChannel,
                trackId: String,
                streamId: String,
            ) {
                if (this@MainActivity.mediaChannel !== mediaChannel) {
                    Log.d(TAG, "onRemoveRemoteTrack: 古いチャネルからの通知を無視します")
                    return
                }

                var removedObservation: AudioTrackObservation? = null
                var newActiveObservation: AudioTrackObservation? = null

                synchronized(audioTrackLock) {
                    removedObservation = audioTrackObservations.remove(trackId)
                    removedObservation?.let { detachAudioTrackLocked(it) }

                    if (activeAudioTrackId == trackId) {
                        activeAudioTrackId = audioTrackObservations.keys.firstOrNull()
                        newActiveObservation = activeAudioTrackId?.let { audioTrackObservations[it] }
                    }
                }

                Log.d(
                    TAG,
                    "onRemoveRemoteTrack: audio track detached, trackId=$trackId, " +
                        "streamId=$streamId, found=${removedObservation != null}",
                )
                if (newActiveObservation == null && removedObservation != null) {
                    updateAnalysisUiOnMainThread()
                }
            }

            override fun onAddLocalStream(
                mediaChannel: SoraMediaChannel,
                ms: MediaStream,
            ) {
                Log.w(TAG, "onAddLocalStream: recvonly 設定では想定外のコールバックです")
            }

            override fun onPushMessage(
                mediaChannel: SoraMediaChannel,
                push: PushMessage,
            ) {
                Log.d(TAG, "onPushMessage: push=$push")
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoraLogger.enabled = true

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.startButton.setOnClickListener { start() }
        binding.stopButton.setOnClickListener { close() }

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { manager ->
            oldAudioMode = manager.mode
            if (manager.mode != AudioManager.MODE_NORMAL) {
                Log.d(TAG, "AudioManager mode change: ${manager.mode} => MODE_NORMAL")
                manager.mode = AudioManager.MODE_NORMAL
            }
        }

        disableStopButton()
        mainHandler.post(uiUpdater)
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    @SuppressLint("WrongConstant")
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mainHandler.removeCallbacks(uiUpdater)
        close()

        audioManager?.let { manager ->
            if (oldAudioMode != AudioManager.MODE_INVALID && manager.mode != oldAudioMode) {
                Log.d(TAG, "AudioManager mode restore: ${manager.mode} => $oldAudioMode")
                manager.mode = oldAudioMode
            }
        }

        super.onDestroy()
    }

    private fun start() {
        if (mediaChannel != null) {
            return
        }

        disableStartButton()
        binding.statusTextView.text = "接続中"
        binding.trackInfoTextView.text = "受信トラック: 待機中"

        var channel: SoraMediaChannel? = null
        runCatching {
            val option =
                SoraMediaOption().apply {
                    role = SoraChannelRole.RECVONLY
                    enableAudioDownstream()
                    audioOption.useStereoOutput = true
                    configureStereoAudioAttributes(audioOption)
                }

            channel =
                SoraMediaChannel(
                    context = this,
                    signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() },
                    channelId = BuildConfig.CHANNEL_ID,
                    signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java),
                    mediaOption = option,
                    listener = channelListener,
                    caCertificate =
                        BuildConfig.CA_CERTIFICATE_PEM
                            .unescapePem()
                            .trim()
                            .ifBlank { null },
                    clientCertificate =
                        BuildConfig.CLIENT_CERTIFICATE_PEM
                            .unescapePem()
                            .trim()
                            .ifBlank { null },
                    clientPrivateKey =
                        BuildConfig.CLIENT_PRIVATE_KEY_PEM
                            .unescapePem()
                            .trim()
                            .ifBlank { null },
                )
            mediaChannel = channel
            channel?.connect()
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to start recvonly media channel", throwable)
            mediaChannel = null
            detachAudioTracks()
            channel?.let { failedChannel ->
                runCatching { failedChannel.disconnect() }
            }
            restoreUiOnStartFailure("接続開始に失敗しました")
        }
    }

    private fun close(expectedChannel: SoraMediaChannel? = null) {
        val channel = mediaChannel
        if (expectedChannel != null && channel !== expectedChannel) {
            Log.d(TAG, "close: すでに別のチャネルへ切り替わっているため無視します")
            return
        }
        mediaChannel = null
        detachAudioTracks()
        channel?.disconnect()
        runOnUiThread {
            disableStopButton()
            binding.statusTextView.text = "未接続"
            binding.trackInfoTextView.text = "受信トラック: なし"
            binding.metricsTextView.text = "L RMS: -\nR RMS: -\nR / L: -\nL - R RMS: -"
            binding.waveformView.clear()
        }
    }

    private fun detachAudioTracks() {
        synchronized(audioTrackLock) {
            audioTrackObservations.values.toList().forEach { detachAudioTrackLocked(it) }
            audioTrackObservations.clear()
            activeAudioTrackId = null
        }
    }

    private fun detachAudioTrackLocked(observation: AudioTrackObservation) {
        runCatching { observation.track.removeSink(observation.analyzer) }
            .onFailure { throwable ->
                Log.w(TAG, "AudioTrackSink の解除に失敗しました: trackId=${observation.trackId}", throwable)
            }
        observation.analyzer.stop()
    }

    private fun updateAnalysisUiOnMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateAnalysisUi()
        } else {
            runOnUiThread { updateAnalysisUi() }
        }
    }

    private fun updateAnalysisUi() {
        val observation =
            synchronized(audioTrackLock) {
                activeAudioTrackId?.let { audioTrackObservations[it] }
            }
        val snapshot = observation?.analyzer?.snapshot() ?: StereoAudioSnapshot.empty()

        binding.waveformView.setSnapshot(snapshot)

        if (observation == null) {
            binding.statusTextView.text = if (mediaChannel == null) "未接続" else "接続済み、受信トラック待ち"
            binding.trackInfoTextView.text = "受信トラック: なし"
            binding.metricsTextView.text = "L RMS: -\nR RMS: -\nR / L: -\nL - R RMS: -"
            return
        }

        binding.statusTextView.text =
            when (snapshot.status) {
                AudioAnalysisStatus.STEREO -> "ステレオ PCM 受信中"
                AudioAnalysisStatus.MONO -> "モノラル PCM 受信中"
                AudioAnalysisStatus.UNSUPPORTED_FORMAT -> "未対応の PCM 形式"
                AudioAnalysisStatus.INVALID_DATA -> "PCM データ不正"
                AudioAnalysisStatus.NO_DATA -> "トラック接続済み、PCM 待機中"
            }
        binding.trackInfoTextView.text =
            "trackId: ${observation.track.id()}\n" +
            "streamId: ${observation.streamId}\n" +
            "channels: ${snapshot.numberOfChannels}, " +
            "sample rate: ${snapshot.sampleRate} Hz, " +
            "bits: ${snapshot.bitsPerSample}, " +
            "frames: ${snapshot.totalFrames}"

        val ratio =
            if (snapshot.rmsLeft != null && snapshot.rmsRight != null && snapshot.rmsLeft > 0.0) {
                snapshot.rmsRight / snapshot.rmsLeft
            } else {
                null
            }
        binding.metricsTextView.text =
            "L RMS: ${formatValue(snapshot.rmsLeft)}\n" +
            "R RMS: ${formatValue(snapshot.rmsRight)}\n" +
            "R / L: ${formatValue(ratio)}\n" +
            "L - R RMS: ${formatValue(snapshot.differenceRms)}"
    }

    private fun restoreUiOnStartFailure(message: String) {
        runOnUiThread {
            disableStopButton()
            binding.statusTextView.text = message
        }
    }

    private fun configureStereoAudioAttributes(audioOption: Any) {
        // audioAttributes は開発中の SDK で追加された API のため、公開済み SDK でもビルドできるよう反射で設定する。
        val audioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

        runCatching {
            audioOption.javaClass
                .getMethod("setAudioAttributes", AudioAttributes::class.java)
                .invoke(audioOption, audioAttributes)
        }.onSuccess {
            Log.d(TAG, "AudioAttributes configured for stereo playback")
        }.onFailure { throwable ->
            Log.w(
                TAG,
                "Sora Android SDK に audioAttributes API がないため、ステレオ用 AudioAttributes を適用できません。" +
                    "ローカル SDK を指定してください",
                throwable,
            )
        }
    }

    private fun formatValue(value: Double?): String = value?.let { String.format(Locale.ROOT, "%.5f", it) } ?: "-"

    private fun enableStartButton() {
        binding.startButton.isEnabled = true
        binding.startButton.setBackgroundColor(Color.parseColor("#F06292"))
        binding.stopButton.isEnabled = false
        binding.stopButton.setBackgroundColor(Color.parseColor("#CCCCCC"))
    }

    private fun disableStartButton() {
        binding.startButton.isEnabled = false
        binding.startButton.setBackgroundColor(Color.parseColor("#CCCCCC"))
        binding.stopButton.isEnabled = true
        binding.stopButton.setBackgroundColor(Color.parseColor("#F06292"))
    }

    private fun disableStopButton() {
        enableStartButton()
    }
}
