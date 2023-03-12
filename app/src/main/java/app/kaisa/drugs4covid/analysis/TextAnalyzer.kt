package app.kaisa.drugs4covid.analysis

import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import app.kaisa.drugs4covid.db.D4CDatabase
import app.kaisa.drugs4covid.models.AtcEntity
import app.kaisa.drugs4covid.models.BioEntity
import app.kaisa.drugs4covid.models.DiseaseEntity
import app.kaisa.drugs4covid.models.DrugEntity
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
class TextAnalyzer(
    var db: D4CDatabase,
    private val previewWidth: Int,
    private val previewHeight: Int,
    private val isFrontLens: Boolean,
) {
    val matched = arrayListOf<BioEntity>()

    /** Listener to receive callbacks for when entities are detected, or an error occurs.  */
    var listener: Listener? = null

    suspend fun analyzeOnBackground(imageProxy: ImageProxy) {
        matched.clear()
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = recognizer.process(image)

            val result = Tasks.await(visionText)

            val rotation = imageProxy.imageInfo.rotationDegrees
            // In order to correctly display the text bounds, the orientation of the analyzed
            // image and that of the viewfinder have to match. Which is why the dimensions of
            // the analyzed image are reversed if its rotation information is 90 or 270.
            val reverseDimens = rotation == 90 || rotation == 270
            val width = if (reverseDimens) imageProxy.height else imageProxy.width
            val height = if (reverseDimens) imageProxy.width else imageProxy.height

            printText(result, width, height)
            imageProxy.close()
        }
    }

    private fun printText(result: Text, width: Int, height: Int) {
        for (block in result.textBlocks) {
            val blockText = block.text
            val blockCornerPoints = block.cornerPoints
            val blockFrame = block.boundingBox
            for (line in block.lines) {
                val lineText = line.text
                val lineCornerPoints = line.cornerPoints
                val lineFrame = line.boundingBox
                for (element in line.elements) {
                    val elementText = element.text
                    val drugs = db.drug().search(elementText)
                    drugs.forEach { drug ->
                        matched.add(
                            DrugEntity(
                                element,
                                drug,
                                element.boundingBox?.transform(width, height),
                            ),
                        )
                    }

                    val diseases = db.disease().search(elementText)
                    diseases.forEach { disease ->
                        matched.add(
                            DiseaseEntity(
                                element,
                                disease,
                                element.boundingBox?.transform(width, height),
                            ),
                        )
                    }

                    val atc = db.atc().search(elementText)
                    atc.forEach { atc ->
                        matched.add(
                            AtcEntity(
                                element,
                                atc,
                                element.boundingBox?.transform(width, height),
                            ),
                        )
                    }
                }
            }
        }

        listener?.onEntitiesDetected(matched)
    }

    private fun Rect.transform(width: Int, height: Int): RectF {
        val scaleX = previewWidth / width.toFloat()
        val scaleY = previewHeight / height.toFloat()

        // If the front camera lens is being used, reverse the right/left coordinates
        val flippedLeft = if (isFrontLens) width - right else left
        val flippedRight = if (isFrontLens) width - left else right

        // Scale all coordinates to match preview
        val scaledLeft = scaleX * flippedLeft
        val scaledTop = scaleY * top
        val scaledRight = scaleX * flippedRight
        val scaledBottom = scaleY * bottom

        return RectF(scaledLeft, scaledTop, scaledRight, scaledBottom)
    }

    /**
     * Interface to register callbacks for when the text analyzer provides detected entities bounds, or
     * when it encounters an error.
     */
    interface Listener {
        /** Callback that receives BioEntity that can be drawn on top of the viewfinder.  */
        fun onEntitiesDetected(entityBounds: List<BioEntity>)

        /** Invoked when an error is encounter during text detection.  */
        fun onError(exception: Exception)
    }
}
