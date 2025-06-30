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
import com.example.testmess.Data.People
import com.example.testmess.Data.Salary
import com.example.testmess.Data.Tasck
import com.example.testmess.databinding.ActivityEditTasckBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditTasckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditTasckBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val peopleList = mutableListOf<String>()
    private var tasckId: String = ""
    private var currentEmployerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditTasckBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.button.setOnClickListener { finish() }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentEmployerId = auth.currentUser?.uid ?: ""

        // Получаем ID задачи из Intent
        tasckId = intent.getStringExtra("tasckId") ?: ""
        if (tasckId.isEmpty()) {
            showToast("Ошибка: не указана задача")
            finish()
            return
        }

        setupPeopleSpinner()
        setupTasckTypeSpinner() // Инициализация спиннера типов задач
        loadTasckData()

        binding.buttonTasckUpdate.setOnClickListener {
            updateTasckData()
        }
    }

    private fun setupPeopleSpinner() {
        database.getReference("People")
            .orderByChild("employerId")
            .equalTo(currentEmployerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    peopleList.clear()
                    peopleList.add("Выберите сотрудника") // Default first item

                    for (childSnapshot in snapshot.children) {
                        val person = childSnapshot.getValue(People::class.java)
                        person?.let {
                            peopleList.add("${it.surname} ${it.name} ${it.patronymic}")
                        }
                    }

                    val adapter = ArrayAdapter(
                        this@EditTasckActivity,
                        android.R.layout.simple_spinner_item,
                        peopleList
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.redactSpinnerPeople.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка загрузки сотрудников: ${error.message}")
                }
            })
    }

    private fun setupTasckTypeSpinner() {
        // Те же типы задач, что и в AddTasckActivity
        val tasckTypes = arrayOf("Срочно", "Квартальная", "Отчет", "Доклад", "Изделие", "Расчет")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tasckTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.tasckEditType.adapter = adapter

        binding.tasckEditType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Можно добавить логику при выборе типа задачи
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                showToast("Выберите тип задачи")
            }
        }
    }

    private fun loadTasckData() {
        database.getReference("Tasck")
            .child(tasckId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showToast("Задача не найдена")
                        finish()
                        return
                    }

                    val tasck = snapshot.getValue(Tasck::class.java)
                    tasck?.let {
                        // Устанавливаем данные в поля
                        binding.redactTasckValue.setText(it.textTasck)
                        binding.dateStart.setText(it.dateTasck)
                        binding.dateComplete.setText(it.dateTaskComplete)

                        // Устанавливаем тип задачи в спиннере
                        val typeAdapter = binding.tasckEditType.adapter as? ArrayAdapter<String>
                        typeAdapter?.let { adapter ->
                            val position = adapter.getPosition(it.typeTasck)
                            if (position >= 0) {
                                binding.tasckEditType.setSelection(position)
                            }
                        }

                        // Устанавливаем сотрудника в спиннере
                        val peopleAdapter = binding.redactSpinnerPeople.adapter as? ArrayAdapter<String>
                        peopleAdapter?.let { adapter ->
                            val position = adapter.getPosition(it.peopleNameTasck)
                            if (position >= 0) {
                                binding.redactSpinnerPeople.setSelection(position)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка загрузки задачи: ${error.message}")
                }
            })
    }

    private fun updateTasckData() {
        val peopleName = binding.redactSpinnerPeople.selectedItem?.toString() ?: ""
        val type = binding.tasckEditType.selectedItem?.toString() ?: ""
        val text = binding.redactTasckValue.text.toString().trim()
        val date = binding.dateStart.text.toString().trim()
        val dateComplete = binding.dateComplete.text.toString().trim()

        if (peopleName.isEmpty() || peopleName == "Выберите сотрудника" ||
            type.isEmpty() || text.isEmpty() || date.isEmpty() || dateComplete.isEmpty()) {
            showToast("Заполните все обязательные поля")
            return
        }

        val updates = hashMapOf<String, Any>(
            "peopleName" to peopleName,
            "typeTasck" to type,
            "textTasck" to text,
            "dateTasck" to date,
            "dateTaskComplete" to dateComplete,
            "employerId" to currentEmployerId
        )

        database.getReference("Tasck")
            .child(tasckId)
            .updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Данные задачи успешно обновлены")
                    finish()
                } else {
                    showToast("Ошибка: ${task.exception?.message}")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}