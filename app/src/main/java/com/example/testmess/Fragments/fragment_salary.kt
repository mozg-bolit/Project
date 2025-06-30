package com.example.testmess.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.testmess.AddSalaryActivity
import com.example.testmess.Data.Salary
import com.example.testmess.Data.SalaryAdapter
import com.example.testmess.EditSalaryActivity
import com.example.testmess.R
import com.example.testmess.databinding.FragmentSalaryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class fragment_salary : Fragment() {
    private lateinit var binding: FragmentSalaryBinding
    private lateinit var adapter: SalaryAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var salariesRef: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSalaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        salariesRef = database.getReference("Salary")

        setupRecyclerView()
        setupFirebaseListener()

        binding.addSalaryButton.setOnClickListener {
            startActivity(Intent(requireActivity(), AddSalaryActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = SalaryAdapter().apply {
            onMenuItemClickListener = { salary, menuItem ->
                when(menuItem) {
                    R.id.pop_delete -> {
                        deleteSalary(salary)
                    }
                    R.id.pop_redact -> {
                        editSalary(salary)
                    }

                }
            }
        }

        binding.recSalary.layoutManager = LinearLayoutManager(requireContext())
        binding.recSalary.adapter = adapter
    }

    private fun setupFirebaseListener() {
        // Удаляем предыдущий слушатель, если он был
        if (::valueEventListener.isInitialized) {
            salariesRef.removeEventListener(valueEventListener)
        }

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("SalaryFragment", "Data received: ${snapshot.childrenCount} items")
                val salariesList = mutableListOf<Salary>()

                for (child in snapshot.children) {
                    try {
                       if (child.child("employerId").value.toString() == auth.currentUser?.uid){
                           var salary = child.getValue(Salary::class.java)?.apply {
                               id = child.key ?: ""
                           }
                           salary?.let { salariesList.add(it) }
                       }
                    } catch (e: Exception) {
                        Log.e("SalaryFragment", "Error parsing salary data", e)
                    }
                }

                // Обновляем адаптер
                adapter.updateList(salariesList)
                            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SalaryFragment", "Database error: ${error.message}")
                Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        }

        // Добавляем слушатель ко всей ноде Salary
        salariesRef.addValueEventListener(valueEventListener)
    }

    private fun deleteSalary(salary: Salary) {
        if (salary.id.isEmpty()) {
            Toast.makeText(requireContext(), "Не удалось идентифицировать выплату", Toast.LENGTH_SHORT).show()
            return
        }

        salariesRef.child(salary.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Выплата удалена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editSalary(salary: Salary) {
        if (salary.id.isEmpty()) {
            Toast.makeText(requireContext(), "Не удалось определить выплату", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(requireContext(), EditSalaryActivity::class.java).apply {
            putExtra("salaryId", salary.id)
        }
        startActivity(intent)
    }

    private fun showDetails(salary: Salary) {
        Toast.makeText(requireContext(), """
            Сотрудник: ${salary.peopleName}
            Тип: ${salary.type}
            Сумма: ${salary.amount} ₽
            Дата: ${salary.date}
        """.trimIndent(), Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Удаляем слушатель при уничтожении View
        if (::valueEventListener.isInitialized) {
            salariesRef.removeEventListener(valueEventListener)
        }
    }
}