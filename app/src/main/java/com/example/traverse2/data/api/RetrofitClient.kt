package com.example.traverse2.data.api

import com.example.traverse2.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // In-memory cookie storage for session management
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
    
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            if (cookieStore[host] == null) {
                cookieStore[host] = mutableListOf()
            }
            // Replace existing cookies with same name
            cookies.forEach { newCookie ->
                cookieStore[host]?.removeAll { it.name == newCookie.name }
                cookieStore[host]?.add(newCookie)
            }
        }
        
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    val api: TraverseApi = retrofit.create(TraverseApi::class.java)
    
    // Clear cookies (for logout)
    fun clearCookies() {
        cookieStore.clear()
    }
    
    // Check if user has auth cookie
    fun hasAuthCookie(): Boolean {
        return cookieStore.values.flatten().any { it.name == "token" }
    }
}
