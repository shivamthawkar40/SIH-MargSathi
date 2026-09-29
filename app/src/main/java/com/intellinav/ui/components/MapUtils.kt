package com.intellinav.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color

object MapUtils {
    /**
     * Creates a Google Maps-style navigation chevron (arrow) bitmap.
     * The arrow points UP, and Google Maps SDK will handle rotating it.
     */
    fun createNavigationArrowBitmap(colorInt: Int): Bitmap {
        val size = 100 // Scale of the arrow in pixels
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }
        
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeJoin = Paint.Join.ROUND
        }
        
        val path = Path()
        // Draw a chevron pointing UP.
        // Top tip
        path.moveTo(size / 2f, 10f)
        // Bottom right corner
        path.lineTo(size - 15f, size - 15f)
        // Bottom center indent
        path.lineTo(size / 2f, size - 30f)
        // Bottom left corner
        path.lineTo(15f, size - 15f)
        path.close()
        
        // Add a subtle drop shadow to make it pop over the map
        fillPaint.setShadowLayer(8f, 0f, 4f, Color.argb(100, 0, 0, 0))
        
        canvas.drawPath(path, fillPaint)
        
        // Clear shadow and draw the white stroke/border
        fillPaint.clearShadowLayer()
        canvas.drawPath(path, strokePaint)
        
        return bitmap
    }
}
