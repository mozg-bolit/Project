package com.example.qwe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwe.data.User
import kotlinx.coroutines.launch
import android.util.Log

class UserViewModel : ViewModel() {
    private val repository = UserRepository()
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    companion object {
        private const val TAG = "UserViewModel"
    }

    fun loadUsers() {
        Log.d(TAG, "Loading users...")
        viewModelScope.launch {
            try {
                val response = repository.getUsers()
                Log.d(TAG, "Users loaded successfully. Count: ${response.data?.size ?: 0}")
                _users.value = response.data
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users", e)
                _errorMessage.value = "Error loading users: ${e.message}"
            }
        }
    }

    fun createUser(name: String, job: String) {
        Log.d(TAG, "Creating user: name=$name, job=$job")
        viewModelScope.launch {
            try {
                val response = repository.createUser(name, job)
                Log.d(TAG, "User created successfully. ID: ${response}")
                loadUsers()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating user", e)
                _errorMessage.value = "Error creating user: ${e.message}"
            }
        }
    }

    fun updateUser(id: Int, name: String, job: String) {
        Log.d(TAG, "Updating user ID $id: name=$name, job=$job")
        viewModelScope.launch {
            try {
                val response = repository.updateUser(id, name, job)
                Log.d(TAG, "User updated successfully. ID: $id")
                loadUsers()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user", e)
                _errorMessage.value = "Error updating user: ${e.message}"
            }
        }
    }

    fun deleteUser(id: Int) {
        Log.d(TAG, "Deleting user ID $id")
        viewModelScope.launch {
            try {
                repository.deleteUser(id)
                Log.d(TAG, "User deleted successfully. ID: $id")
                loadUsers()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting user", e)
                _errorMessage.value = "Error deleting user: ${e.message}"
            }
        }
    }
}