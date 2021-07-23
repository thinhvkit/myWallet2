package com.example.mywallet.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "transaction")
data class Transaction @JvmOverloads constructor(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "date") var date: String = "",
    @ColumnInfo(name = "info") var info: String = "",
    @ColumnInfo(name = "increasing") var isIncreasing: Boolean = true
) {
    val getInfo: String
        get() = if (isIncreasing) "+$info" else "-$info"
}