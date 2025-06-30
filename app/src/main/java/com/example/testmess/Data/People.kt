package com.example.testmess.Data

import com.example.testmess.databinding.FragmentSalaryBinding
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class People(
    var id: String = "",
    var name: String = "",
    var surname: String = "",
    var patronymic: String = "",
    var email: String = "",
    var password: String = "",
    var job: String = "",
    var salaryValue: String = "",
    var salaryType: String = "",
    var employerId: String = ""
)