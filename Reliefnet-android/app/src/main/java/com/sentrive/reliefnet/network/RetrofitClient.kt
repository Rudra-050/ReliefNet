package com.sentrive.reliefnet.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private var retrofit: Retrofit? = null
    private const val TAG = "RetrofitClient"
    
    // Optional global auth token used by some repository calls
    @Volatile
    var authToken: String? = null
    
    private fun getHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                val token = authToken
                
                // Log interceptor activity
                Log.d(TAG, "🔍 Interceptor check for ${original.url.encodedPath}")
                Log.d(TAG, "  - authToken available: ${!token.isNullOrBlank()}")
                Log.d(TAG, "  - Existing Auth header: ${original.header("Authorization")}")
                
                if (!token.isNullOrBlank() && original.header("Authorization") == null) {
                    builder.header("Authorization", "Bearer $token")
                    Log.d(TAG, "  ✅ Added Authorization header")
                } else if (original.header("Authorization") != null) {
                    Log.d(TAG, "  ⚠️ Already has Authorization header, skipping")
                } else {
                    Log.d(TAG, "  ⚠️ No token available, no Authorization added")
                }
                
                chain.proceed(builder.build())
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    fun getClient(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(getHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
    
    val apiService: ApiService by lazy {
        getClient().create(ApiService::class.java)
    }
}
