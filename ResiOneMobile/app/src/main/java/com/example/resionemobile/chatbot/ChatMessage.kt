package com.example.resionemobile.chatbot

data class ChatMessage(
    val message: String,
    val isSentByUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)