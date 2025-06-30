package com.example.testmess.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.testmess.AddPeopleActivity
import com.example.testmess.AddTasckActivity
import com.example.testmess.Data.Tasck
import com.example.testmess.Data.TasckAdapter
import com.example.testmess.EditPersonActivity
import com.example.testmess.EditTasckActivity
import com.example.testmess.R
import com.example.testmess.databinding.FragmentTasckBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class fragment_tasck : Fragment() {

    // Объявление переменных
    private lateinit var adapter: TasckAdapter
    private lateinit var binding: FragmentTasckBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var tasckRef: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация binding
        binding = FragmentTasckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        tasckRef = database.getReference("Tasck")

        // Настройка RecyclerView и слушателей
        setupRecyclerView()
        setupFirebaseListener()

        // Обработчик кнопки добавления задачи
        binding.buttonAddTasck.setOnClickListener {
            startActivity(Intent(requireActivity(), AddTasckActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        // Инициализация адаптера
        adapter = TasckAdapter().apply {
            onMenuItemClickListener = { tasck, menuItem ->
                when(menuItem) {
                    R.id.pop_delete -> deleteTasck(tasck)
                    R.id.pop_redact -> editTasck(tasck)
                }
            }
        }

        // Настройка RecyclerView
        binding.recTasck.layoutManager = LinearLayoutManager(requireContext())
        binding.recTasck.adapter = adapter
    }

    private fun setupFirebaseListener() {
        // Удаление предыдущего слушателя, если он был
        if (::valueEventListener.isInitialized) {
            tasckRef.removeEventListener(valueEventListener)
        }

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasckList = mutableListOf<Tasck>()

                for (child in snapshot.children) {
                    if (child.child("employerId").value.toString() == auth.currentUser?.uid) {
                        val tasck = child.getValue(Tasck::class.java)?.apply {
                            id = child.key ?: ""
                        }
                        tasck?.let { tasckList.add(it) }
                    }
                }
                adapter.updateList(tasckList)
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Ошибка загрузки данных: ${error.message}")
            }
        }

        // Добавление слушателя
        tasckRef.addValueEventListener(valueEventListener)
    }

    private fun deleteTasck(tasck: Tasck) {
        if (!isAdded) return

        if (tasck.id.isEmpty()) {
            showToast("Не удалось идентифицировать задачу")
            return
        }

        tasckRef.child(tasck.id).removeValue()
            .addOnSuccessListener {
                showToast("Задача удалена")
            }
            .addOnFailureListener { e ->
                showToast("Ошибка удаления: ${e.message}")
            }
    }

    private fun editTasck(tasck: Tasck) {
        if (!isAdded) return

        if (tasck.id.isEmpty()) {
            showToast("Не удалось определить задачу")
            return
        }

        val intent = Intent(requireContext(), EditTasckActivity::class.java).apply {
            putExtra("tasckId", tasck.id)
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        if (!isAdded) return
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Удаление слушателя при уничтожении View
        if (::valueEventListener.isInitialized) {
            tasckRef.removeEventListener(valueEventListener)
        }
    }
}