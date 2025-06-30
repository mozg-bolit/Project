package com.example.testmess.Fragments

import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.testmess.AddPeopleActivity
import com.example.testmess.Data.People
import com.example.testmess.Data.PeopleAdapter
import com.example.testmess.EditPersonActivity
import com.example.testmess.databinding.FragmentPeoplesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.testmess.R

class fragment_peoples : Fragment() {
    private lateinit var adapter: PeopleAdapter
    private lateinit var binding: FragmentPeoplesBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var datebase: FirebaseDatabase
    private lateinit var peopleRef: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPeoplesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        datebase = FirebaseDatabase.getInstance()
        peopleRef = FirebaseDatabase.getInstance().getReference("People")


        setupRecyclerView()
        setupFirebaseListener()
        binding.addPeopleButton.setOnClickListener {
            startActivity(Intent(requireActivity(), AddPeopleActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = PeopleAdapter().apply {
        onMenuItemClickListener = {people, menuItem ->
            when(menuItem){
                R.id.pop_delete -> {
                    deletePerson(people)
                }
                R.id.pop_redact -> {
                    editPerson(people)
                }

            }
        }
        }

        binding.recPeople.layoutManager = LinearLayoutManager(requireContext())
        binding.recPeople.adapter = adapter
    }


    private fun setupFirebaseListener() {
        // Удаляем предыдущий слушатель, если он был
        if (::valueEventListener.isInitialized) {
            peopleRef.removeEventListener(valueEventListener)
        }

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val peopleList = mutableListOf<People>()

                for (child in snapshot.children) {
                    if (child.child("employerId").value.toString() == auth.currentUser?.uid) {
                        var person = child.getValue(People::class.java)?.apply {
                            id = child.key ?: ""
                        }
                        person?.let { peopleList.add(it) }
                    }
                }
                adapter.updateList(peopleList)

            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Ошибка загрузки данных: ${error.message}")
            }
        }

        // Добавляем слушатель ко всей ноде People
        peopleRef.addValueEventListener(valueEventListener)
    }


    private fun deletePerson(people: People){
        if (people.id.isEmpty()){
            showToast("Неудалось идентифицировать работника")
        }
        else{
            val personRef = FirebaseDatabase.getInstance().getReference("People").child(people.id)
            personRef.removeValue()
                .addOnSuccessListener {
                    showToast("Сотрудник удален")
                }
                .addOnFailureListener {e ->
                    showToast("Ошибка удаления: ${e.message}")
                }
        }
    }
    private fun editPerson(people: People) {
        if (people.id.isEmpty()) {
            showToast("Не удалось определить сотрудника")
            return
        }

        val intent = Intent(requireContext(), EditPersonActivity::class.java).apply {
            putExtra("personId", people.id)
        }
        startActivity(intent)
    }



    private fun showToast(message: String){
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
