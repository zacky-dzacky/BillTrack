package com.billtrack

import com.google.gson.annotations.SerializedName

// New wrapper class for the API response
data class ApiResponse(
    val message: OcrData?
)

// Renamed from OcrResponse to OcrData, representing the nested object
data class OcrData(
    @SerializedName("card_type") val cardType: String?,
    val province: String?,
    val region: String?,
    @SerializedName("identity_information") val identityInformation: IdentityInformation?,
    @SerializedName("photo_description") val photoDescription: PhotoDescription?,
    val signature: String?
)

data class IdentityInformation(
    val nik: String?,
    val nama: String?,
    @SerializedName("tempat_tanggal_lahir") val tempatTanggalLahir: String?,
    @SerializedName("jenis_kelamin") val jenisKelamin: String?,
    @SerializedName("gol_darah") val golDarah: String?,
    @SerializedName("Alamat") val alamat: String?, // Note: API uses uppercase 'A'
    @SerializedName("rt_rw") val rtRw: String?,
    @SerializedName("kelurahan_desa") val kelurahanDesa: String?,
    val kecamatan: String?,
    val agama: String?,
    @SerializedName("status_pekerjaan") val statusPekerjaan: String?,
    val pekerjaan: String?,
    val citizenship: String?,
    @SerializedName("valid_until") val validUntil: String?
)

data class PhotoDescription(
    val person: String?,
    val style: String?
)
