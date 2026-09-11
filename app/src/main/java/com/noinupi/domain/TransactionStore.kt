package com.noinupi.domain

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TransactionStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("transactions", Context.MODE_PRIVATE)

    fun save(transaction: Transaction) {
        val transactions = JSONArray(
            prefs.getString("list", "[]") ?: "[]"
        )

        val obj = JSONObject().apply {
            put("upiId", transaction.upiId)
            put("amount", transaction.amount)
            put("timestamp", transaction.timestamp)
            put("status", transaction.status)
        }

        transactions.put(obj)

        prefs.edit()
            .putString("list", transactions.toString())
            .apply()
    }

    fun getAll(): List<Transaction> {
        val transactions = JSONArray(
            prefs.getString("list", "[]") ?: "[]"
        )

        return buildList {
            for (i in 0 until transactions.length()) {
                val obj = transactions.getJSONObject(i)

                add(
                    Transaction(
                        upiId = obj.getString("upiId"),
                        amount = obj.getString("amount"),
                        timestamp = obj.getLong("timestamp"),
                        status = obj.getString("status")
                    )
                )
            }
        }.reversed()
    }
}