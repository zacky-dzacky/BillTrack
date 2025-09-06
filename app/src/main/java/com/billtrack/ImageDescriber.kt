package com.billtrack

import android.graphics.Bitmap
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.imagedescription.ImageDescriber
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest

// Create an image describer
val options = ImageDescriberOptions.builder(context).build()
val imageDescriber = ImageDescription.getClient(options)

suspend fun prepareAndStartImageDescription(
    bitmap: Bitmap
) {
    // Check feature availability, status will be one of the following:
    // UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE
    val featureStatus = imageDescriber.checkFeatureStatus().await()

    if (featureStatus == FeatureStatus.DOWNLOADABLE) {
        // Download feature if necessary.
        // If downloadFeature is not called, the first inference request
        // will also trigger the feature to be downloaded if it's not
        // already downloaded.
        imageDescriber.downloadFeature(object : DownloadCallback {
            override fun onDownloadStarted(bytesToDownload: Long) { }

            override fun onDownloadFailed(e: GenAiException) { }

            override fun onDownloadProgress(totalBytesDownloaded: Long) {}

            override fun onDownloadCompleted() {
                startImageDescriptionRequest(bitmap, imageDescriber)
            }
        })
    } else if (featureStatus == FeatureStatus.DOWNLOADING) {
        // Inference request will automatically run once feature is
        // downloaded.
        // If Gemini Nano is already downloaded on the device, the
        // feature-specific LoRA adapter model will be downloaded
        // very quickly. However, if Gemini Nano is not already
        // downloaded, the download process may take longer.
        startImageDescriptionRequest(bitmap, imageDescriber)
    } else if (featureStatus == FeatureStatus.AVAILABLE) {
        startImageDescriptionRequest(bitmap, imageDescriber)
    }
}

fun startImageDescriptionRequest(
    bitmap: Bitmap,
    imageDescriber: ImageDescriber
) {
    // Create task request
    val imageDescriptionRequest = ImageDescriptionRequest
        .builder(bitmap)
        .build()

    // Run inference with a streaming callback
    val imageDescriptionResultStreaming =
        imageDescriber.runInference(imageDescriptionRequest) { outputText ->
            // Append new output text to show in UI
            // This callback is called incrementally as the description
            // is generated
        }

    // You can also get a non-streaming response from the request
    // val imageDescription = imageDescriber.runInference(
//        imageDescriptionRequest).await().description
}

// Be sure to release the resource when no longer needed
// For example, on viewModel.onCleared() or activity.onDestroy()
imageDescriber.close()