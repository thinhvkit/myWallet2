package com.example.mywallet.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mywallet.data.Task
import com.example.mywallet.data.source.TransactionConverters

@Database(entities = [Task::class], version = 1, exportSchema = true)
@TypeConverters(TransactionConverters::class)
abstract class WalletDatabase : RoomDatabase() {

    abstract fun taskDao(): TasksDao
}