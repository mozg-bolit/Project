package com.example.testmess

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testmess.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Получаем userId из интента
        userId = intent.getStringExtra("userId") ?: ""
        if (userId.isEmpty()) {
            showToast("Ошибка: не указан пользователь")
            finish()
            return
        }

        loadUserData()

        binding.buttonSave.setOnClickListener {
            saveUserData()
        }

        binding.buttonLogout.setOnClickListener {
            finish()
        }
    }

    private fun saveUserData() {
        val surname = binding.editSurname.text.toString().trim()
        val name = binding.editName.text.toString().trim()
        val patronymic = binding.editPatronymic.text.toString().trim()

        if (surname.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "surname" to surname,
            "patronymic" to patronymic
        )

        database.reference.child("Users").child(userId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Данные успешно сохранены", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserData() {
        database.reference.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val surname = snapshot.child("surname").getValue(String::class.java) ?: ""
                        val patronymic = snapshot.child("patronymic").getValue(String::class.java) ?: ""

                        binding.editSurname.setText(surname)
                        binding.editName.setText(name)
                        binding.editPatronymic.setText(patronymic)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка загрузки данных")
                }
            })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}