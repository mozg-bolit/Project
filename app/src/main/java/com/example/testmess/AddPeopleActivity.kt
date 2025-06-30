package com.example.testmess

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.testmess.Data.People
import com.example.testmess.Data.User
import com.example.testmess.databinding.ActivityAddPeopleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class AddPeopleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPeopleBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var employerEmail: String? = null
    private var employerPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPeopleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Получаем данные текущего пользователя
        val currentUser = auth.currentUser
        employerEmail = currentUser?.email

        // Запрашиваем пароль работодателя
        showPasswordDialog()

        setupSpinner()
        setupBackPressHandler()

        binding.persButton.setOnClickListener {
            if (employerPassword == null) {
                showToast("Сначала подтвердите ваш пароль")
                showPasswordDialog()
            } else {
                savePersonData()
            }
        }
    }

    private fun showPasswordDialog() {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Введите ваш пароль"
        }

        AlertDialog.Builder(this)
            .setTitle("Подтверждение пароля")
            .setMessage("Для добавления сотрудника подтвердите ваш пароль")
            .setView(editText)
            .setPositiveButton("Подтвердить") { _, _ ->
                employerPassword = editText.text.toString().trim()
            }
            .setNegativeButton("Отмена") { _, _ ->
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun setupSpinner() {
        val salaryTypes = arrayOf("Выберите", "Почасовая", "Сдельная", "Оклад")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            salaryTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.editSpinnerST.adapter = adapter

        binding.editSpinnerST.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (parent?.getItemAtPosition(position).toString()) {
                    "Почасовая" -> binding.editSalaryValue.hint = "Плата в час"
                    "Сдельная" -> binding.editSalaryValue.hint = "Плата за изделие"
                    "Оклад" -> binding.editSalaryValue.hint = "Размер оклада"
                    else -> binding.editSalaryValue.hint = "Величина заработной платы"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = showToast("Ничего не выбрано")
        }
    }

    private fun savePersonData() {
        val currentUser = auth.currentUser ?: run {
            showToast("Вы не авторизованы")
            finish()
            return
        }

        val name = binding.editPersName.text.toString().trim()
        val surname = binding.editPersSurname.text.toString().trim()
        val patronymic = binding.editPersPatronymic.text.toString().trim()
        val email = binding.editPersEmailAddress.text.toString().trim()
        val password = binding.editPersPassword.text.toString().trim()
        val job = binding.editJob.text.toString().trim()
        val salaryValue = binding.editSalaryValue.text.toString().trim()
        val salaryType = binding.editSpinnerST.selectedItem.toString()

        // Валидация
        if (listOf(name, surname, email, password).any { it.isEmpty() }) {
            showToast("Заполните обязательные поля")
            return
        }
        if (password.length < 6) {
            showToast("Пароль должен быть не менее 6 символов")
            return
        }
        if (salaryType == "Выберите") {
            showToast("Выберите тип зарплаты")
            return
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                val newUser = authTask.result?.user
                newUser?.let { user ->
                    // Устанавливаем имя пользователя
                    UserProfileChangeRequest.Builder()
                        .setDisplayName("$surname $name")
                        .build().let { profileUpdates ->
                            user.updateProfile(profileUpdates).addOnCompleteListener {
                                saveUserData(
                                    userId = user.uid,
                                    employerId = currentUser.uid,
                                    name = name,
                                    surname = surname,
                                    patronymic = patronymic,
                                    email = email,
                                    password = password,
                                    job = job,
                                    salaryValue = salaryValue,
                                    salaryType = salaryType
                                )

                                // Восстанавливаем сессию работодателя
                                restoreEmployerSession()
                            }
                        }
                }
            } else {
                showToast("Ошибка создания пользователя: ${authTask.exception?.message}")
            }
        }
    }

    private fun restoreEmployerSession() {
        val email = employerEmail
        val password = employerPassword

        if (email == null || password == null) {
            showToast("Ошибка: данные для восстановления недоступны")
            finish()
            return
        }

        auth.signOut()
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast("Сотрудник успешно добавлен")
                setResult(RESULT_OK)
            } else {
                showToast("Ошибка восстановления сессии: ${task.exception?.message}")
            }
            finish()
        }
    }

    private fun saveUserData(
        userId: String,
        employerId: String,
        name: String,
        surname: String,
        patronymic: String,
        email: String,
        password : String,
        job: String,
        salaryValue: String,
        salaryType: String
    ) {
        val user = User(
            userId = userId,
            email = email,
            password = password,
            name = name,
            surname = surname,
            patronymic = patronymic,
            role = "employee"
        )

        val people = People(
            name = name,
            surname = surname,
            patronymic = patronymic,
            email = email,
            password = password,
            job = job,
            salaryValue = salaryValue,
            salaryType = salaryType,
            employerId = employerId
        )

        database.reference.child("Users").child(userId).setValue(user)
        database.reference.child("People").child(userId).setValue(people)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}