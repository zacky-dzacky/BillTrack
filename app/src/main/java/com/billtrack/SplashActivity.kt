package com.billtrack

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen") // Suppress warning for custom splash screen
class SplashActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        private const val SPLASH_DELAY_MS = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        supportActionBar?.hide()

        firebaseAuth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, SPLASH_DELAY_MS)
    }

    private fun checkUserStatus() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // User is signed in, navigate to DashboardActivity
            navigateToActivity(DashboardActivity::class.java) // Changed from BillCaptureActivity
        } else {
            // No user is signed in, navigate to LoginActivity
            navigateToActivity(LoginActivity::class.java)
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish() // Finish SplashActivity so it can't be returned to
    }
}
