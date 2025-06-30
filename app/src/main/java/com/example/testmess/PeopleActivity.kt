package com.example.testmess

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.testmess.databinding.ActivityPeopleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PeopleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPeopleBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPeopleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bb.setOnClickListener {
            auth.signOut()
            finish()
        }

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Получаем UID работника из интента
        val personUid = intent.getStringExtra("PERSON_UID")

        if (personUid.isNullOrEmpty()) {
            // Пытаемся получить UID текущего пользователя
            val currentUserUid = auth.currentUser?.uid
            if (currentUserUid != null) {
                // Загружаем данные текущего пользователя
                loadPersonData(currentUserUid)
            } else {
                showErrorAndFinish("Ошибка: не указан сотрудник и пользователь не авторизован")
            }
        } else {
            // Загружаем данные по переданному UID
            loadPersonData(personUid)
        }
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e("PeopleActivity", message)
        finish()
    }

    private fun loadPersonData(personUid: String) {
        val personRef = database.reference.child("People").child(personUid)

        personRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    showErrorAndFinish("Данные сотрудника не найдены")
                    return
                }

                try {
                    // Получаем основные данные
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val surname = snapshot.child("surname").getValue(String::class.java) ?: ""
                    val patronymic = snapshot.child("patronymic").getValue(String::class.java) ?: ""
                    val job = snapshot.child("job").getValue(String::class.java) ?: "Не указана"
                    val salaryType = snapshot.child("salaryType").getValue(String::class.java) ?: "Не указан"
                    val salaryValue = snapshot.child("salaryValue").getValue(String::class.java) ?: "0"
                    val email = snapshot.child("email").getValue(String::class.java) ?: "Не указан"
                    val employerId = snapshot.child("employerId").getValue(String::class.java) ?: ""

                    // Форматируем ФИО
                    val fullName = "$surname $name $patronymic".trim()
                    binding.nameValue.text = if (fullName.isNotEmpty()) fullName else "Не указано"

                    binding.jobValue.text = job
                    binding.salaryTypeValue.text = salaryType

                    // Форматируем зарплату в зависимости от типа
                    val formattedSalary = when (salaryType) {
                        "Почасовая" -> "$salaryValue руб/час"
                        "Оклад" -> "$salaryValue руб/мес"
                        "Сдельная" -> "$salaryValue руб/ед"
                        else -> "$salaryValue руб"
                    }
                    binding.salaryValueValue.text = formattedSalary

                    binding.emailValue.text = email

                    // Загружаем имя работодателя
                    loadEmployerName(employerId)

                    // Загружаем текущую задачу
                    loadCurrentTask(personUid)
                } catch (e: Exception) {
                    showErrorAndFinish("Ошибка обработки данных: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showErrorAndFinish("Ошибка загрузки данных: ${error.message}")
            }
        })
    }

    private fun loadEmployerName(employerId: String) {
        if (employerId.isEmpty()) {
            binding.employerValue.text = "Не указан"
            return
        }

        val employerRef = database.reference.child("Users").child(employerId)

        employerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        val surname = snapshot.child("surname").getValue(String::class.java) ?: ""
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val patronymic = snapshot.child("patronymic").getValue(String::class.java) ?: ""

                        val fullName = "$surname $name $patronymic".trim()
                        binding.employerValue.text = if (fullName.isNotEmpty()) fullName else "Неизвестный работодатель"
                    } else {
                        binding.employerValue.text = "Неизвестный работодатель"
                    }
                } catch (e: Exception) {
                    binding.employerValue.text = "Ошибка обработки"
                    Log.e("PeopleActivity", "Employer data error: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.employerValue.text = "Ошибка загрузки"
                Log.e("PeopleActivity", "Employer load error: ${error.message}")
            }
        })
    }

    private fun loadCurrentTask(personUid: String) {
        // Получаем текущую задачу из узла "Tasck"
        val tasksRef = database.reference.child("Tasck")
            .orderByChild("assignedTo")
            .equalTo(personUid)
            .limitToLast(1) // Берем последнюю задачу

        tasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Скрываем прогресс-бар после загрузки всех данных

                if (snapshot.exists()) {
                    for (taskSnapshot in snapshot.children) {
                        val taskName = taskSnapshot.child("textTasck").getValue(String::class.java) ?: "Не указано"
                        binding.currentTaskValue.text = taskName
                        return
                    }
                }

                // Если задачи не найдены
                binding.currentTaskValue.text = "Нет текущих задач"
            }

            override fun onCancelled(error: DatabaseError) {
                binding.currentTaskValue.text = "Ошибка загрузки"
                Log.e("PeopleActivity", "Task load error: ${error.message}")
            }
        })
    }
}