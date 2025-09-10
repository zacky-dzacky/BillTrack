package com.billtrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.billtrack.databinding.ActivityOcrBinding
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OCRActivity : AppCompatActivity() {

    private var textToSend: String = ""
    private lateinit var binding: ActivityOcrBinding
    private var capturedBitmap: Bitmap? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var latestCapturedImageUri: Uri? = null
    private lateinit var textRecognizer: TextRecognizer
    private lateinit var imageLabeler: ImageLabeler

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val SUSPICIOUS_LABELS = listOf("Screen", "Monitor", "LCD screen", "Photograph", "Document", "Screenshot")
        private const val LABEL_CONFIDENCE_THRESHOLD = 0.75f
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied. App requires camera to function.", Toast.LENGTH_LONG).show()
            }
        }

    private val apiService: ApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(ChuckerInterceptor.Builder(this).build())
            .build()

        Retrofit.Builder()
            .baseUrl("https://21a9ede354e5.ngrok-free.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.doneButton.text = getString(R.string.take_picture)
        binding.retakeButton.visibility = View.GONE
        binding.instructionTextView.text = getString(R.string.align_id_instruction)
        binding.titleTextView.text = getString(R.string.photo_id_ocr_title)

        binding.doneButton.setOnClickListener {
            if (binding.capturedImagePreview.isVisible) {
                capturedBitmap?.let { bitmap -> uploadImageToServer(bitmap) }
                    ?: Toast.makeText(this, "No image to upload.", Toast.LENGTH_SHORT).show()
            } else {
                takePhoto()
            }
        }

        binding.retakeButton.setOnClickListener {
            switchToCameraView()
        }
    }

    private fun switchToCameraView() {
        binding.cameraContainer.visibility = View.VISIBLE
        binding.capturedImagePreview.visibility = View.GONE
        binding.capturedImagePreview.setImageDrawable(null)
        binding.doneButton.text = getString(R.string.take_picture)
        binding.retakeButton.visibility = View.GONE
        binding.instructionTextView.text = getString(R.string.align_id_instruction)
        deleteLatestCapturedImage()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetResolution(Size(720, 1280))
                .build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1080, 1920))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(100)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Failed to start camera.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val photoFile = File(cacheDir, "${name}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    runOnUiThread {
                        Toast.makeText(baseContext, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    latestCapturedImageUri = Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded, saved to: $latestCapturedImageUri")

                    lifecycleScope.launch(Dispatchers.IO) {
                        var originalBitmap: Bitmap? = null
                        try {
                            originalBitmap = latestCapturedImageUri?.let { uri ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    val source = ImageDecoder.createSource(contentResolver, uri)
                                    ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
                                } else {
                                    @Suppress("DEPRECATION")
                                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                                }
                            }

                            withContext(Dispatchers.Main) {
                                originalBitmap?.let { loadedBitmap ->
                                    Log.d(TAG, "Original captured image dimensions: ${loadedBitmap.width}x${loadedBitmap.height}")

                                    val overlayRect = binding.overlayView.getFrameRectRelativeToView()
                                    val overlayW = binding.overlayView.width.toFloat()
                                    val overlayH = binding.overlayView.height.toFloat()
                                    var bitmapToProcess = loadedBitmap

                                    if (overlayW > 0f && overlayH > 0f && !overlayRect.isEmpty) {
                                        val bitmapW = loadedBitmap.width.toFloat()
                                        val bitmapH = loadedBitmap.height.toFloat()
                                        val scaleX = bitmapW / overlayW
                                        val scaleY = bitmapH / overlayH
                                        val cropX = (overlayRect.left * scaleX).coerceAtLeast(0f)
                                        val cropY = (overlayRect.top * scaleY).coerceAtLeast(0f)
                                        val cropWidth = (overlayRect.width() * scaleX).coerceAtMost(bitmapW - cropX)
                                        val cropHeight = (overlayRect.height() * scaleY).coerceAtMost(bitmapH - cropY)

                                        if (cropWidth > 0 && cropHeight > 0) {
                                            try {
                                                val cropped = Bitmap.createBitmap(loadedBitmap, cropX.toInt(), cropY.toInt(), cropWidth.toInt(), cropHeight.toInt())
                                                Log.d(TAG, "Cropped image to: ${cropped.width}x${cropped.height}")
                                                bitmapToProcess = cropped
                                            } catch (e: IllegalArgumentException) {
                                                Log.e(TAG, "Error creating cropped bitmap: ${e.message}. Using original.", e)
                                            }
                                        } else {
                                            Log.w(TAG, "Calculated crop dimensions are invalid. Using original. W: $cropWidth, H: $cropHeight")
                                        }
                                    } else {
                                        Log.w(TAG, "OverlayView dimensions are zero or rect is empty. Using original image.")
                                    }
                                    
                                    capturedBitmap = bitmapToProcess
                                    performAuthenticityCheck(capturedBitmap!!)

                                } ?: run {
                                    Toast.makeText(baseContext, "Failed to load captured image.", Toast.LENGTH_SHORT).show()
                                    switchToCameraView()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during image loading/cropping: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(baseContext, "Error processing image.", Toast.LENGTH_SHORT).show()
                                switchToCameraView()
                            }
                        }
                    }
                }
            }
        )
    }

    private fun performAuthenticityCheck(bitmapToCheck: Bitmap) {
        val image = InputImage.fromBitmap(bitmapToCheck, 0)
        imageLabeler.process(image)
            .addOnSuccessListener { labels ->
                var isSuspicious = false
                for (label in labels) {
                    if (label.confidence >= LABEL_CONFIDENCE_THRESHOLD && label.text.equals("Paper", ignoreCase = true)) {
                        Log.w(TAG, "Suspicious label detected: ${label.text} (Confidence: ${label.confidence})")
                        isSuspicious = true
                        break
                    }
                }
                Log.d("Suspicious Label", labels.toString())
                if (!isSuspicious) {
                    Toast.makeText(this, "Please capture the physical ID card, not a photo or from a screen. Try again.", Toast.LENGTH_LONG).show()
                    switchToCameraView()
                } else {
                    Log.d(TAG, "Authenticity check passed (no highly confident suspicious labels).")
                    processImageWithMLKit(bitmapToCheck) // Proceed to text recognition
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Image Labeling failed: ${e.message}", e)
                // To be less restrictive, proceed to text recognition if labeling fails
                Toast.makeText(this, "Could not perform authenticity check. Proceeding with text scan.", Toast.LENGTH_SHORT).show()
                processImageWithMLKit(bitmapToCheck)
            }
    }

    private fun processImageWithMLKit(bitmapToAnalyze: Bitmap) {
        val image = InputImage.fromBitmap(bitmapToAnalyze, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isNotBlank()) {
                    textToSend = visionText.text
                    Log.d(TAG, "ML Kit detected text (first 100 chars): ${visionText.text.substring(0, Math.min(visionText.text.length, 100))}")
                    binding.capturedImagePreview.setImageBitmap(bitmapToAnalyze)
                    binding.capturedImagePreview.visibility = View.VISIBLE
                    binding.cameraContainer.visibility = View.GONE
                    binding.doneButton.text = getString(R.string.upload_picture)
                    binding.retakeButton.visibility = View.VISIBLE
                    binding.instructionTextView.text = "Review your photo"
                } else {
                    Log.d(TAG, "ML Kit: No text detected or image unclear.")
                    Toast.makeText(this, "No text detected or image unclear. Please try again.", Toast.LENGTH_LONG).show()
                    switchToCameraView()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ML Kit Text Recognition failed", e)
                Toast.makeText(this, "Text recognition failed: ${e.message}. Please try again.", Toast.LENGTH_LONG).show()
                switchToCameraView()
            }
    }

    private fun deleteLatestCapturedImage() {
        latestCapturedImageUri?.path?.let {
            val fileToDelete = File(it)
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.d(TAG, "Successfully deleted temp image file: $it")
                } else {
                    Log.e(TAG, "Failed to delete temp image file: $it")
                }
            }
        }
        latestCapturedImageUri = null
    }

    private fun uploadImageToServer(bitmapToUpload: Bitmap) {
        lifecycleScope.launch {
            var tempPngFile: File? = null
            val sourceUriForDeletion = latestCapturedImageUri 

            try {
                binding.loadingProgressBar.visibility = View.VISIBLE
                binding.doneButton.isEnabled = false
                binding.retakeButton.isEnabled = false

                tempPngFile = bitmapToPngFileForUpload(bitmapToUpload, "upload_image.png")
                if (tempPngFile == null) {
                    Toast.makeText(this@OCRActivity, "Failed to prepare image for upload (PNG conversion)", Toast.LENGTH_SHORT).show()
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.doneButton.isEnabled = true
                    binding.retakeButton.isEnabled = true
                    return@launch
                }

                val requestFile = tempPngFile.asRequestBody("image/png".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", tempPngFile.name, requestFile)
                val descriptionText = "ID Card Image (CameraX Cropped)"
                val descriptionPart = descriptionText.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = withContext(Dispatchers.IO) {
                    apiService.uploadImage(imagePart, descriptionPart, textToSend)
                }

                binding.loadingProgressBar.visibility = View.GONE
                binding.doneButton.isEnabled = true
                binding.retakeButton.isEnabled = true

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val ocrData = apiResponse?.message
                    if (ocrData != null) {
                        Toast.makeText(this@OCRActivity, "Upload successful! Processing data...", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@OCRActivity, OcrResultActivity::class.java).apply {
                            putExtra(OcrResultActivity.EXTRA_OCR_RESPONSE, Gson().toJson(ocrData))
                        }
                        startActivity(intent)
                    } else {
                        val errorMessage = if (apiResponse == null) "Response body is null." else "Message in response is null."
                        Toast.makeText(this@OCRActivity, "Upload successful but no data received: $errorMessage", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Upload successful but no data received: $errorMessage")
                    }
                    switchToCameraView()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@OCRActivity, "Upload failed: $errorBody", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Upload failed: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload exception: ${e.message}", e)
                Toast.makeText(this@OCRActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                binding.loadingProgressBar.visibility = View.GONE
                binding.doneButton.isEnabled = true
                binding.retakeButton.isEnabled = true
            } finally {
                tempPngFile?.delete()
                if (binding.loadingProgressBar.isVisible) {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.doneButton.isEnabled = true
                    binding.retakeButton.isEnabled = true
                }
            }
        }
    }

    private fun bitmapToPngFileForUpload(bitmap: Bitmap, fileName: String): File? {
        return try {
            val file = File(cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            Log.d(TAG, "Bitmap successfully converted to PNG file: ${file.absolutePath}")
            file
        } catch (e: IOException) {
            Log.e(TAG, "Error converting bitmap to PNG file", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::textRecognizer.isInitialized) {
            textRecognizer.close()
        }
        if (::imageLabeler.isInitialized) {
            imageLabeler.close()
        }
    }
}
