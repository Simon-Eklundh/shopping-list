package com.simoneklundh.shoppinglist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ShoppingRepository
    private lateinit var homeAdapter: HomeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        repository = ShoppingRepository(this)

        homeAdapter = HomeAdapter(
            onClick = { list -> openList(list) },
            onLongClick = { list -> confirmDeleteList(list) },
            remainingItemCount = { list -> repository.loadItems(list.id).count { !it.checked } },
        )
        val recycler = findViewById<RecyclerView>(R.id.listsRecyclerView)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = homeAdapter

        findViewById<FloatingActionButton>(R.id.newListButton).setOnClickListener {
            showCreateListDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLists()
    }

    private fun refreshLists() {
        val lists = repository.loadLists().sortedByDescending { it.createdAt }
        homeAdapter.submitList(lists)
        findViewById<View>(R.id.emptyState).visibility =
            if (lists.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openList(list: ShoppingListMeta) {
        val intent = Intent(this, ShoppingListActivity::class.java)
        intent.putExtra(ShoppingListActivity.EXTRA_LIST_ID, list.id)
        intent.putExtra(ShoppingListActivity.EXTRA_LIST_NAME, list.name)
        startActivity(intent)
    }

    private fun showCreateListDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_new_list, null)
        val input = view.findViewById<TextInputEditText>(R.id.listNameInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_list)
            .setView(view)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    input.error = getString(R.string.list_name_required)
                } else {
                    val list = repository.createList(name)
                    dialog.dismiss()
                    refreshLists()
                    openList(list)
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteList(list: ShoppingListMeta) {
        AlertDialog.Builder(this)
            .setTitle(list.name)
            .setMessage(R.string.delete_list_confirm)
            .setPositiveButton(R.string.delete_list) { _, _ ->
                repository.deleteList(list.id)
                refreshLists()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
