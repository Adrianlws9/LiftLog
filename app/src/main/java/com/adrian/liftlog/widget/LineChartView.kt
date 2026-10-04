package com.adrian.liftlog.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.adrian.liftlog.R

/**
 * A minimal, self-drawn line chart for a single data series.
 * No external library — just enough to plot a trend over time clearly.
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Float> = emptyList()
    private var labels: List<String> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        alpha = 60
        strokeWidth = 2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 28f
    }

    private val valueLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 28f
    }

    fun setData(values: List<Float>, labels: List<String>) {
        this.values = values
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.size < 2) return

        val paddingLeft = 60f
        val paddingRight = 24f
        val paddingTop = 24f
        val paddingBottom = 60f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).let { if (it == 0f) 1f else it }

        fun xFor(index: Int) = paddingLeft + (chartWidth * index / (values.size - 1))
        fun yFor(value: Float) = paddingTop + chartHeight - ((value - minVal) / range * chartHeight)

        // Horizontal grid lines (min, mid, max)
        for (fraction in listOf(0f, 0.5f, 1f)) {
            val y = paddingTop + chartHeight * (1 - fraction)
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
            val labelValue = minVal + range * fraction
            canvas.drawText(String.format("%.1f", labelValue), 4f, y + 10f, valueLabelPaint)
        }

        // Line connecting points
        for (i in 0 until values.size - 1) {
            canvas.drawLine(
                xFor(i), yFor(values[i]),
                xFor(i + 1), yFor(values[i + 1]),
                linePaint
            )
        }

        // Dots
        for (i in values.indices) {
            canvas.drawCircle(xFor(i), yFor(values[i]), 8f, dotPaint)
        }

        // X-axis labels — show only first and last to avoid clutter
        if (labels.isNotEmpty()) {
            canvas.drawText(labels.first(), paddingLeft, height - 16f, labelPaint)
            val lastWidth = labelPaint.measureText(labels.last())
            canvas.drawText(labels.last(), width - paddingRight - lastWidth, height - 16f, labelPaint)
        }
    }
}