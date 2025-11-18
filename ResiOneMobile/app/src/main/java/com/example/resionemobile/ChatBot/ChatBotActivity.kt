package com.example.resionemobile.ChatBot
import android.os.Bundle
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R

class ChatBotActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)  // ← Este es el layout que acabamos de crear

        supportActionBar?.title = "ResiBot - Asistente"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}