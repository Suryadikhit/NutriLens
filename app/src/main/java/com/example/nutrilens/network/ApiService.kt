package com.example.nutrilens.network

import com.example.nutrilens.viewmodel.Product
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// ApiService Interface
interface ApiService {
    @GET("product/{barcode}")
    suspend fun getProduct(@Path("barcode") barcode: String): Product
}

object RetrofitClient {
    private const val BASE_URL = "https://your-app.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY  // Log request and response body
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)  // Set the custom OkHttpClient with logging
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java) // Create the ApiService instance
    }
}
