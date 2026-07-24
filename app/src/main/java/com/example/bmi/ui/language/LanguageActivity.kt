package com.example.bmi.ui.language

import android.os.Bundle
import com.example.bmi.BaseActivity
import com.example.bmi.databinding.ActivityLanguageBinding

class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
            finish()
        }
    }
}