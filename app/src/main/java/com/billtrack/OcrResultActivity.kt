package com.billtrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.billtrack.databinding.ActivityOcrResultBinding
import com.google.gson.Gson

class OcrResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcrResultBinding

    companion object {
        const val EXTRA_OCR_RESPONSE = "extra_ocr_response"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ocrResponseJson = intent.getStringExtra(EXTRA_OCR_RESPONSE)
        if (ocrResponseJson != null) {
            // Parse as OcrData, which is what MainActivity now sends
            val ocrData = Gson().fromJson(ocrResponseJson, OcrData::class.java)
            displayOcrData(ocrData)
        } else {
            // Handle the case where no data is passed, perhaps show an error or finish
            binding.tvCardType.text = "Error: No data received"
        }

        // Optional: Add a back button to the ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "OCR Result"
    }

    // Parameter type changed to OcrData
    private fun displayOcrData(data: OcrData) {
        binding.tvCardType.text = data.cardType ?: "-"
        binding.tvProvince.text = data.province ?: "-"
        binding.tvRegion.text = data.region ?: "-"

        data.identityInformation?.let {
            binding.tvNik.text = it.nik ?: "-"
            binding.tvNama.text = it.nama ?: "-"
            binding.tvTempatTanggalLahir.text = it.tempatTanggalLahir ?: "-"
            binding.tvJenisKelamin.text = it.jenisKelamin ?: "-"
            binding.tvGolDarah.text = it.golDarah ?: "-"
            binding.tvAlamat.text = it.alamat ?: "-"
            binding.tvRtRw.text = it.rtRw ?: "-"
            binding.tvKelurahanDesa.text = it.kelurahanDesa ?: "-"
            binding.tvKecamatan.text = it.kecamatan ?: "-"
            binding.tvAgama.text = it.agama ?: "-"
            binding.tvStatusPekerjaan.text = it.statusPekerjaan ?: "-"
            binding.tvPekerjaan.text = it.pekerjaan ?: "-"
            binding.tvCitizenship.text = it.citizenship ?: "-"
            binding.tvValidUntil.text = it.validUntil ?: "-"
        } ?: run {
            // Handle null identityInformation: Clear or set default text for all identity fields
            binding.tvNik.text = "-"
            binding.tvNama.text = "-"
            binding.tvTempatTanggalLahir.text = "-"
            binding.tvJenisKelamin.text = "-"
            binding.tvGolDarah.text = "-"
            binding.tvAlamat.text = "-"
            binding.tvRtRw.text = "-"
            binding.tvKelurahanDesa.text = "-"
            binding.tvKecamatan.text = "-"
            binding.tvAgama.text = "-"
            binding.tvStatusPekerjaan.text = "-"
            binding.tvPekerjaan.text = "-"
            binding.tvCitizenship.text = "-"
            binding.tvValidUntil.text = "-"
        }

        data.photoDescription?.let {
            binding.tvPhotoPerson.text = it.person ?: "-"
            binding.tvPhotoStyle.text = it.style ?: "-"
        } ?: run {
            binding.tvPhotoPerson.text = "-"
            binding.tvPhotoStyle.text = "-"
        }

        binding.tvSignature.text = data.signature ?: "-"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() // Explicitly finish the activity to ensure it's removed from backstack
        return true
    }
}
