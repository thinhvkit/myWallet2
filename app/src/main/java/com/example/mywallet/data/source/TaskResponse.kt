package com.example.mywallet.data.source

import com.example.mywallet.data.Task

data class TaskResponse(
    val data: List<Task>? = null,
    val status: String = ""
)