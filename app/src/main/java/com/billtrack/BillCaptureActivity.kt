package com.billtrack

import android.Manifest
import android.app.Activity
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.billtrack.databinding.ActivityBillCaptureBinding
import com.billtrack.model.FindTotalRequest
import com.billtrack.utils.BillTextParser
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BillCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBillCaptureBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var textRecognizer: TextRecognizer
    private var latestCapturedImageUri: Uri? = null
    private var currentCapturedBitmap: Bitmap? = null

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

    companion object {
        private const val TAG = "BillCaptureActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val EXTRA_FOUND_TOTAL_DOUBLE = "com.billtrack.EXTRA_FOUND_TOTAL_DOUBLE"
        const val EXTRA_FOUND_CURRENCY = "com.billtrack.EXTRA_FOUND_CURRENCY"
        const val EXTRA_RAW_CAPTURED_TEXT = "com.billtrack.EXTRA_RAW_CAPTURED_TEXT"
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to capture bills.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupButtonClickListeners()
        switchToCameraView()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.billPreviewView.surfaceProvider)
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

    private fun setupButtonClickListeners() {
        binding.captureBillButton.setOnClickListener { takePhoto() }
        binding.retakeBillButton.setOnClickListener { switchToCameraView() }
        binding.uploadButton.setOnClickListener {
            currentCapturedBitmap?.let {
                processAndUpload(it)
            } ?: Toast.makeText(this, "No image captured to upload.", Toast.LENGTH_SHORT).show()
        }
        binding.addNoteButton.setOnClickListener {
            Toast.makeText(this, "Add Note clicked (placeholder)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processAndUpload(bitmap: Bitmap) {
        binding.uploadButton.isEnabled = false
        binding.retakeBillButton.isEnabled = false

        recognizeTextInternal(bitmap,
            onSuccess = { recognizedText ->
                if (recognizedText.isNotBlank()) {
                    val lines = recognizedText.lines()
                    val potentialTotals = lines.map { it.trim() }.filter { BillTextParser.checkFormat(it) && it.isNotBlank() }

                    if (potentialTotals.isNotEmpty()) {
                        showTotalSelectionDialog(potentialTotals, recognizedText)
                    } else {
                        Toast.makeText(this, "No specific monetary lines found. Sending all text.", Toast.LENGTH_LONG).show()
                        findTotalFromText(recognizedText) // Send full text if no specific lines found
                    }
                } else {
                    Toast.makeText(this, "No text found on the bill. Please retake.", Toast.LENGTH_LONG).show()
                    binding.uploadButton.isEnabled = true
                    binding.retakeBillButton.isEnabled = true
                }
            },
            onFailure = {
                Toast.makeText(this, "Could not recognize text. Please retake.", Toast.LENGTH_LONG).show()
                binding.uploadButton.isEnabled = true
                binding.retakeBillButton.isEnabled = true
            }
        )
    }

    private fun showTotalSelectionDialog(options: List<String>, fullRawText: String) {
        val items = options.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select the Total Amount")
            .setItems(items) { dialog, which ->
                val selectedOption = items[which]
                Toast.makeText(this, "Total pengeluaran anda: $selectedOption disimpan", Toast.LENGTH_SHORT).show()
                findTotalFromText(selectedOption) // Send only the selected monetary string
                dialog.dismiss()
            }
            .setNegativeButton("Use Full Text") { dialog, _ ->
                Log.d(TAG, "User opted to use full text.")
                findTotalFromText(fullRawText) // Send the full original text
                dialog.dismiss()
            }
            .setNeutralButton("Cancel") { dialog, _ ->
                Toast.makeText(this, "Selection cancelled.", Toast.LENGTH_SHORT).show()
                binding.uploadButton.isEnabled = true
                binding.retakeBillButton.isEnabled = true
                dialog.dismiss()
            }
            .setOnCancelListener { // Handles back button press or tapping outside
                Toast.makeText(this, "Selection cancelled.", Toast.LENGTH_SHORT).show()
                binding.uploadButton.isEnabled = true
                binding.retakeBillButton.isEnabled = true
            }
            .show()
    }

    private fun recognizeTextInternal(bitmap: Bitmap, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d(TAG, "ML Kit Text (internal): ${visionText.text.take(500)}")
                onSuccess(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ML Kit Text Recognition failed (internal)", e)
                onFailure()
            }
    }

    private fun findTotalFromText(textToProcess: String) {
        // Buttons are typically disabled before calling this.
        // Toast for "Finding total..." is handled by processAndUpload or can be added here if needed.
        Log.d(TAG, "Finding total from text: '$textToProcess'")

        lifecycleScope.launch {
            try {
                val request = FindTotalRequest(text = textToProcess)
//                val response = apiService.findBillTotal(request)
//
//                if (response.isSuccessful) {
//                    val findTotalResponse = response.body()
//                    if (findTotalResponse != null) {
//                        if (findTotalResponse.total != null && findTotalResponse.currency != null) {
//                            val resultIntent = Intent().apply {
//                                putExtra(EXTRA_FOUND_TOTAL_DOUBLE, findTotalResponse.total)
//                                putExtra(EXTRA_FOUND_CURRENCY, findTotalResponse.currency)
//                                // Send back the text that was actually processed by the API
//                                putExtra(EXTRA_RAW_CAPTURED_TEXT, textToProcess)
//                            }
//                            setResult(Activity.RESULT_OK, resultIntent)
//                            Toast.makeText(this@BillCaptureActivity, "Total Found: ${findTotalResponse.currency} ${findTotalResponse.total}", Toast.LENGTH_LONG).show()
//                            finish()
//                        } else if (findTotalResponse.error != null) {
//                            Toast.makeText(this@BillCaptureActivity, "Server: ${findTotalResponse.error}", Toast.LENGTH_LONG).show()
//                        } else {
//                            Toast.makeText(this@BillCaptureActivity, "Could not determine total from bill.", Toast.LENGTH_LONG).show()
//                        }
//                    } else {
//                        Toast.makeText(this@BillCaptureActivity, "Error: Empty response from server.", Toast.LENGTH_LONG).show()
//                    }
//                } else {
//                    val errorBody = response.errorBody()?.string() ?: "Unknown API error"
//                    Toast.makeText(this@BillCaptureActivity, "API Error: $errorBody", Toast.LENGTH_LONG).show()
//                    Log.e(TAG, "API Error: ${response.code()} - $errorBody")
//                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error or exception during findTotal: ${e.message}", e)
                Toast.makeText(this@BillCaptureActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                if (!isFinishing) {
                    binding.uploadButton.isEnabled = true
                    binding.retakeBillButton.isEnabled = true
                }
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val photoFile = File(cacheDir, "${name}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        binding.captureBillButton.isEnabled = false
        imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                runOnUiThread {
                    Toast.makeText(baseContext, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                    binding.captureBillButton.isEnabled = true
                }
            }
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                latestCapturedImageUri = Uri.fromFile(photoFile)
                lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap: Bitmap? = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(contentResolver, latestCapturedImageUri!!)
                            ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
                        } else {
                            @Suppress("DEPRECATION")
                            contentResolver.openInputStream(latestCapturedImageUri!!)
                                ?.use { BitmapFactory.decodeStream(it) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load bitmap from URI: $latestCapturedImageUri", e)
                        null
                    }
                    withContext(Dispatchers.Main) {
                        bitmap?.let {
                            currentCapturedBitmap = it
                            binding.capturedBillImageView.setImageBitmap(it)
                            switchToPreviewView()
                        } ?: run {
                            Toast.makeText(this@BillCaptureActivity, "Failed to load captured image.", Toast.LENGTH_SHORT).show()
                            switchToCameraView()
                        }
                        binding.captureBillButton.isEnabled = true
                    }
                }
            }
        })
    }

    private fun switchToCameraView() {
        currentCapturedBitmap = null
        binding.billPreviewView.visibility = View.VISIBLE
        binding.captureBillButton.visibility = View.VISIBLE
        binding.captureBillButton.isEnabled = true
        binding.capturedBillImageView.visibility = View.GONE
        binding.extractedBillTextScrollview.visibility = View.GONE
        binding.postCaptureButtonsLayout.visibility = View.GONE
        binding.retakeBillButton.visibility = View.GONE
        binding.capturedBillImageView.setImageDrawable(null)
        deleteLatestCapturedImageFile()
    }

    private fun switchToPreviewView() {
        binding.capturedBillImageView.visibility = View.VISIBLE
        binding.postCaptureButtonsLayout.visibility = View.VISIBLE
        binding.retakeBillButton.visibility = View.VISIBLE
        binding.uploadButton.isEnabled = true
        binding.retakeBillButton.isEnabled = true
        binding.billPreviewView.visibility = View.GONE
        binding.captureBillButton.visibility = View.GONE
        binding.extractedBillTextScrollview.visibility = View.GONE
    }

    private fun deleteLatestCapturedImageFile() {
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::textRecognizer.isInitialized) {
            textRecognizer.close()
        }
        currentCapturedBitmap = null
    }
}
