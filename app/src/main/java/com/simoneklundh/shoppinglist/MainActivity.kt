package com.simoneklundh.shoppinglist

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ShoppingRepository
    private lateinit var listAdapter: ShoppingListAdapter
    private lateinit var suggestionsAdapter: ArrayAdapter<String>
    private lateinit var input: MaterialAutoCompleteTextView

    private val items = mutableListOf<ShoppingItem>()
    private var checkedExpanded = false

    private val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (!listAdapter.canReorder(from, to)) return false
            listAdapter.moveItem(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun isLongPressDragEnabled() = false

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            persistReorder()
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<android.view.View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        repository = ShoppingRepository(this)
        items.addAll(repository.loadItems())

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear_checked -> {
                    clearChecked()
                    true
                }
                else -> false
            }
        }

        listAdapter = ShoppingListAdapter(
            onToggle = { item, checked -> toggleItem(item, checked) },
            onDelete = { item -> deleteItem(item) },
            onHeaderClick = {
                checkedExpanded = !checkedExpanded
                refreshList()
            },
            onStartDrag = { holder -> itemTouchHelper.startDrag(holder) },
        )
        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = listAdapter
        itemTouchHelper.attachToRecyclerView(recycler)

        input = findViewById(R.id.inputItem)
        suggestionsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            repository.loadHistory(),
        )
        input.setAdapter(suggestionsAdapter)
        input.threshold = 1
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addItem()
                true
            } else {
                false
            }
        }
        input.setOnItemClickListener { _, _, _, _ -> addItem() }

        findViewById<ImageButton>(R.id.addButton).setOnClickListener { addItem() }

        refreshList()
    }

    private fun addItem() {
        val name = input.text.toString().trim()
        if (name.isEmpty()) return

        val existingIndex = items.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (existingIndex != -1) {
            val existing = items[existingIndex]
            if (existing.checked) {
                // Re-adding a bought item puts it back on the active list.
                items[existingIndex] = existing.copy(checked = false)
                repository.saveItems(items)
                refreshList()
            } else {
                Snackbar.make(
                    findViewById(R.id.root),
                    getString(R.string.duplicate_item, existing.name),
                    Snackbar.LENGTH_SHORT,
                ).show()
            }
        } else {
            items.add(ShoppingItem(name))
            repository.saveItems(items)
            refreshSuggestions(name)
            refreshList()
        }
        input.setText("")
        input.dismissDropDown()
    }

    private fun toggleItem(item: ShoppingItem, checked: Boolean) {
        val index = items.indexOfFirst { it.name == item.name }
        if (index == -1) return
        items[index] = items[index].copy(checked = checked)
        repository.saveItems(items)
        refreshList()
    }

    private fun deleteItem(item: ShoppingItem) {
        items.removeAll { it.name == item.name }
        repository.saveItems(items)
        refreshList()
    }

    private fun persistReorder() {
        val newUnchecked = listAdapter.currentUncheckedItems()
        val checked = items.filter { it.checked }
        items.clear()
        items.addAll(newUnchecked)
        items.addAll(checked)
        repository.saveItems(items)
    }

    private fun clearChecked() {
        if (items.none { it.checked }) return
        items.removeAll { it.checked }
        repository.saveItems(items)
        refreshList()
    }

    private fun refreshList() {
        val (checked, unchecked) = items.partition { it.checked }
        val rows = buildList {
            unchecked.forEach { add(Row.Item(it)) }
            if (checked.isNotEmpty()) {
                add(Row.CheckedHeader(checked.size, checkedExpanded))
                if (checkedExpanded) checked.forEach { add(Row.Item(it)) }
            }
        }
        listAdapter.submitList(rows)
    }

    private fun refreshSuggestions(newName: String) {
        repository.addToHistory(newName)
        suggestionsAdapter.clear()
        suggestionsAdapter.addAll(repository.loadHistory())
    }
}