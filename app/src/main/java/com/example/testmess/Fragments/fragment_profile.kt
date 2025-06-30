package com.example.testmess.Fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.testmess.EditProfileActivity
import com.example.testmess.LoginActivty
import com.example.testmess.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class fragment_profile : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private var currentUserId: String = ""

    companion object {
        private const val THEME_PREF_KEY = "app_theme"
        private const val THEME_LIGHT = "light"
        private const val THEME_DARK = "dark"
        private const val THEME_SYSTEM = "system"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonLogin.setOnClickListener {
            startActivity(Intent(requireActivity(), LoginActivty::class.java))
            requireActivity().finish()
        }

        binding.redbutton.setOnClickListener {
            if (currentUserId.isNotEmpty()) {
                editProfile(currentUserId)
            } else {
                Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            }
        }

        binding.themeButton.setOnClickListener {
            showThemeSelectionDialog()
        }

        loadUserInfo()
    }

    private fun editProfile(userId: String) {
        val intent = Intent(requireActivity(), EditProfileActivity::class.java).apply {
            putExtra("userId", userId)
        }
        startActivity(intent)
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("Светлая тема", "Тёмная тема", "Системная тема")
        val currentTheme = sharedPreferences.getString(THEME_PREF_KEY, THEME_SYSTEM) ?: THEME_SYSTEM

        val checkedItem = when (currentTheme) {
            THEME_LIGHT -> 0
            THEME_DARK -> 1
            else -> 2
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Выбор темы")
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val selectedTheme = when (which) {
                    0 -> THEME_LIGHT
                    1 -> THEME_DARK
                    else -> THEME_SYSTEM
                }

                sharedPreferences.edit().putString(THEME_PREF_KEY, selectedTheme).apply()
                applyTheme(selectedTheme)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        requireActivity().recreate()
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            binding.usernameTv.text = "Гость"
            binding.fullName.text = "Пожалуйста, войдите в систему"
        } else {
            // Удаляем использование SharedPreferences для имени
            database.reference.child("Users").child(currentUser.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Всегда загружаем email
                            val email = snapshot.child("email").getValue(String::class.java) ?: ""
                            binding.usernameTv.text = email

                            // Всегда загружаем имя из Firebase
                            val name = snapshot.child("name").getValue(String::class.java) ?: ""
                            val surname = snapshot.child("surname").getValue(String::class.java) ?: ""
                            val patronymic = snapshot.child("patronymic").getValue(String::class.java) ?: ""

                            // Форматируем полное имя
                            binding.fullName.text = surname
                            binding.pJob.text = name
                            binding.pSalaryType.text = patronymic
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }


}

