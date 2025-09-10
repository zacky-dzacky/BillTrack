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
        // No need to set content view if your theme handles the splash background
        // or if activity_splash.xml is simple and primarily for the launch theme.
        // If activity_splash.xml contains specific UI elements you want to show during these 2s,
        // then setContentView(R.layout.activity_splash) is needed.
        // For a very basic splash, often a launch theme is preferred.
        // Let's assume you want to show the layout elements from activity_splash.xml:
        setContentView(R.layout.activity_splash)

        // Hide action bar if it's visible and you're not using a NoActionBar theme
        supportActionBar?.hide()

        firebaseAuth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, SPLASH_DELAY_MS)
    }

    private fun checkUserStatus() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // User is signed in, navigate to BillCaptureActivity
            navigateToActivity(BillCaptureActivity::class.java)
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
