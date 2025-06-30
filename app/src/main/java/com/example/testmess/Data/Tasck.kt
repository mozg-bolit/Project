package com.example.testmess.Data
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Tasck(
    var id: String = "",
    var peopleNameTasck: String = "",
    var typeTasck: String = "",
    var textTasck: String = "",
    var dateTasck: String = "",
    var dateTaskComplete: String = "",
    var employerId: String = ""
)
