package com.noinupi.domain

data class Transaction(
    val upiId: String,
    val amount: String,
    val timestamp: Long,
    val status: String
)