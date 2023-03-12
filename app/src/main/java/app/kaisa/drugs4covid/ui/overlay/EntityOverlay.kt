package app.kaisa.drugs4covid.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import app.kaisa.drugs4covid.models.BioEntity

class EntityOverlay(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    private val entityBounds: MutableList<BioEntity> = mutableListOf()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context!!, android.R.color.holo_red_dark)
        strokeWidth = 10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        entityBounds.forEach { entity ->
            entity.rect?.let { rect ->
                canvas.drawRect(rect, paint)
            }
        }
    }

    fun drawEntitiesBounds(faceBounds: List<BioEntity>) {
        this.entityBounds.clear()
        this.entityBounds.addAll(faceBounds)
        invalidate()
    }
}
