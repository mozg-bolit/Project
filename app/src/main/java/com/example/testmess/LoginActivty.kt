package com.example.testmess

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.testmess.databinding.ActivityLoginActivtyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivty : AppCompatActivity() {

    private lateinit var binding: ActivityLoginActivtyBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginActivtyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Убеждаемся, что пользователь вышел при открытии экрана входа
        auth.signOut()

        setupListeners()
    }

    private fun setupListeners() {
        binding.logButton.setOnClickListener {
            performLogin()
        }

        binding.regText.setOnClickListener {
            startActivity(Intent(this, RegusterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = binding.EmailAddress.text.toString().trim()
        val password = binding.Password.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }


        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    checkUserRole()
                } else {
                    Toast.makeText(
                        this,
                        "Ошибка входа: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun checkUserRole() {
        val currentUser = auth.currentUser ?: return


        database.child("Users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {


                    if (snapshot.exists()) {
                        val role = snapshot.child("role").getValue(String::class.java) ?: "user"
                        redirectBasedOnRole(role)
                    } else {
                        // Если нет в Users, проверяем People (для совместимости)
                        checkInPeopleBranch(currentUser.uid)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@LoginActivty, "Ошибка базы данных", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun checkInPeopleBranch(uid: String) {

        database.child("People").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {
                        val isEmployee = snapshot.child("employerId").exists()
                        redirectBasedOnRole(if (isEmployee) "employee" else "employer")
                    } else {
                        Toast.makeText(
                            this@LoginActivty,
                            "Данные пользователя не найдены",
                            Toast.LENGTH_SHORT
                        ).show()
                        auth.signOut()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@LoginActivty, "Ошибка базы данных", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun redirectBasedOnRole(role: String) {
        val intent = when (role) {
            "employee" -> Intent(this, PeopleActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }

        // Запускаем новую задачу и очищаем стек
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}