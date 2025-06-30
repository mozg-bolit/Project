package com.example.testmess.Data

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.testmess.R
import com.example.testmess.databinding.ItemSalaryBinding

class SalaryAdapter : RecyclerView.Adapter<SalaryAdapter.SalaryHolder>() {
    private val salaryList = mutableListOf<Salary>()
    var onMenuItemClickListener: ((Salary, Int) -> Unit)? = null

    inner class SalaryHolder(private val binding: ItemSalaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(salary: Salary) {
            with(binding) {
                personName.text = salary.peopleName ?: "Не указано"
                amount.text = "${salary.amount ?: 0} ₽"
                date.text = salary.date ?: "Не указана"
                type.text = salary.type ?: "Не указан"

                popButton.setOnClickListener { view ->
                    showPopupMenu(view, salary)
                }

                Log.d("SalaryAdapter", "Binding salary: ${salary.peopleName}")
            }
        }

        private fun showPopupMenu(view: View, salary: Salary) {
            PopupMenu(view.context, view).apply {
                menuInflater.inflate(R.menu.popupmenu, menu)
                setOnMenuItemClickListener { item ->
                    onMenuItemClickListener?.invoke(salary, item.itemId)
                    true
                }
                show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalaryHolder {
        val binding = ItemSalaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SalaryHolder(binding)
    }

    override fun onBindViewHolder(holder: SalaryHolder, position: Int) {
        holder.bind(salaryList[position])
    }

    override fun getItemCount(): Int = salaryList.size

    fun updateList(newList: List<Salary>) {
        salaryList.clear()
        salaryList.addAll(newList)
        Log.d("SalaryAdapter", "List updated with ${newList.size} items")
        notifyDataSetChanged()
    }
}