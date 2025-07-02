package com.example.qwe

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.qwe.data.CreateUserRequest
import com.example.qwe.data.User
import com.example.qwe.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: UserAdapter
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()

        binding.fabAdd.setOnClickListener { showAddUserDialog() }
        viewModel.loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter().apply {
            setOnEditClickListener { user -> showEditUserDialog(user) }
            setOnDeleteClickListener { user ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete User")
                    .setMessage("Delete ${user.first_name} ${user.last_name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteUser(user.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.users.observe(this) { users ->
            adapter.submitList(users)
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.etName)
        val jobEditText = dialogView.findViewById<EditText>(R.id.etJob)

        AlertDialog.Builder(this)
            .setTitle("Add New User")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString()
                val job = jobEditText.text.toString()
                if (name.isNotBlank() && job.isNotBlank()) {
                    viewModel.createUser(name, job)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditUserDialog(user: User) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.etName)
        val jobEditText = dialogView.findViewById<EditText>(R.id.etJob)

        nameEditText.setText("${user.first_name} ${user.last_name}")
        jobEditText.setText("Developer") // Пример значения по умолчанию

        AlertDialog.Builder(this)
            .setTitle("Edit User")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEditText.text.toString()
                val job = jobEditText.text.toString()
                if (name.isNotBlank() && job.isNotBlank()) {
                    viewModel.updateUser(user.id, name, job)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
/////////////////////////////////////////////////////////////////


class UserAdapter : ListAdapter<User, UserAdapter.ViewHolder>(UserDiffCallback()) {
    private var onEditClickListener: ((User) -> Unit)? = null
    private var onDeleteClickListener: ((User) -> Unit)? = null

    fun setOnEditClickListener(listener: (User) -> Unit) {
        onEditClickListener = listener
    }

    fun setOnDeleteClickListener(listener: (User) -> Unit) {
        onDeleteClickListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)

        fun bind(user: User) {
            tvName.text = "${user.first_name} ${user.last_name}"
            tvEmail.text = user.email

            Glide.with(itemView)
                .load(user.avatar)
                .circleCrop()
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_error_placeholder)
                .into(ivAvatar)

            btnMore.setOnClickListener { view ->
                PopupMenu(view.context, view).apply {
                    inflate(R.menu.menu_user)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.menu_edit -> {
                                onEditClickListener?.invoke(user)
                                true
                            }
                            R.id.menu_delete -> {
                                onDeleteClickListener?.invoke(user)
                                true
                            }
                            else -> false
                        }
                    }
                    show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}