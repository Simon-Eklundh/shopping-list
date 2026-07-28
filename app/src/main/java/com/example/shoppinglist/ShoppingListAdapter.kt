package com.example.shoppinglist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import java.util.Collections

sealed interface Row {
    data class Item(val item: ShoppingItem) : Row
    data class CheckedHeader(val count: Int, val expanded: Boolean) : Row
}

class ShoppingListAdapter(
    private val onToggle: (ShoppingItem, Boolean) -> Unit,
    private val onDelete: (ShoppingItem) -> Unit,
    private val onHeaderClick: () -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: MutableList<Row> = mutableListOf()

    fun submitList(newRows: List<Row>) {
        val diff = DiffUtil.calculateDiff(RowDiff(rows, newRows))
        rows = newRows.toMutableList()
        diff.dispatchUpdatesTo(this)
    }

    /** Unchecked items in their current on-screen order, reflecting any in-progress drag. */
    fun currentUncheckedItems(): List<ShoppingItem> =
        rows.filterIsInstance<Row.Item>().map { it.item }.filter { !it.checked }

    /** Only adjacent unchecked items may be reordered; the header and checked items are fixed. */
    fun canReorder(from: Int, to: Int): Boolean {
        if (from !in rows.indices || to !in rows.indices) return false
        val fromRow = rows[from]
        val toRow = rows[to]
        return fromRow is Row.Item && !fromRow.item.checked && toRow is Row.Item && !toRow.item.checked
    }

    fun moveItem(from: Int, to: Int) {
        if (from < to) {
            for (i in from until to) Collections.swap(rows, i, i + 1)
        } else {
            for (i in from downTo to + 1) Collections.swap(rows, i, i - 1)
        }
        notifyItemMoved(from, to)
    }

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int) = when (rows[position]) {
        is Row.Item -> R.layout.item_shopping
        is Row.CheckedHeader -> R.layout.item_checked_header
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.item_shopping -> ItemViewHolder(view)
            else -> HeaderViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Item -> (holder as ItemViewHolder).bind(row.item)
            is Row.CheckedHeader -> (holder as HeaderViewHolder).bind(row)
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.itemCheckBox)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val dragHandle: ImageView = itemView.findViewById(R.id.dragHandle)

        fun bind(item: ShoppingItem) {
            checkBox.setOnCheckedChangeListener(null)
            checkBox.text = item.name
            checkBox.isChecked = item.checked
            itemView.alpha = if (item.checked) 0.55f else 1f
            checkBox.paintFlags = if (item.checked) {
                checkBox.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                checkBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            checkBox.setOnCheckedChangeListener { _, isChecked -> onToggle(item, isChecked) }
            deleteButton.setOnClickListener { onDelete(item) }

            dragHandle.visibility = if (item.checked) View.INVISIBLE else View.VISIBLE
            dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN && !item.checked) {
                    onStartDrag(this)
                }
                false
            }
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerRow: View = itemView.findViewById(R.id.headerRow)
        private val chevron: ImageView = itemView.findViewById(R.id.chevron)
        private val text: TextView = itemView.findViewById(R.id.headerText)

        fun bind(row: Row.CheckedHeader) {
            text.text = itemView.resources.getQuantityString(
                R.plurals.checked_items, row.count, row.count
            )
            chevron.rotation = if (row.expanded) 90f else 0f
            headerRow.setOnClickListener { onHeaderClick() }
        }
    }

    private class RowDiff(private val old: List<Row>, private val new: List<Row>) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = old[oldItemPosition]
            val newItem = new[newItemPosition]
            return when {
                oldItem is Row.CheckedHeader && newItem is Row.CheckedHeader -> true
                oldItem is Row.Item && newItem is Row.Item ->
                    // Names are unique (duplicates are blocked), so the name is a stable identity.
                    oldItem.item.name.equals(newItem.item.name, ignoreCase = true)
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition] == new[newItemPosition]
    }
}
