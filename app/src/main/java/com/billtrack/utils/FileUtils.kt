package com.billtrack.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    private const val TAG = "FileUtils"
    private const val IMAGE_DIRECTORY_NAME = "bill_images"

    /**
     * Copies an image from a given Uri (typically from cache or external provider) 
     * to a permanent location within the app's internal storage.
     *
     * @param context The application context.
     * @param imageUri The Uri of the image to copy.
     * @param desiredNamePrefix A prefix for the new file name (e.g., "bill_"). A timestamp will be appended.
     * @return The absolute path of the saved image file, or null if saving fails.
     */
    fun copyImageToInternalStorage(context: Context, imageUri: Uri, desiredNamePrefix: String): String? {
        val contentResolver = context.contentResolver
        val imageDir = File(context.filesDir, IMAGE_DIRECTORY_NAME)

        if (!imageDir.exists()) {
            if (!imageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: ${imageDir.absolutePath}")
                return null
            }
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
        val fileName = "${desiredNamePrefix}${timeStamp}.jpg"
        val destinationFile = File(imageDir, fileName)

        try {
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    val buffer = ByteArray(4 * 1024) // 4K buffer
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                    Log.d(TAG, "Image successfully copied to: ${destinationFile.absolutePath}")
                    return destinationFile.absolutePath
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error copying image to internal storage", e)
            // Clean up the partially created file if an error occurs
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception reading from URI: $imageUri", e)
        }
        return null
    }
}
