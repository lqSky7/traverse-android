package com.example.traverse2

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.traverse2.data.api.RetrofitClient

class TraverseApplication : Application() {
    
    private lateinit var connectivityManager: ConnectivityManager
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            RetrofitClient.setNetworkAvailable(true)
        }
        
        override fun onLost(network: Network) {
            RetrofitClient.setNetworkAvailable(false)
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            RetrofitClient.setNetworkAvailable(hasInternet)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize RetrofitClient with application context for HTTP caching
        RetrofitClient.init(applicationContext)
        
        // Setup network connectivity monitoring for offline cache support
        setupNetworkMonitoring()
    }
    
    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        
        // Check initial network state
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        RetrofitClient.setNetworkAvailable(isConnected)
        
        // Register for network changes
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    override fun onTerminate() {
        super.onTerminate()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }
}
