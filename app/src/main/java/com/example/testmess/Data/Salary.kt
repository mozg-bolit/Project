package com.example.testmess.Data


data class Salary(
    var id: String = "",
    var peopleName: String = "",
    var type: String = "",
    var amount: Int = 0,
    var date: String = "",
    var employerId: String = "" // Критически важное поле!
)