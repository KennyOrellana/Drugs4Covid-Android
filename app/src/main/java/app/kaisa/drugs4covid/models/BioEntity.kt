package app.kaisa.drugs4covid.models

import android.graphics.RectF
import app.kaisa.drugs4covid.db.entity.Atc
import app.kaisa.drugs4covid.db.entity.Disease
import app.kaisa.drugs4covid.db.entity.Drug
import com.google.mlkit.vision.text.Text

sealed class BioEntity(val element: Text.Element, var rect: RectF? = null)
class DiseaseEntity(element: Text.Element, data: Disease, rect: RectF?) : BioEntity(element, rect)
class DrugEntity(element: Text.Element, data: Drug, rect: RectF?) : BioEntity(element, rect)
class AtcEntity(element: Text.Element, data: Atc, rect: RectF?) : BioEntity(element, rect)
