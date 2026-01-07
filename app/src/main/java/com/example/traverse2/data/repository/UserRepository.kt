package com.example.traverse2.data.repository

import com.example.traverse2.data.api.TraverseApi
import com.example.traverse2.data.model.User
import com.example.traverse2.data.api.SolveStats
import retrofit2.Response

class UserRepository(private val api: TraverseApi) {
    
    suspend fun getCurrentUser(): Result<User> {
        return try {
            val response = api.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.user)
            } else {
                Result.failure(Exception("Failed to get user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSolveStats(): Result<SolveStats> {
        return try {
            val response = api.getSolveStats()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.stats)
            } else {
                Result.failure(Exception("Failed to get stats: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
