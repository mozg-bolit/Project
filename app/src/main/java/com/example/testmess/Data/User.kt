package com.example.testmess.Data

data class User(
    var id: String = "",
    val userId: String,
    val email: String,
    val password: String,
    val name: String,
    val surname: String,
    val patronymic: String,
    val role: String
)
