package com.marlon.portalusuario.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.marlon.portalusuario.databinding.ActivityPrivacyBinding

class PrivacyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.webviewPrivacy
            .loadUrl("https://portalusuarioapp.blogspot.com/p/portal-usuario-mas-que-una-simple.html")
    }
}
