package jp.shiguredo.sora.quickstart

import org.webrtc.AudioTrackSink
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

enum class AudioAnalysisStatus {
    NO_DATA,
    STEREO,
    MONO,
    UNSUPPORTED_FORMAT,
    INVALID_DATA,
}

data class StereoAudioSnapshot(
    val status: AudioAnalysisStatus = AudioAnalysisStatus.NO_DATA,
    val numberOfChannels: Int = 0,
    val sampleRate: Int = 0,
    val bitsPerSample: Int = 0,
    val totalFrames: Long = 0,
    val rmsLeft: Double? = null,
    val rmsRight: Double? = null,
    val differenceRms: Double? = null,
    val waveformLeft: FloatArray = FloatArray(0),
    val waveformRight: FloatArray = FloatArray(0),
    val waveformDifference: FloatArray = FloatArray(0),
) {
    companion object {
        fun empty(): StereoAudioSnapshot = StereoAudioSnapshot()
    }
}

class StereoAudioAnalyzer(
    private val waveformCapacityFrames: Int = DEFAULT_WAVEFORM_CAPACITY_FRAMES,
) : AudioTrackSink {
    companion object {
        private const val SUPPORTED_BITS_PER_SAMPLE = 16
        private const val MAX_SUPPORTED_CHANNELS = 8
        private const val PCM_SCALE = 32768.0f
        private const val DEFAULT_WAVEFORM_CAPACITY_FRAMES = 4_800
    }

    init {
        require(waveformCapacityFrames > 0) { "waveformCapacityFrames must be positive" }
    }

    private val lock = Any()
    private val waveformLeft = FloatArray(waveformCapacityFrames)
    private val waveformRight = FloatArray(waveformCapacityFrames)
    private val waveformDifference = FloatArray(waveformCapacityFrames)

    private var running = true
    private var status = AudioAnalysisStatus.NO_DATA
    private var numberOfChannels = 0
    private var sampleRate = 0
    private var bitsPerSample = 0
    private var totalFrames = 0L

    private var measurementFrames = 0L
    private var sumSquaresLeft = 0.0
    private var sumSquaresRight = 0.0
    private var sumSquaresDifference = 0.0
    private var latestRmsLeft: Double? = null
    private var latestRmsRight: Double? = null
    private var latestDifferenceRms: Double? = null

    private var waveformWriteIndex = 0
    private var waveformFrameCount = 0

    override fun onData(
        audioData: ByteBuffer,
        bitsPerSample: Int,
        sampleRate: Int,
        numberOfChannels: Int,
        numberOfFrames: Int,
    ) {
        val input = audioData.duplicate().order(ByteOrder.LITTLE_ENDIAN)

        synchronized(lock) {
            if (!running) {
                return
            }

            updateFormatLocked(bitsPerSample, sampleRate, numberOfChannels)

            if (bitsPerSample != SUPPORTED_BITS_PER_SAMPLE ||
                numberOfChannels !in 1..MAX_SUPPORTED_CHANNELS
            ) {
                status = AudioAnalysisStatus.UNSUPPORTED_FORMAT
                resetMeasurementLocked()
                return
            }

            if (numberOfFrames <= 0) {
                return
            }

            val bytesPerFrame = numberOfChannels.toLong() * (bitsPerSample / 8)
            val requiredBytes = numberOfFrames.toLong() * bytesPerFrame
            if (requiredBytes > input.remaining().toLong()) {
                status = AudioAnalysisStatus.INVALID_DATA
                resetMeasurementLocked()
                return
            }

            repeat(numberOfFrames) {
                var left = 0.0f
                var right = 0.0f

                repeat(numberOfChannels) { channel ->
                    val sample = input.getShort().toInt() / PCM_SCALE
                    when (channel) {
                        0 -> left = sample
                        1 -> right = sample
                    }
                }

                val difference = left - right
                sumSquaresLeft += left * left
                if (numberOfChannels >= 2) {
                    sumSquaresRight += right * right
                    sumSquaresDifference += difference * difference
                }
                measurementFrames += 1
                totalFrames += 1

                waveformLeft[waveformWriteIndex] = left
                waveformRight[waveformWriteIndex] = right
                waveformDifference[waveformWriteIndex] = difference
                waveformWriteIndex = (waveformWriteIndex + 1) % waveformCapacityFrames
                waveformFrameCount = minOf(waveformFrameCount + 1, waveformCapacityFrames)
            }

            status = if (numberOfChannels >= 2) AudioAnalysisStatus.STEREO else AudioAnalysisStatus.MONO
        }
    }

    fun stop() {
        synchronized(lock) {
            running = false
        }
    }

    fun snapshot(): StereoAudioSnapshot =
        synchronized(lock) {
            updateLatestMeasurementLocked()

            val left = FloatArray(waveformFrameCount)
            val right = FloatArray(waveformFrameCount)
            val difference = FloatArray(waveformFrameCount)
            val firstIndex =
                (waveformWriteIndex - waveformFrameCount + waveformCapacityFrames) % waveformCapacityFrames

            repeat(waveformFrameCount) { offset ->
                val index = (firstIndex + offset) % waveformCapacityFrames
                left[offset] = waveformLeft[index]
                right[offset] = waveformRight[index]
                difference[offset] = waveformDifference[index]
            }

            StereoAudioSnapshot(
                status = status,
                numberOfChannels = numberOfChannels,
                sampleRate = sampleRate,
                bitsPerSample = bitsPerSample,
                totalFrames = totalFrames,
                rmsLeft = latestRmsLeft,
                rmsRight = latestRmsRight.takeIf { numberOfChannels >= 2 },
                differenceRms = latestDifferenceRms.takeIf { numberOfChannels >= 2 },
                waveformLeft = left,
                waveformRight = right,
                waveformDifference = difference,
            )
        }

    private fun updateFormatLocked(
        bitsPerSample: Int,
        sampleRate: Int,
        numberOfChannels: Int,
    ) {
        if (this.bitsPerSample != 0 &&
            (
                this.bitsPerSample != bitsPerSample ||
                    this.sampleRate != sampleRate ||
                    this.numberOfChannels != numberOfChannels
            )
        ) {
            resetDataLocked()
        }
        this.bitsPerSample = bitsPerSample
        this.sampleRate = sampleRate
        this.numberOfChannels = numberOfChannels
    }

    private fun updateLatestMeasurementLocked() {
        if (measurementFrames == 0L) {
            return
        }

        latestRmsLeft = sqrt(sumSquaresLeft / measurementFrames)
        if (numberOfChannels >= 2) {
            latestRmsRight = sqrt(sumSquaresRight / measurementFrames)
            latestDifferenceRms = sqrt(sumSquaresDifference / measurementFrames)
        } else {
            latestRmsRight = null
            latestDifferenceRms = null
        }
        resetMeasurementLocked()
    }

    private fun resetMeasurementLocked() {
        measurementFrames = 0L
        sumSquaresLeft = 0.0
        sumSquaresRight = 0.0
        sumSquaresDifference = 0.0
    }

    private fun resetDataLocked() {
        status = AudioAnalysisStatus.NO_DATA
        numberOfChannels = 0
        sampleRate = 0
        bitsPerSample = 0
        totalFrames = 0L
        latestRmsLeft = null
        latestRmsRight = null
        latestDifferenceRms = null
        waveformWriteIndex = 0
        waveformFrameCount = 0
        resetMeasurementLocked()
    }
}
