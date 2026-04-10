package com.phishblock.models

data class Message(
    val text: String,
    val timestamp: Long,
    var isMalicious: Boolean = false,
    var riskScore: Int = 0,
    var reason: String = ""
)