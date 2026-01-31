package com.example.traverse2.data.api

import android.content.Context
import com.example.traverse2.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
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
    
    // Cache configuration - 10 MB
    private var cache: Cache? = null
    
    // Interceptor to add cache headers to responses for cacheable endpoints
    private val cacheInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Cache GET requests for stats and read-only data (1 minute cache)
        val path = request.url.encodedPath
        val shouldCache = request.method == "GET" && (
            path.contains("/stats") ||
            path.contains("/achievements") ||
            path.contains("/friends") && !path.contains("/request")
        )
        
        if (shouldCache) {
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", "public, max-age=60") // 1 minute cache
                .build()
        } else {
            response
        }
    }
    
    // Offline cache interceptor - serve stale data when offline
    private val offlineCacheInterceptor = Interceptor { chain ->
        var request = chain.request()
        
        // If offline, try to use cached response (up to 10 days old)
        if (!isNetworkAvailable) {
            val cacheControl = CacheControl.Builder()
                .maxStale(10, TimeUnit.DAYS)
                .build()
            request = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
        }
        
        chain.proceed(request)
    }
    
    // Network availability flag (updated by app)
    @Volatile
    private var isNetworkAvailable = true
    
    fun setNetworkAvailable(available: Boolean) {
        isNetworkAvailable = available
    }
    
    private var okHttpClient: OkHttpClient? = null
    private var retrofit: Retrofit? = null
    private var _api: TraverseApi? = null
    
    /**
     * Initialize RetrofitClient with application context for caching.
     * Call this from Application.onCreate() or MainActivity.onCreate()
     */
    fun init(context: Context) {
        if (okHttpClient != null) return // Already initialized
        
        // Setup cache directory
        val cacheDir = File(context.cacheDir, "http_cache")
        cache = Cache(cacheDir, 10L * 1024L * 1024L) // 10 MB cache
        
        okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .cookieJar(cookieJar)
            .addInterceptor(offlineCacheInterceptor)
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient!!)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        
        _api = retrofit!!.create(TraverseApi::class.java)
    }
    
    // Lazy initialization fallback (without cache) if init() not called
    private val fallbackOkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val fallbackRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(fallbackOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    private val fallbackApi: TraverseApi by lazy {
        fallbackRetrofit.create(TraverseApi::class.java)
    }
    
    val api: TraverseApi
        get() = _api ?: fallbackApi
    
    // Clear cookies (for logout)
    fun clearCookies() {
        cookieStore.clear()
    }
    
    // Clear HTTP cache
    fun clearCache() {
        try {
            cache?.evictAll()
        } catch (e: Exception) {
            // Ignore cache clear errors
        }
    }
    
    // Check if user has auth cookie
    fun hasAuthCookie(): Boolean {
        return cookieStore.values.flatten().any { it.name == "token" }
    }
}
