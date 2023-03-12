package app.kaisa.drugs4covid

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView.StreamState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import app.kaisa.drugs4covid.analysis.TextAnalyzer
import app.kaisa.drugs4covid.databinding.ActivityMainBinding
import app.kaisa.drugs4covid.db.D4CDatabase
import app.kaisa.drugs4covid.db.entity.Atc
import app.kaisa.drugs4covid.db.entity.Disease
import app.kaisa.drugs4covid.db.entity.Drug
import app.kaisa.drugs4covid.models.BioEntity
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.opencsv.CSVReaderBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

typealias LumaListener = (luma: Double) -> Unit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var db: D4CDatabase
    lateinit var analyzer: TextAnalyzer

    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var isAnalyzing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        readPreviewSize()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS,
            )
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener {
            if (isAnalyzing) {
                viewBinding.entityOverlay.clear()
                startCamera()
                viewBinding.imageCaptureButton.text = "Analyze"
            } else {
                takePhoto()
                viewBinding.imageCaptureButton.text = "Back"
            }
            isAnalyzing = !isAnalyzing
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        loadDB()
    }

    private fun setupAnalyzer(
        previewWidth: Int,
        previewHeight: Int,
        isFrontLens: Boolean,
    ): Unit = with(viewBinding) {
        analyzer = TextAnalyzer(db, previewWidth, previewHeight, isFrontLens)
        analyzer.listener = object : TextAnalyzer.Listener {
            override fun onEntitiesDetected(entityBounds: List<BioEntity>) {
                entityOverlay.post {
                    entityOverlay.drawEntitiesBounds(entityBounds)
                }
            }

            override fun onError(exception: Exception) {
//                TODO("Not yet implemented")
            }
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    freezePreview()
                    viewBinding.progressBar.isVisible = true
                    analyzeImage(image)
                }
            },
        )
    }

    fun freezePreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.get().unbindAll()
    }

    fun analyzeImage(image: ImageProxy) {
        CoroutineScope(Dispatchers.IO).launch {
            analyzer.analyzeOnBackground(image)
            runOnUiThread {
                viewBinding.progressBar.isVisible = false
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(RATIO_16_9)
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun readPreviewSize() {
        viewBinding.viewFinder.previewStreamState.observe(
            this,
            object : androidx.lifecycle.Observer<StreamState> {
                override fun onChanged(streamState: StreamState) {
                    if (streamState != StreamState.STREAMING) {
                        return
                    }

                    val preview = viewBinding.viewFinder.getChildAt(0)
                    var width = preview.width * preview.scaleX
                    var height = preview.height * preview.scaleY
                    val rotation = preview.display.rotation
                    if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                        val temp = width
                        width = height
                        height = temp
                    }
                    Log.v(
                        "Sizes",
                        "Preview size: $width x $height. width: ${preview.width} height: ${preview.height} scaleX: ${preview.scaleX} scaleY: ${preview.scaleY} rotation: $rotation",
                    )
                    setupAnalyzer(width.toInt(), height.toInt(), false)
                    viewBinding.viewFinder.previewStreamState.removeObserver(this)
                }
            },
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext,
            it,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun recognizeText() {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private fun loadDB() {
        GlobalScope.launch(Dispatchers.IO) {
            if (db.atc().count() == 0) {
                populateDb("atc")
            }
            if (db.drug().count() == 0) {
                populateDb("drugs")
            }
            if (db.disease().count() == 0) {
                populateDb("diseases")
            }
        }
    }

    fun populateDb(table: String) {
        val csvReader = CSVReaderBuilder(assets.open("$table.csv").reader())
            .withSkipLines(1)
            .build()

        csvReader.use { reader ->

            val records = reader.readAll().map { record ->
                when (table) {
                    "atc" -> {
                        Atc(
                            id = record[0],
                            name = record[1],
                        )
                    }
                    "drugs" -> {
                        Drug(
                            id = record[0],
                            name = record[1],
                        )
                    }
                    "diseases" -> {
                        Disease(
                            id = record[1],
                            name = record[0],
                        )
                    }
                    else -> {
                        throw Exception("Unknown table")
                    }
                }
            }

            when (table) {
                "atc" -> db.atc().insertAll(records as List<Atc>)
                "drugs" -> db.drug().insertAll(records as List<Drug>)
                "diseases" -> db.disease().insertAll(records as List<Disease>)
            }

            val count = db.atc().count()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults:
        IntArray,
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}
