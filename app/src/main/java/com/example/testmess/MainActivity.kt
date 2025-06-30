package com.example.testmess

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.testmess.Fragments.fragment_tasck
import com.example.testmess.Fragments.fragment_peoples
import com.example.testmess.Fragments.fragment_profile
import com.example.testmess.Fragments.fragment_salary
import com.example.testmess.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val THEME_PREF_KEY = "app_theme"
        private const val THEME_LIGHT = "light"
        private const val THEME_DARK = "dark"
        private const val THEME_SYSTEM = "system"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Применяем сохранённую тему перед установкой контента
        applySavedTheme()

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверяем авторизацию пользователя
        if(FirebaseAuth.getInstance().currentUser == null){
            startActivity(Intent(this, LoginActivty::class.java))
            finish() // Закрываем MainActivity
            return // Прерываем выполнение onCreate
        }

        // Загружаем фрагмент профиля
        loadFragment(fragment_profile())

        binding.bottomNav.setOnItemSelectedListener {
            Log.d("NAV", "Selected item: ${it.itemId}")
            when(it.itemId){
                R.id.frtc -> {
                    Log.d("NAV", "Loading Chats fragment")
                    loadFragment(fragment_tasck())
                }
                R.id.frpl ->{
                    Log.d("NAV", "Loading Peoples fragment")
                    loadFragment(fragment_peoples())
                }
                R.id.frsl -> {
                    Log.d("NAV", "Loading Salary fragment")
                    loadFragment(fragment_salary())
                }
                R.id.frpr -> {
                    Log.d("NAV", "Loading Profile fragment")
                    loadFragment(fragment_profile())
                }
            }
            true
        }
    }

    // Применяем сохранённую тему при запуске
    private fun applySavedTheme() {
        // Инициализируем SharedPreferences прямо здесь
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val savedTheme = sharedPreferences.getString(THEME_PREF_KEY, THEME_SYSTEM) ?: THEME_SYSTEM

        when (savedTheme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }
}