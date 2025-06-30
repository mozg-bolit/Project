package com.example.testmess

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testmess.Data.People
import com.example.testmess.databinding.ActivityEditPersonBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditPersonActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditPersonBinding
    private var personId: String = ""
    private var userEmail: String? = null // Store the email to find user in Users table

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPersonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener { finish() }

        // Get person ID from Intent
        personId = intent.getStringExtra("personId") ?: ""
        if (personId.isEmpty()) {
            showToast("Error: No person specified")
            finish()
            return
        }

        // Load person data
        loadPersonData()

        // Setup Spinner
        setupSpinner()

        binding.buttonSalaryUpdate.setOnClickListener {
            updatePersonData()
        }
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
        binding.salaryEditType.adapter = adapter

        binding.salaryEditType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateSalaryHint(parent?.getItemAtPosition(position).toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                showToast("Ничего не выбрано")
            }
        }
    }

    private fun updateSalaryHint(selectedType: String) {
        when(selectedType) {
            "Почасовая" -> binding.salaryValue.hint = "Плата в час"
            "Сдельная" -> binding.salaryValue.hint = "Плата за изделие"
            "Оклад" -> binding.salaryValue.hint = "Размер оклада"
            else -> binding.salaryValue.hint = "Величина заработной платы"
        }
    }

    private fun loadPersonData() {
        FirebaseDatabase.getInstance().getReference("People")
            .child(personId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showToast("Сотрудник не найден")
                        finish()
                        return
                    }

                    val person = snapshot.getValue(People::class.java)
                    person?.let {
                        // Store email for user lookup
                        userEmail = it.email

                        // Populate UI fields
                        binding.editName.setText(it.name)
                        binding.editSurname.setText(it.surname)
                        binding.editPatronumyc.setText(it.patronymic)
                        binding.job.setText(it.job)
                        binding.salaryValue.setText(it.salaryValue)

                        // Set spinner selection
                        val adapter = binding.salaryEditType.adapter as ArrayAdapter<String>
                        val position = adapter.getPosition(it.salaryType)
                        if (position >= 0) {
                            binding.salaryEditType.setSelection(position)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка загрузки: ${error.message}")
                }
            })
    }

    private fun updatePersonData() {
        val name = binding.editName.text.toString().trim()
        val surname = binding.editSurname.text.toString().trim()
        val patronymic = binding.editPatronumyc.text.toString().trim()
        val salaryType = binding.salaryEditType.selectedItem.toString()
        val salaryValue = binding.salaryValue.text.toString().trim()
        val job = binding.job.text.toString().trim()

        if (name.isEmpty() || surname.isEmpty() || salaryValue.isEmpty()) {
            showToast("Заполните обязательные поля")
            return
        }

        if (salaryType == "Выберите") {
            showToast("Выберите тип зарплаты")
            return
        }

        // Prepare updates for People
        val peopleUpdates = hashMapOf<String, Any>(
            "name" to name,
            "surname" to surname,
            "patronymic" to patronymic,
            "salaryType" to salaryType,
            "salaryValue" to salaryValue,
            "job" to job
        )

        // First update People
        FirebaseDatabase.getInstance().getReference("People")
            .child(personId)
            .updateChildren(peopleUpdates)
            .addOnSuccessListener {
                // Now update Users if we have email
                userEmail?.let { email ->
                    // Find user in Users table by email
                    FirebaseDatabase.getInstance().getReference("Users")
                        .orderByChild("email")
                        .equalTo(email)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    // Should be only one user with this email
                                    snapshot.children.firstOrNull()?.let { userSnapshot ->
                                        // Prepare user updates
                                        val userUpdates = hashMapOf<String, Any>(
                                            "name" to name,
                                            "surname" to surname,
                                            "patronymic" to patronymic
                                        )

                                        // Update the user
                                        userSnapshot.ref.updateChildren(userUpdates)
                                            .addOnSuccessListener {
                                                showToast("Данные обновлены в People и Users")
                                                finish()
                                            }
                                            .addOnFailureListener { e ->
                                                showToast("People updated but Users update failed: ${e.message}")
                                            }
                                    }
                                } else {
                                    showToast("People updated but no matching User found")
                                    finish()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                showToast("People updated but error searching Users: ${error.message}")
                            }
                        })
                } ?: run {
                    showToast("Данные в People обновлены (нет email для поиска в Users)")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                showToast("Ошибка при обновлении People: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}