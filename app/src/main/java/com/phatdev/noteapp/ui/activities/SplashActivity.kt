package com.phatdev.noteapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "SplashActivity onCreate started")
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            
            Handler(Looper.getMainLooper()).postDelayed({
                navigateNext()
            }, 1500)
        } catch (e: Exception) {
            Log.e(TAG, "Error in SplashActivity", e)
            navigateNext()
        }
    }
    
    private fun navigateNext() {
        try {
            val auth = FirebaseAuth.getInstance()
            val intent = if (auth.currentUser != null) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error", e)

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
