package jp.shiguredo.sora.quickstart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class StereoWaveformView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        private val backgroundColor = Color.rgb(18, 18, 18)
        private val gridColor = Color.rgb(70, 70, 70)
        private val textColor = Color.rgb(235, 235, 235)
        private val leftColor = Color.rgb(80, 220, 130)
        private val rightColor = Color.rgb(100, 170, 255)
        private val differenceColor = Color.rgb(255, 190, 80)

        private val gridPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gridColor
                strokeWidth = resources.displayMetrics.density
            }
        private val labelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        14f,
                        resources.displayMetrics,
                    )
            }
        private val leftPaint = createWaveformPaint(leftColor)
        private val rightPaint = createWaveformPaint(rightColor)
        private val differencePaint = createWaveformPaint(differenceColor)
        private val path = Path()

        private var snapshot = StereoAudioSnapshot.empty()

        fun setSnapshot(snapshot: StereoAudioSnapshot) {
            this.snapshot = snapshot
            postInvalidateOnAnimation()
        }

        fun clear() {
            setSnapshot(StereoAudioSnapshot.empty())
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(backgroundColor)

            val laneHeight = height / 3f
            drawLane(canvas, 0, laneHeight, "L", snapshot.waveformLeft, leftPaint, true)
            drawLane(
                canvas,
                1,
                laneHeight,
                "R",
                snapshot.waveformRight,
                rightPaint,
                snapshot.numberOfChannels >= 2,
            )
            drawLane(
                canvas,
                2,
                laneHeight,
                "L - R",
                snapshot.waveformDifference,
                differencePaint,
                snapshot.numberOfChannels >= 2,
            )

            if (snapshot.status == AudioAnalysisStatus.NO_DATA) {
                canvas.drawText("受信待ち", 12f, height / 2f, labelPaint)
            }
        }

        private fun drawLane(
            canvas: Canvas,
            lane: Int,
            laneHeight: Float,
            label: String,
            samples: FloatArray,
            paint: Paint,
            drawWaveform: Boolean,
        ) {
            val top = lane * laneHeight
            val center = top + laneHeight / 2f
            canvas.drawLine(0f, center, width.toFloat(), center, gridPaint)
            canvas.drawLine(0f, top, width.toFloat(), top, gridPaint)
            canvas.drawText(label, 12f, top + labelPaint.textSize + 6f, labelPaint)

            if (!drawWaveform || samples.size < 2 || width <= 0) {
                return
            }

            path.reset()
            val amplitude = laneHeight * 0.38f
            samples.forEachIndexed { index, sample ->
                val x = index * width.toFloat() / (samples.size - 1)
                val y = center - sample.coerceIn(-1f, 1f) * amplitude
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            canvas.drawPath(path, paint)
        }

        private fun createWaveformPaint(color: Int): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * resources.displayMetrics.density
            }
    }
