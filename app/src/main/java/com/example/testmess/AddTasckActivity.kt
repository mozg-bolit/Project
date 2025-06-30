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
import com.example.testmess.AddSalaryActivity
import com.example.testmess.Data.Salary
import com.example.testmess.Data.Tasck
import com.example.testmess.Data.TasckAdapter
import com.example.testmess.databinding.ActivityAddSalaryBinding
import com.example.testmess.databinding.ActivityAddTasckBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddTasckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTasckBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val peopleList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_tasck)
        binding = ActivityAddTasckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupSpinners()
        loadPeopleData()

        binding.addTasckButton.setOnClickListener {
            saveTasck()
        }
    }

    private fun loadPeopleData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Вы не авторизированы")
            finish()
            return
        }

        val peopleRef = database.getReference("People")
        peopleRef.orderByChild("employerId").equalTo(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    peopleList.clear()
                    peopleList.add("Выберите сотрудника") // Default first item

                    for (personSnapshot in snapshot.children) {
                        val name = personSnapshot.child("name").getValue(String::class.java) ?: ""
                        val surname =
                            personSnapshot.child("surname").getValue(String::class.java) ?: ""
                        val patronymic =
                            personSnapshot.child("patronymic").getValue(String::class.java) ?: ""

                        val fullName = "$surname $name $patronymic".trim()
                        if (fullName.isNotEmpty()) {
                            peopleList.add(fullName)
                        }
                    }

                    // Update the spinner adapter
                    val peopleAdapter = ArrayAdapter(
                        this@AddTasckActivity,
                        android.R.layout.simple_spinner_item,
                        peopleList
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    binding.peopleNameTasck.adapter = peopleAdapter
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка загрузки данных сотрудников")
                }
            })
    }

    private fun setupSpinners() {
        val tasckType = arrayOf("Срочно", "Квартальная", "Отчет", "Доклад", "Изделие", "Расчет")

        val tasckTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tasckType
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.TypeTasck.adapter = tasckTypeAdapter
        binding.TypeTasck.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selected = parent?.getItemAtPosition(position).toString()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    showToast("Ничего не выбрано")
                }
            }
    }

    private fun saveTasck() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Вы не авторизированы")
            finish()
            return
        }

        val peopleName: String = binding.peopleNameTasck.selectedItem?.toString() ?: ""
        val tasckType: String = binding.TypeTasck.selectedItem?.toString() ?: ""
        val tasckValue: String = binding.editTextText.text.toString().trim()
        val tasckDate: String = binding.editDateTasck.text.toString().trim()
        val tasckDateComplete: String = binding.editDateTasckComlete.text.toString().trim()

        if (peopleName.isEmpty() || peopleName == "Выберите сотрудника" ||
            tasckType.isEmpty() || tasckType == "Выберите" ||
            tasckValue.isEmpty() || tasckDate.isEmpty()
        ) {
            showToast("Заполните все поля")
            return
        }

        val tasck = Tasck(
            peopleNameTasck = peopleName,
            typeTasck = tasckType,
            textTasck = tasckValue,
            dateTasck = tasckDate,
            dateTaskComplete = tasckDateComplete,
            employerId = currentUser.uid
        )

        val tasckRef = database.getReference("Tasck")
        val tasckId = tasckRef.push().key ?: ""

        tasckRef.child(tasckId)
            .setValue(tasck)
            .addOnCompleteListener {
                showToast("Выплата добавлена")
                finish()
            }
            .addOnFailureListener {
                showToast("Ошибка добавления")
            }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}