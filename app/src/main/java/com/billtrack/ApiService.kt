package com.billtrack

import com.billtrack.model.FindTotalRequest
import com.billtrack.model.FindTotalResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("upload") // Assuming this is for image + text upload for OCR ID cards as per previous context
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("image") description: RequestBody, 
        @Part("text") name: String 
    ): Response<ApiResponse> // Make sure ApiResponse is defined somewhere, e.g. in models

    @POST("/api/bill/find-total") // New endpoint for finding total in a bill
    suspend fun findBillTotal(
        @Body request: FindTotalRequest
    ): Response<FindTotalResponse>
}
