package com.example.testmess

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.testmess.databinding.ActivityRegusterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegusterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegusterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.regButton.setOnClickListener {
            if (binding.editRegPassword.text.toString().trim().length <= 6) {
                Toast.makeText(this, "Пароль должен быть более 6 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.editRegName.text.toString().trim().isEmpty() ||
                binding.editRegSurname.text.toString().trim().isEmpty() ||
                binding.editRegPatronymic.text.toString().trim().isEmpty() ||
                binding.editRegEmailAddress.text.toString().trim().isEmpty() ||
                binding.editRegPassword.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = binding.editRegEmailAddress.text.toString().trim()
            val password = binding.editRegPassword.text.toString().trim()

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                        // Сохраняем в Users (для обычных пользователей)
                        val userInfo = hashMapOf(
                            "email" to email,
                            "name" to binding.editRegName.text.toString().trim(),
                            "surname" to binding.editRegSurname.text.toString().trim(),
                            "patronymic" to binding.editRegPatronymic.text.toString().trim(),
                            "role" to "user" // Добавляем роль по умолчанию
                        )

                        FirebaseDatabase.getInstance().getReference()
                            .child("Users").child(userId)
                            .setValue(userInfo)
                            .addOnSuccessListener {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}