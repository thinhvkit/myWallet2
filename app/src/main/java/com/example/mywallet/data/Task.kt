package com.example.mywallet.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "tasks")
data class Task @JvmOverloads constructor(
    @ColumnInfo(name = "base") var base: String = "",
    @ColumnInfo(name = "counter") var counter: String = "",
    @ColumnInfo(name = "buy_price") var buyPrice: String = "",
    @ColumnInfo(name = "sell_price") var sellPrice: String = "",
    @ColumnInfo(name = "icon") var icon: String = "",
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "completed") var isCompleted: Boolean = false,
    @PrimaryKey @ColumnInfo(name = "entryid") var id: String = UUID.randomUUID().toString()
){
    val isEmpty
        get() = base.isEmpty() || name.isEmpty()
    val isActive
        get() = !isCompleted
}