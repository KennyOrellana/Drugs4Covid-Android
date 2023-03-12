package app.kaisa.drugs4covid.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import app.kaisa.drugs4covid.R
import app.kaisa.drugs4covid.models.AtcEntity
import app.kaisa.drugs4covid.models.BioEntity
import app.kaisa.drugs4covid.models.DiseaseEntity
import app.kaisa.drugs4covid.models.DrugEntity

class EntityOverlay(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    private val entityBounds: MutableList<BioEntity> = mutableListOf()
    private val paintAtc = Paint().apply {
        color = ContextCompat.getColor(context!!, R.color.marker_atc)
        strokeWidth = 4f
        isDither = true
        style = Paint.Style.FILL_AND_STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(8f)
        isAntiAlias = true
    }

    private val paintDisease = Paint().apply {
        color = ContextCompat.getColor(context!!, R.color.marker_disease)
        strokeWidth = 4f
        isDither = true
        style = Paint.Style.FILL_AND_STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(8f)
        isAntiAlias = true
    }

    private val paintDrug = Paint().apply {
        color = ContextCompat.getColor(context!!, R.color.marker_drug)
        strokeWidth = 4f
        isDither = true
        style = Paint.Style.FILL_AND_STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(8f)
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        entityBounds.forEach { entity ->
            entity.rect?.let { rect ->
                val paint = when (entity) {
                    is AtcEntity -> paintAtc
                    is DiseaseEntity -> paintDisease
                    is DrugEntity -> paintDrug
                }
                canvas.drawRect(rect, paint)
            }
        }
    }

    fun drawEntitiesBounds(faceBounds: List<BioEntity>) {
        this.entityBounds.clear()
        this.entityBounds.addAll(faceBounds)
        invalidate()
    }

    fun clear() {
        this.entityBounds.clear()
        invalidate()
    }
}
