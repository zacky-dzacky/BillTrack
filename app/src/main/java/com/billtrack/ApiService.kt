package com.billtrack

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("upload")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("image") description: RequestBody, // Example of an additional part
        @Part("text") name: String // Example of an additional part
    ): Response<ApiResponse> // Changed to expect ApiResponse
}
