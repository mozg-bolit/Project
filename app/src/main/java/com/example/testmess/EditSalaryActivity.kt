package com.example.testmess

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testmess.Data.People
import com.example.testmess.Data.Salary
import com.example.testmess.databinding.ActivityEditSalaryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditSalaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditSalaryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val peopleList = mutableListOf<String>()
    private var salaryId: String = ""
    private var currentEmployerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditSalaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentEmployerId = auth.currentUser?.uid ?: ""

        // Получаем ID выплаты из Intent
        salaryId = intent.getStringExtra("salaryId") ?: ""
        if (salaryId.isEmpty()) {
            showToast("Ошибка: не указана выплата")
            finish()
            return
        }

        setupPeopleSpinner()
        setupSalaryTypeSpinner()
        loadSalaryData()

        binding.buttonSalaryUpdate.setOnClickListener {
            updateSalaryData()
        }
    }

    private fun setupPeopleSpinner() {
        database.getReference("People")
            .orderByChild("employerId")
            .equalTo(currentEmployerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    peopleList.clear()
                    for (childSnapshot in snapshot.children) {
                        val person = childSnapshot.getValue(People::class.java)
                        person?.let {
                            peopleList.add("${it.surname} ${it.name} ${it.patronymic}")
                        }
                    }

                    val adapter = ArrayAdapter(
                        this@EditSalaryActivity,
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

    private fun setupSalaryTypeSpinner() {
        binding.salaryEditType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                parent?.getItemAtPosition(position)?.toString()?.let {
                    updateSalaryHint(it)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                showToast("Ничего не выбрано")
            }
        }
    }

    private fun updateSalaryHint(selectedType: String) {
        when(selectedType) {
            "Почасовая" -> binding.redactSalaryValue.hint = "Плата в час"
            "Сдельная" -> binding.redactSalaryValue.hint = "Плата за изделие"
            "Оклад" -> binding.redactSalaryValue.hint = "Размер оклада"
            else -> binding.redactSalaryValue.hint = "Величина выплаты"
        }
    }

    private fun loadSalaryData() {
        database.getReference("Salary")
            .child(salaryId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showToast("Выплата не найдена")
                        finish()
                        return
                    }

                    val salary = snapshot.getValue(Salary::class.java)
                    salary?.let {
                        // Устанавливаем данные в поля
                        binding.redactTextDate.setText(it.date)
                        binding.redactSalaryValue.setText(it.amount.toString())

                        // Устанавливаем тип выплаты в спиннере
                        val typeAdapter = binding.salaryEditType.adapter as? ArrayAdapter<String>
                        typeAdapter?.let { adapter ->
                            val position = adapter.getPosition(it.type)
                            if (position >= 0) {
                                binding.salaryEditType.setSelection(position)
                            }
                        }

                        // Устанавливаем сотрудника в спиннере
                        val peopleAdapter = binding.redactSpinnerPeople.adapter as? ArrayAdapter<String>
                        peopleAdapter?.let { adapter ->
                            val position = adapter.getPosition(it.peopleName)
                            if (position >= 0) {
                                binding.redactSpinnerPeople.setSelection(position)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка загрузки выплаты: ${error.message}")
                }
            })
    }

    private fun updateSalaryData() {
        val date = binding.redactTextDate.text.toString().trim()
        val amountText = binding.redactSalaryValue.text.toString().trim()
        val type = binding.salaryEditType.selectedItem?.toString() ?: ""
        val selectedPerson = binding.redactSpinnerPeople.selectedItem?.toString() ?: ""

        if (date.isEmpty() || amountText.isEmpty() || type.isEmpty() || selectedPerson.isEmpty()) {
            showToast("Заполните все обязательные поля")
            return
        }

        val amount = try {
            amountText.toInt()
        } catch (e: NumberFormatException) {
            showToast("Некорректная сумма выплаты")
            return
        }

        val updates = hashMapOf<String, Any>(
            "peopleName" to selectedPerson,
            "type" to type,
            "amount" to amount,
            "date" to date,
            "employerId" to currentEmployerId
        )

        database.getReference("Salary")
            .child(salaryId)
            .updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Данные выплаты успешно обновлены")
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