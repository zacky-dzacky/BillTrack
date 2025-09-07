package com.billtrack.model

data class FindTotalResponse(
    val total: Double?,
    val currency: String?,
    val error: String? // For any error messages from the backend
)
