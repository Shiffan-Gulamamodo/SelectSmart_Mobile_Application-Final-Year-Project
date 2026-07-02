package com.example.selectsmart_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.selectsmart_app.databinding.ActivityAuthBinding

// AuthActivity is the main activity used for the authentication section of the app.
// It acts as a container for screens such as Login, Register, and Forgot Password.
class AuthActivity : AppCompatActivity() {

    // ViewBinding object for activity_auth.xml
    // This allows the Kotlin code to access the XML layout safely without using findViewById.
    private lateinit var binding: ActivityAuthBinding

    // onCreate runs when this activity is first created/opened.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflates/loads the activity_auth.xml layout using ViewBinding.
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
