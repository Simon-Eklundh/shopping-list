package com.example.shoppinglist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox

sealed interface Row {
    data class Item(val item: ShoppingItem) : Row
    data class CheckedHeader(val count: Int, val expanded: Boolean) : Row
}

class ShoppingListAdapter(
    private val onToggle: (ShoppingItem, Boolean) -> Unit,
    private val onDelete: (ShoppingItem) -> Unit,
    private val onHeaderClick: () -> Unit,
) : ListAdapter<Row, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int) = when (getItem(position)) {
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
        when (val row = getItem(position)) {
            is Row.Item -> (holder as ItemViewHolder).bind(row.item)
            is Row.CheckedHeader -> (holder as HeaderViewHolder).bind(row)
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.itemCheckBox)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

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

    private object Diff : DiffUtil.ItemCallback<Row>() {
        override fun areItemsTheSame(oldItem: Row, newItem: Row) = when {
            oldItem is Row.CheckedHeader && newItem is Row.CheckedHeader -> true
            oldItem is Row.Item && newItem is Row.Item ->
                // Names are unique (duplicates are blocked), so the name is a stable identity.
                oldItem.item.name.equals(newItem.item.name, ignoreCase = true)
            else -> false
        }

        override fun areContentsTheSame(oldItem: Row, newItem: Row) = oldItem == newItem
    }
}
