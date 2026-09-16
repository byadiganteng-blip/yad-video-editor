package com.yad.videoeditor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LoginActivity — halaman login Google.
 *
 * Alur:
 *  1. User tap tombol "Masuk dengan Google"
 *  2. Google Sign-In flow → dapat idToken
 *  3. Firebase Auth login dengan idToken
 *  4. Save user ke Firestore
 *  5. Lanjut ke MainActivity
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        Tracker.lifecycle("LoginActivity", "onCreate")
        Tracker.authEvent("google", "signin_button_ready")

        findViewById<com.google.android.gms.common.SignInButton>(R.id.btnGoogleSignIn)?.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        Tracker.userAction("LoginActivity", "click_sign_in")
        val intent = googleSignInClient.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    handleGoogleLogin(idToken)
                } else {
                    Toast.makeText(this, "❌ ID Token null", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("LoginActivity", "Google sign in failed: ${e.statusCode}", e)
                Toast.makeText(this, "❌ Login gagal: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleLogin(idToken: String) {
        Tracker.authEvent("google", "id_token_received", mapOf(
            "token_prefix" to idToken.take(20) + "..."
        ))
        Toast.makeText(this, "⏳ Memproses...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            val ok = withContext(Dispatchers.IO) {
                // Login Firebase dengan Google credential
                val firebaseOk = FirebaseManager.loginWithGoogle(idToken)
                if (!firebaseOk) return@withContext false

                // Save user ke Firestore
                val uid = FirebaseManager.getCurrentUserUid() ?: return@withContext false
                val profile = UserProfile(
                    uid = uid,
                    email = FirebaseManager.getCurrentUserEmail() ?: "",
                    displayName = FirebaseManager.getCurrentUserName() ?: "",
                    photoUrl = FirebaseManager.getCurrentUserPhoto() ?: "",
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    appVersion = 15,
                    installedAt = System.currentTimeMillis(),
                    lastUsed = System.currentTimeMillis()
                )
                FirebaseManager.saveUserToFirestore(profile)
            }

            if (ok) {
                Toast.makeText(this@LoginActivity, "✅ Login berhasil", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this@LoginActivity, "❌ Gagal simpan data user", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
