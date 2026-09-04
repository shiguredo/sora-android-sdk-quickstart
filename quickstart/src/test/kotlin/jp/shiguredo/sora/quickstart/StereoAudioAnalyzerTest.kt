package jp.shiguredo.sora.quickstart

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StereoAudioAnalyzerTest {
    @Test
    fun `stereo PCM の L と R の RMS を計算する`() {
        val analyzer = StereoAudioAnalyzer(waveformCapacityFrames = 4)
        val audioData =
            stereoPcm(
                left = shortArrayOf(16_384, -16_384),
                right = shortArrayOf(8_192, -8_192),
            )
        val initialPosition = audioData.position()

        analyzer.onData(audioData, bitsPerSample = 16, sampleRate = 48_000, numberOfChannels = 2, numberOfFrames = 2)

        val snapshot = analyzer.snapshot()
        assertEquals(initialPosition, audioData.position())
        assertEquals(AudioAnalysisStatus.STEREO, snapshot.status)
        assertEquals(2, snapshot.numberOfChannels)
        assertEquals(48_000, snapshot.sampleRate)
        assertEquals(16, snapshot.bitsPerSample)
        assertEquals(2L, snapshot.totalFrames)
        assertEquals(0.5, snapshot.rmsLeft!!, 0.000001)
        assertEquals(0.25, snapshot.rmsRight!!, 0.000001)
        assertEquals(0.25, snapshot.differenceRms!!, 0.000001)
        assertArrayEquals(floatArrayOf(0.5f, -0.5f), snapshot.waveformLeft, 0.000001f)
        assertArrayEquals(floatArrayOf(0.25f, -0.25f), snapshot.waveformRight, 0.000001f)
        assertArrayEquals(floatArrayOf(0.25f, -0.25f), snapshot.waveformDifference, 0.000001f)
    }

    @Test
    fun `waveform は最新のフレームだけを保持する`() {
        val analyzer = StereoAudioAnalyzer(waveformCapacityFrames = 2)
        val audioData =
            stereoPcm(
                left = shortArrayOf(1_000, 2_000, 3_000),
                right = shortArrayOf(4_000, 5_000, 6_000),
            )

        analyzer.onData(audioData, bitsPerSample = 16, sampleRate = 48_000, numberOfChannels = 2, numberOfFrames = 3)

        val snapshot = analyzer.snapshot()
        assertArrayEquals(floatArrayOf(2_000 / 32_768f, 3_000 / 32_768f), snapshot.waveformLeft, 0.000001f)
        assertEquals(3L, snapshot.totalFrames)
    }

    @Test
    fun `mono PCM はステレオとして扱わない`() {
        val analyzer = StereoAudioAnalyzer()
        val audioData = monoPcm(shortArrayOf(16_384, -16_384))

        analyzer.onData(audioData, bitsPerSample = 16, sampleRate = 48_000, numberOfChannels = 1, numberOfFrames = 2)

        val snapshot = analyzer.snapshot()
        assertEquals(AudioAnalysisStatus.MONO, snapshot.status)
        assertEquals(1, snapshot.numberOfChannels)
        assertEquals(0.5, snapshot.rmsLeft!!, 0.000001)
        assertNull(snapshot.rmsRight)
        assertNull(snapshot.differenceRms)
    }

    @Test
    fun `未対応の PCM 形式を通知する`() {
        val analyzer = StereoAudioAnalyzer()
        val audioData = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).apply { putShort(1).flip() }

        analyzer.onData(audioData, bitsPerSample = 8, sampleRate = 48_000, numberOfChannels = 1, numberOfFrames = 2)

        val snapshot = analyzer.snapshot()
        assertEquals(AudioAnalysisStatus.UNSUPPORTED_FORMAT, snapshot.status)
        assertEquals(0L, snapshot.totalFrames)
    }

    @Test
    fun `バッファが短い場合は PCM データ不正を通知する`() {
        val analyzer = StereoAudioAnalyzer()
        val audioData = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).apply { putShort(1).flip() }

        analyzer.onData(audioData, bitsPerSample = 16, sampleRate = 48_000, numberOfChannels = 2, numberOfFrames = 2)

        val snapshot = analyzer.snapshot()
        assertEquals(AudioAnalysisStatus.INVALID_DATA, snapshot.status)
        assertEquals(0L, snapshot.totalFrames)
    }

    @Test
    fun `停止後は PCM を受け取らない`() {
        val analyzer = StereoAudioAnalyzer()
        analyzer.stop()

        analyzer.onData(
            stereoPcm(shortArrayOf(16_384), shortArrayOf(16_384)),
            bitsPerSample = 16,
            sampleRate = 48_000,
            numberOfChannels = 2,
            numberOfFrames = 1,
        )

        val snapshot = analyzer.snapshot()
        assertEquals(AudioAnalysisStatus.NO_DATA, snapshot.status)
        assertEquals(0L, snapshot.totalFrames)
    }

    private fun stereoPcm(
        left: ShortArray,
        right: ShortArray,
    ): ByteBuffer {
        require(left.size == right.size)
        return ByteBuffer
            .allocate(left.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                left.indices.forEach { index ->
                    putShort(left[index])
                    putShort(right[index])
                }
                flip()
            }
    }

    private fun monoPcm(samples: ShortArray): ByteBuffer =
        ByteBuffer
            .allocate(samples.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                samples.forEach(::putShort)
                flip()
            }
}
