package com.phatdev.noteapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

class CloudinaryRepository(private val context: Context) {

    companion object {
        private const val TAG = "CloudinaryRepository"
    }

    suspend fun uploadImage(uri: Uri): Result<String> = suspendCancellableCoroutine { cont ->
        try {
            val requestId = MediaManager.get().upload(uri)
                .unsigned("ml_default")
                .option("folder", "noteapp")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {
                        Log.d(TAG, "Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        Log.d(TAG, "Upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                        if (url != null) {
                            Log.d(TAG, "Upload success: $url")
                            cont.resume(Result.success(url))
                        } else {
                            cont.resume(Result.failure(Exception("URL không hợp lệ")))
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Log.e(TAG, "Upload error: ${error?.description}")
                        cont.resume(Result.failure(Exception(error?.description ?: "Upload thất bại")))
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        Log.w(TAG, "Upload rescheduled: ${error?.description}")
                    }
                })
                .dispatch()

            cont.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            cont.resume(Result.failure(e))
        }
    }

    suspend fun uploadBitmap(bitmap: Bitmap, fileName: String): Result<String> {
        return try {
            val tempFile = File(context.cacheDir, "$fileName.jpg")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            val uri = Uri.fromFile(tempFile)
            val result = uploadImage(uri)

            tempFile.delete()
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading bitmap", e)
            Result.failure(e)
        }
    }

    fun getThumbnailUrl(originalUrl: String, width: Int = 200): String {
        return originalUrl.replace("/upload/", "/upload/w_$width,c_scale/")
    }
}
