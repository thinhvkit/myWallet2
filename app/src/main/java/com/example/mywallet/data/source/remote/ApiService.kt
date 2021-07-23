package com.example.mywallet.data.source.remote

import com.example.mywallet.data.source.TaskResponse
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {

    @GET("price/all_prices_for_mobile")
    suspend fun getTasks(): Response<TaskResponse>

}