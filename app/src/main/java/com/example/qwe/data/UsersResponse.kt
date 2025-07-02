package com.example.qwe.data

data class UsersResponse(
    val page: Int,
    val per_page: Int,
    val total: Int,
    val total_pages: Int,
    val data: List<User>  // Это поле содержит список пользователей
)