package com.simoneklundh.shoppinglist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

data class HomeListItem(val meta: ShoppingListMeta, val remainingCount: Int)

class HomeAdapter(
    private val onClick: (ShoppingListMeta) -> Unit,
    private val onLongClick: (ShoppingListMeta) -> Unit,
) : RecyclerView.Adapter<HomeAdapter.ListViewHolder>() {

    private var rows: MutableList<HomeListItem> = mutableListOf()

    fun submitList(newRows: List<HomeListItem>) {
        val diff = DiffUtil.calculateDiff(ListDiff(rows, newRows))
        rows = newRows.toMutableList()
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_card, parent, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(rows[position])
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.listName)
        private val countText: TextView = itemView.findViewById(R.id.listItemCount)

        fun bind(row: HomeListItem) {
            nameText.text = row.meta.name
            countText.text = if (row.remainingCount == 0) {
                itemView.resources.getString(R.string.list_empty)
            } else {
                itemView.resources.getQuantityString(
                    R.plurals.list_item_count, row.remainingCount, row.remainingCount
                )
            }
            itemView.setOnClickListener { onClick(row.meta) }
            itemView.setOnLongClickListener {
                onLongClick(row.meta)
                true
            }
        }
    }

    private class ListDiff(
        private val old: List<HomeListItem>,
        private val new: List<HomeListItem>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition].meta.id == new[newItemPosition].meta.id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition] == new[newItemPosition]
    }
}
