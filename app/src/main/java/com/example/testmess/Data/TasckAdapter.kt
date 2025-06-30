package com.example.testmess.Data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.testmess.R
import com.example.testmess.databinding.ItemTasckBinding

class TasckAdapter : RecyclerView.Adapter<TasckAdapter.TasckHolder>() {

    private val tasckList = mutableListOf<Tasck>()

    var onMenuItemClickListener: ((Tasck, Int) -> Unit)? = null

    inner class TasckHolder(private val binding: ItemTasckBinding) :
        RecyclerView.ViewHolder(binding.root){

            fun bind(tasck: Tasck) {
            with(binding) {
                peopleNameTasck.text = "${tasck.peopleNameTasck}"
                tasckType.text = "${tasck.typeTasck}"
                spinnerTasckText.text = "Задача: ${tasck.textTasck}"
                dateTasck.text = "Дата начала: ${tasck.dateTasck}"
                dateTasckComplete.text = "Дата сдачи:${tasck.dateTaskComplete}"

                buttonTasck.setOnClickListener { view ->
                    showPopopMenu(view, tasck)
                }
            }
        }
    }

    private fun showPopopMenu(view: View, tasck: Tasck) {
        PopupMenu(view.context, view).apply {
            menuInflater.inflate(R.menu.popupmenu, menu)
            true

            setOnMenuItemClickListener { item ->
                onMenuItemClickListener?.invoke(tasck, item.itemId)
                true
            }
            show()
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TasckHolder {
        val binding = ItemTasckBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return TasckHolder(binding)
    }

    override fun onBindViewHolder(holder: TasckHolder, position: Int) {
       holder.bind(tasckList[position])
    }

    override fun getItemCount(): Int = tasckList.size

    fun updateList(newList: List<Tasck>) {
        tasckList.clear()
        tasckList.addAll(newList)
        notifyDataSetChanged()
    }


}
