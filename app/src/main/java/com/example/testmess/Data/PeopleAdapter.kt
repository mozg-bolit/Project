package com.example.testmess.Data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.testmess.R
import com.example.testmess.databinding.ItemPersonBinding

class PeopleAdapter : RecyclerView.Adapter<PeopleAdapter.PeopleHolder>() {
    private val peopleList = mutableListOf<People>()

    var onMenuItemClickListener: ((People, Int) -> Unit)? = null


    inner class PeopleHolder(private val binding: ItemPersonBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(people: People) {
            with(binding) {
                nameTextView.text = people.name
                surnameTextView.text = people.surname
                patronymicTextView.text = people.patronymic

                popButton.setOnClickListener { view ->
                    showPopopMenu(view, people)
                }
            }
        }
    }

    private fun showPopopMenu(view: View, people: People) {
        PopupMenu(view.context, view).apply {
            menuInflater.inflate(R.menu.popupmenu, menu)
            true

            setOnMenuItemClickListener { item ->
                onMenuItemClickListener?.invoke(people, item.itemId)
                true
            }
            show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeopleHolder {
        val binding = ItemPersonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PeopleHolder(binding)
    }

    override fun onBindViewHolder(holder: PeopleHolder, position: Int) {
        holder.bind(peopleList[position])
    }

    override fun getItemCount(): Int = peopleList.size

    fun updateList(newList: List<People>) {
        peopleList.clear()
        peopleList.addAll(newList)
        notifyDataSetChanged()
    }
}