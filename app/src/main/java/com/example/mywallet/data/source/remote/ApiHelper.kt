package com.example.mywallet.data.source.remote

import com.example.mywallet.data.source.TaskResponse
import retrofit2.Response

interface ApiHelper {
    suspend fun getTasks(): Response<TaskResponse>

}