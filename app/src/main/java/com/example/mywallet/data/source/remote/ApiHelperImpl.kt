package com.example.mywallet.data.source.remote

import com.example.mywallet.data.source.TaskResponse
import retrofit2.Response
import javax.inject.Inject

class ApiHelperImpl @Inject constructor(
    private val apiService: ApiService
) : ApiHelper {
    override suspend fun getTasks(): Response<TaskResponse> = apiService.getTasks()
}