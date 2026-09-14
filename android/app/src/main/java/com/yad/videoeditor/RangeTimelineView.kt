package com.yad.videoeditor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * RangeTimelineView — timeline dengan 2 handle untuk trim range.
 * Menampilkan thumbnail strip + playhead marker.
 */
class RangeTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Range
    var rangeStartMs: Long = 0L
    var rangeEndMs: Long = 0L
    var totalDurationMs: Long = 0L

    // Playhead
    var playheadMs: Long = 0L

    // Thumbnails
    private var thumbnails: List<Bitmap> = emptyList()

    // Paint
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E")
    }
    private val thumbPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA000000")
    }
    private val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#338B5CF6")
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B5CF6")
    }
    private val handleInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
        strokeWidth = 3f
    }

    // Handle metrics
    private val handleWidth = 24f
    private val handleTop = 8f
    private val handleBottom = 0f

    // Touch
    private var draggingHandle: Int = -1  // -1 none, 0 left, 1 right, 2 playhead

    // Listener
    var onRangeChanged: ((startMs: Long, endMs: Long) -> Unit)? = null
    var onPlayheadMoved: ((ms: Long) -> Unit)? = null

    fun setVideoDuration(durationMs: Long) {
        totalDurationMs = durationMs
        if (rangeEndMs == 0L || rangeEndMs > durationMs) {
            rangeEndMs = durationMs
        }
        invalidate()
    }

    fun setThumbnails(thumbs: List<Bitmap>) {
        thumbnails = thumbs
        invalidate()
    }

    fun setRange(startMs: Long, endMs: Long) {
        rangeStartMs = startMs.coerceIn(0, totalDurationMs)
        rangeEndMs = endMs.coerceIn(rangeStartMs, totalDurationMs)
        invalidate()
    }

    fun setPlayhead(ms: Long) {
        playheadMs = ms.coerceIn(0, totalDurationMs)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Thumbnails
        if (thumbnails.isNotEmpty() && totalDurationMs > 0) {
            val thumbW = w / thumbnails.size
            for ((i, bmp) in thumbnails.withIndex()) {
                val left = i * thumbW
                val srcRect = Rect(0, 0, bmp.width, bmp.height)
                val dstRect = RectF(left, 0f, left + thumbW, h)
                canvas.drawBitmap(bmp, srcRect, dstRect, thumbPaint)
            }
        } else {
            // Placeholder kalau belum ada thumbnail
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Timeline", w / 2, h / 2 + 8, textPaint)
        }

        if (totalDurationMs <= 0) return

        // Overlay di luar range
        val startX = (rangeStartMs.toFloat() / totalDurationMs) * w
        val endX = (rangeEndMs.toFloat() / totalDurationMs) * w

        canvas.drawRect(0f, 0f, startX, h, overlayPaint)
        canvas.drawRect(endX, 0f, w, h, overlayPaint)

        // Range highlight border
        val rangeRect = RectF(startX, 0f, endX, h)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#8B5CF6")
        }
        canvas.drawRect(rangeRect, borderPaint)

        // Handles
        drawHandle(canvas, startX, h, isLeft = true)
        drawHandle(canvas, endX, h, isLeft = false)

        // Playhead
        if (playheadMs in rangeStartMs..rangeEndMs) {
            val phX = (playheadMs.toFloat() / totalDurationMs) * w
            canvas.drawLine(phX, 0f, phX, h, playheadPaint)
            // Playhead knob
            val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EF4444")
            }
            canvas.drawCircle(phX, h - 6f, 6f, knob)
        }
    }

    private fun drawHandle(canvas: Canvas, x: Float, h: Float, isLeft: Boolean) {
        val left = if (isLeft) x else x - handleWidth
        val right = left + handleWidth
        val top = handleTop
        val bottom = h - handleBottom

        val rect = RectF(left, top, right, bottom)
        val radius = 6f
        canvas.drawRoundRect(rect, radius, radius, handlePaint)

        // Inner lines
        val lineY1 = top + (bottom - top) * 0.35f
        val lineY2 = top + (bottom - top) * 0.5f
        val lineY3 = top + (bottom - top) * 0.65f
        val cx = left + handleWidth / 2
        canvas.drawCircle(cx, lineY1, 1.5f, handleInnerPaint)
        canvas.drawCircle(cx, lineY2, 1.5f, handleInnerPaint)
        canvas.drawCircle(cx, lineY3, 1.5f, handleInnerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (totalDurationMs <= 0) return false
        val w = width.toFloat()
        val x = event.x

        val startX = (rangeStartMs.toFloat() / totalDurationMs) * w
        val endX = (rangeEndMs.toFloat() / totalDurationMs) * w
        val playX = (playheadMs.toFloat() / totalDurationMs) * w

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchThreshold = 40f
                draggingHandle = when {
                    Math.abs(x - startX) < touchThreshold -> 0
                    Math.abs(x - endX) < touchThreshold -> 1
                    Math.abs(x - playX) < touchThreshold -> 2
                    else -> -1
                }
                return draggingHandle != -1
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingHandle == -1) return false
                val ms = ((x / w) * totalDurationMs).toLong().coerceIn(0, totalDurationMs)
                when (draggingHandle) {
                    0 -> {
                        rangeStartMs = ms.coerceAtMost(rangeEndMs - 100)
                        onRangeChanged?.invoke(rangeStartMs, rangeEndMs)
                    }
                    1 -> {
                        rangeEndMs = ms.coerceAtLeast(rangeStartMs + 100)
                        onRangeChanged?.invoke(rangeStartMs, rangeEndMs)
                    }
                    2 -> {
                        playheadMs = ms
                        onPlayheadMoved?.invoke(playheadMs)
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggingHandle = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
