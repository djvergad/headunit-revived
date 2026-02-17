package com.andrerinas.headunitrevived.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Custom view for touch calibration that displays markers at each corner of the screen.
 * Users click anywhere on the screen at each marker location to calibrate touch offset for that corner.
 * The view calculates and stores the offset between the marker center and the actual click location.
 */
class TouchCalibrationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface CalibrationListener {
        fun onCalibrationChanged()
    }

    private var calibrationListener: CalibrationListener? = null

    fun setCalibrationListener(listener: CalibrationListener?) {
        this.calibrationListener = listener
    }

    private data class CornerData(
        val name: String,
        val x: Float,
        val y: Float,
        val radius: Float = 40f,
        var offsetX: Int = 0,
        var offsetY: Int = 0,
        var isCalibrated: Boolean = false
    )

    private val corners = mutableListOf<CornerData>()
    private var currentCornerIndex = 0
    private val paint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 24f
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private val CORNER_RADIUS = 40f
    private val MARKER_SIZE = 100f
    private val CORNER_INSET = 200f  // Distance from edge to move dots toward center

    init {
        setBackgroundColor(Color.BLACK)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        android.util.Log.d("TOUCH_CAL_VIEW", "onSizeChanged: w=$w, h=$h, oldw=$oldw, oldh=$oldh")
        
        // If corners already exist (calibration in progress), preserve their offsets
        if (corners.isNotEmpty()) {
            android.util.Log.d("TOUCH_CAL_VIEW", "Preserving ${corners.size} existing corners with offsets")
            // Just update the positions but keep offsets
            corners[0] = corners[0].copy(x = CORNER_INSET, y = CORNER_INSET)
            corners[1] = corners[1].copy(x = w - CORNER_INSET, y = CORNER_INSET)
            corners[2] = corners[2].copy(x = CORNER_INSET, y = h - CORNER_INSET)
            corners[3] = corners[3].copy(x = w - CORNER_INSET, y = h - CORNER_INSET)
            android.util.Log.d("TOUCH_CAL_VIEW", "Updated corner positions for new size")
            return
        }
        
        // First time initialization
        corners.clear()
        corners.add(CornerData("TL", CORNER_INSET, CORNER_INSET))
        corners.add(CornerData("TR", w - CORNER_INSET, CORNER_INSET))
        corners.add(CornerData("BL", CORNER_INSET, h - CORNER_INSET))
        corners.add(CornerData("BR", w - CORNER_INSET, h - CORNER_INSET))
        
        android.util.Log.d("TOUCH_CAL_VIEW", "Initialized ${corners.size} corners at positions: ${corners.map { "${it.name}=(${it.x}, ${it.y})" }}")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all corners
        for ((index, corner) in corners.withIndex()) {
            val isCurrentCorner = index == currentCornerIndex && !corner.isCalibrated
            val isCalibrated = corner.isCalibrated

            // Draw the target marker
            paint.color = when {
                isCurrentCorner -> Color.GREEN
                isCalibrated -> Color.BLUE
                else -> Color.GRAY
            }

            // Draw circle
            canvas.drawCircle(corner.x, corner.y, corner.radius, paint)

            // Draw border
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawCircle(corner.x, corner.y, corner.radius, paint)
            paint.style = Paint.Style.FILL

            // Draw label
            textPaint.color = Color.WHITE
            textPaint.textSize = 20f
            canvas.drawText(corner.name, corner.x, corner.y + 7, textPaint)

            // Draw offset if calibrated
            if (isCalibrated) {
                textPaint.color = Color.YELLOW
                textPaint.textSize = 16f
                canvas.drawText(
                    "Offset: (${corner.offsetX}, ${corner.offsetY})",
                    corner.x,
                    corner.y + corner.radius + 30,
                    textPaint
                )
            }
        }

        // Draw current instruction
        if (currentCornerIndex < corners.size) {
            val corner = corners[currentCornerIndex]
            if (!corner.isCalibrated) {
                textPaint.color = Color.YELLOW
                textPaint.textSize = 28f
                canvas.drawText(
                    "Click on the green circle",
                    width / 2f,
                    50f,
                    textPaint
                )
            }
        } else {
            textPaint.color = Color.GREEN
            textPaint.textSize = 28f
            canvas.drawText(
                "Calibration Complete!",
                width / 2f,
                50f,
                textPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        android.util.Log.d("TOUCH_CAL_VIEW", "onTouchEvent called: action=${event.action}, x=${event.x}, y=${event.y}, currentCorner=$currentCornerIndex")
        
        if (event.action != MotionEvent.ACTION_DOWN) {
            return true
        }

        if (currentCornerIndex >= corners.size) {
            android.util.Log.d("TOUCH_CAL_VIEW", "Already calibrated, ignoring touch")
            return true // Already calibrated
        }

        val currentCorner = corners[currentCornerIndex]

        // Record the offset based on where the user actually clicked
        val offsetX = (event.x - currentCorner.x).toInt()
        val offsetY = (event.y - currentCorner.y).toInt()

        android.util.Log.d("TOUCH_CAL_VIEW", "Setting offset for corner $currentCornerIndex: markerPos=(${currentCorner.x}, ${currentCorner.y}), clickPos=(${event.x}, ${event.y}), offset=($offsetX, $offsetY)")

        currentCorner.offsetX = offsetX
        currentCorner.offsetY = offsetY
        currentCorner.isCalibrated = true

        // Move to next corner
        currentCornerIndex++
        invalidate()
        
        // Notify listener of calibration change
        calibrationListener?.onCalibrationChanged()
        
        android.util.Log.d("TOUCH_CAL_VIEW", "Now at corner $currentCornerIndex, total calibrated: ${getCalibratedCornerCount()}/4")

        return true
    }

    fun getCurrentCorner(): Int = currentCornerIndex

    fun getCalibratedCornerCount(): Int {
        val count = corners.count { it.isCalibrated }
        android.util.Log.d("TOUCH_CAL_VIEW", "getCalibratedCornerCount: $count/4 calibrated")
        return count
    }

    fun isCalibrationComplete(): Boolean {
        val complete = currentCornerIndex >= corners.size
        android.util.Log.d("TOUCH_CAL_VIEW", "isCalibrationComplete check: currentCornerIndex=$currentCornerIndex, corners.size=${corners.size}, result=$complete")
        return complete
    }

    fun getCalibrationOffsets(): List<Pair<Int, Int>> {
        val offsets = corners.map { Pair(it.offsetX, it.offsetY) }
        android.util.Log.d("TOUCH_CAL_VIEW", "getCalibrationOffsets called: returning $offsets from ${corners.size} corners")
        return offsets
    }

    fun resetCalibration() {
        currentCornerIndex = 0
        for (corner in corners) {
            corner.offsetX = 0
            corner.offsetY = 0
            corner.isCalibrated = false
        }
        invalidate()
    }
}
