package com.billtrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.billtrack.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        private const val TAG = "LoginActivity"
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { 
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google Sign-In successful. ID Token: ${account?.idToken?.take(20)}...")
                account?.let {
                    firebaseAuthWithGoogle(it)
                } ?: run {
                    Toast.makeText(this, "Google Sign-In failed: No account data.", Toast.LENGTH_SHORT).show()
                    showProgress(false)
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Google Sign-In failed ${e.message}")
                Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                showProgress(false)
            }
        } else {
            Log.w(TAG, "Google Sign-In not successful. Result code: ${result.resultCode}")
            Toast.makeText(this, "Google Sign-In was not completed.", Toast.LENGTH_SHORT).show()
            showProgress(false) 
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Changed to use string resource
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = Firebase.auth

        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        showProgress(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle an account from google: ${account.id}")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase Sign-In successful.")
                    val user = firebaseAuth.currentUser
                    Toast.makeText(this, "Sign-In Successful: ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    navigateToBillCaptureActivity()
                } else {
                    Log.w(TAG, "Firebase Sign-In failed", task.exception)
                    Toast.makeText(this, "Firebase Authentication Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    showProgress(false)
                }
            }
    }

    private fun navigateToBillCaptureActivity() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK 
        startActivity(intent)
        finish()
    }

    private fun showProgress(show: Boolean) {
        binding.loginProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.googleSignInButton.isEnabled = !show
    }

    override fun onStart() {
        super.onStart()
        // Safeguard handled by SplashActivity primarily.
        // If LoginActivity is reached and user is already logged in, 
        // SplashActivity should have ideally already redirected.
        // Consider if firebaseAuth.currentUser check here is still needed or could lead to unexpected navigation
        // if this activity is brought to foreground while already logged in.
        // For now, keeping it minimal as SplashActivity is the main gatekeeper.
    }
}
