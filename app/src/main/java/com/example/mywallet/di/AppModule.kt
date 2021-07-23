package com.example.mywallet.di

import android.content.Context
import androidx.room.Room
import com.example.mywallet.BuildConfig
import com.example.mywallet.data.source.DefaultTasksRepository
import com.example.mywallet.data.source.TasksDataSource
import com.example.mywallet.data.source.TasksRepository
import com.example.mywallet.data.source.local.TasksLocalDataSource
import com.example.mywallet.data.source.local.WalletDatabase
import com.example.mywallet.data.source.remote.ApiHelper
import com.example.mywallet.data.source.remote.ApiHelperImpl
import com.example.mywallet.data.source.remote.ApiService
import com.example.mywallet.data.source.remote.TasksRemoteDataSource
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindRepository(repo: DefaultTasksRepository): TasksRepository
}

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class TasksRemoteDataSource

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class TasksLocalDataSource

    @Provides
    fun provideBaseUrl() =
        "https://www.coinhako.com/api/v3/"

    @Provides
    fun provideGson(): Gson = GsonBuilder().create()

    @Singleton
    @Provides
    fun provideOkHttpClient() = if (BuildConfig.DEBUG) {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    } else {
        OkHttpClient
            .Builder()
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient, BASE_URL: String, gson: Gson): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(gson))
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit) = retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideApiHelper(apiHelper: ApiHelperImpl): ApiHelper = apiHelper

    @Singleton
    @TasksRemoteDataSource
    @Provides
    fun provideTasksRemoteDataSource(apiHelper: ApiHelper): TasksDataSource {
        return TasksRemoteDataSource(apiHelper)
    }

    @Singleton
    @TasksLocalDataSource
    @Provides
    fun provideTasksLocalDataSource(
        database: WalletDatabase,
        ioDispatcher: CoroutineDispatcher
    ): TasksDataSource {
        return TasksLocalDataSource(
            database.taskDao(), ioDispatcher
        )
    }

    @Singleton
    @Provides
    fun provideDataBase(@ApplicationContext context: Context): WalletDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            WalletDatabase::class.java,
            "Tasks.db"
        ).build()
    }

    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO
}