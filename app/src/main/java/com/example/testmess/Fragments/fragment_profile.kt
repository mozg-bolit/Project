package com.example.testmess.Fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.example.testmess.LoginActivty
import com.example.testmess.MainActivity
import com.example.testmess.R
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

        // Инициализация кнопки выхода
        binding.buttonLogin.setOnClickListener {
            startActivity(Intent(requireActivity(), LoginActivty::class.java))
            requireActivity().finish()
        }

        // Инициализация кнопки смены темы
        binding.themeButton.setOnClickListener {
            showThemeSelectionDialog()
        }

        loadUserInfo()
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

        // Перезапускаем активность для применения темы
        requireActivity().recreate()
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            binding.usernameTv.text = "Гость"
            binding.fullName.text = "Пожалуйста, войдите в систему"
        } else {
            database.reference.child("Users").child(currentUser.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            binding.usernameTv.text = "Данные не найдены"
                            return
                        }

                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val surname = snapshot.child("surname").getValue(String::class.java) ?: ""
                        val patronymic = snapshot.child("patronymic").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""

                        binding.usernameTv.text = email
                        binding.fullName.text = "$surname $name $patronymic"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        binding.usernameTv.text = "Ошибка загрузки"
                    }
                })
        }
    }
}