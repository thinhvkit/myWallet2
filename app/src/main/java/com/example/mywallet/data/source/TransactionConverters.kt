package com.example.mywallet.data.source

import androidx.room.TypeConverter
import com.example.mywallet.data.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransactionConverters {
    @TypeConverter
    fun fromTransactionList(value: List<Transaction>): String {
        val gson = Gson()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toTransactionList(value: String): List<Transaction> {
        val gson = Gson()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(value, type)
    }
}