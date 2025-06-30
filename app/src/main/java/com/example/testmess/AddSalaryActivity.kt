package com.example.testmess

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.testmess.Data.Salary
import com.example.testmess.databinding.ActivityAddSalaryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddSalaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddSalaryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val peopleList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddSalaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupSpinners()
        loadPeopleData()

        binding.addSalaryButton.setOnClickListener {
            savePayment()
        }
    }

    private fun loadPeopleData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Вы не авторизированы")
            finish()
            return
        }

        val peopleRef = database.getReference("People")
        peopleRef.orderByChild("employerId").equalTo(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    peopleList.clear()
                    peopleList.add("Выберите сотрудника") // Default first item

                    for (personSnapshot in snapshot.children) {
                        val name = personSnapshot.child("name").getValue(String::class.java) ?: ""
                        val surname = personSnapshot.child("surname").getValue(String::class.java) ?: ""
                        val patronymic = personSnapshot.child("patronymic").getValue(String::class.java) ?: ""

                        val fullName = "$surname $name $patronymic".trim()
                        if (fullName.isNotEmpty()) {
                            peopleList.add(fullName)
                        }
                    }

                    // Update the spinner adapter
                    val peopleAdapter = ArrayAdapter(
                        this@AddSalaryActivity,
                        android.R.layout.simple_spinner_item,
                        peopleList
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    binding.spinnerPeople.adapter = peopleAdapter
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка загрузки данных сотрудников")
                }
            })
    }

    private fun setupSpinners() {
        val paymentType = arrayOf("Выберите", "Зарплата", "Премия", "Штраф", "Аванс", "Увольнение")

        val paymentTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            paymentType
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerTypeSalary.adapter = paymentTypeAdapter
        binding.spinnerTypeSalary.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position).toString()
                when(selected) {
                    "Зарплата" -> {
                        binding.editSalaryValue.hint = "Размер зарплаты"
                        binding.editTextDate.hint = "Дата выплаты"
                    }
                    "Премия" -> {
                        binding.editSalaryValue.hint = "Размер премии"
                        binding.editTextDate.hint = "Назначить дату выплаты"
                    }
                    "Штраф" -> {
                        binding.editSalaryValue.hint = "Размер штрафа"
                        binding.editTextDate.hint = "Дата взыскания"
                    }
                    "Аванс" -> {
                        binding.editSalaryValue.hint = "Размер аванса"
                        binding.editTextDate.hint = "Дата выплаты"
                    }
                    "Увольнение" -> {
                        binding.editSalaryValue.hint = "Размер увольнительной выплаты"
                        binding.editTextDate.hint = "Дата увольнения"
                        binding.editTextDate.isClickable = false
                    }
                    else -> {
                        binding.editSalaryValue.hint = "Размер выплаты"
                        binding.editTextDate.hint = "Дата выплаты"
                        binding.editTextDate.isClickable = true
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                showToast("Ничего не выбрано")
            }
        }
    }

    private fun savePayment() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Вы не авторизированы")
            finish()
            return
        }

        val peopleName: String = binding.spinnerPeople.selectedItem?.toString() ?: ""
        val paymentType: String = binding.spinnerTypeSalary.selectedItem?.toString() ?: ""
        val paymentValue: String = binding.editSalaryValue.text.toString().trim()
        val paymentDate: String = binding.editTextDate.text.toString().trim()

        if (peopleName.isEmpty() || peopleName == "Выберите сотрудника" ||
            paymentType.isEmpty() || paymentType == "Выберите" ||
            paymentValue.isEmpty() || paymentDate.isEmpty()) {
            showToast("Заполните все поля")
            return
        }

        val payment = Salary(
            peopleName = peopleName,
            type = paymentType,
            amount = paymentValue.toInt(),
            date = paymentDate,
            employerId = currentUser.uid
        )

        val paymentRef = database.getReference("Salary")
        val paymentId = paymentRef.push().key ?: ""

        paymentRef.child(paymentId)
            .setValue(payment)
            .addOnCompleteListener {
                showToast("Выплата добавлена")
                finish()
            }
            .addOnFailureListener {
                showToast("Ошибка добавления")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}