// UserRepository.kt
package com.example.qwe

import com.example.qwe.data.CreateUserRequest
import com.example.qwe.data.UsersResponse

class UserRepository {
    private val api = ApiClient.instance

    suspend fun getUsers(): UsersResponse = api.getUsers()
    suspend fun createUser(name: String, job: String) = api.createUser(CreateUserRequest(name, job))
    suspend fun updateUser(id: Int, name: String, job: String) = api.updateUser(id, CreateUserRequest(name, job))
    suspend fun deleteUser(id: Int) = api.deleteUser(id)
}